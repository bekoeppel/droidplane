package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.net.Uri;
import android.util.Log;

import ch.benediktkoeppel.code.droidplane.LazyLoaderHandler.SAXEndNodeFoundException;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
public class Mindmap {
	
	/**
	 * the XML DOM document, the mind map
	 */
	public Document document;
	// TODO: need to know whether we load DOM or SAX, if SAX document is nonsense
	// TODO: should Mindmap and MindmapNode be an interface or abstract, and we have two classes (DOMMindmap/DOMMindmapNode, and SAXMindmap/SAXMindmapNode)
	// TODO: why exactly do we need t know the document? after all, it's just here to get to the root node basically

	/**
	 * The currently loaded Uri
	 */
	private Uri uri;
	
	/**
	 * The root node of the document.
	 */
	private MindmapNode rootNode;
	
	/**
	 * Counts the number of Mindmap nodes in the inputStream, without loading
	 * the whole document as DOM tree. This is useful to determine the size of
	 * the mind map before even trying (and spending a lot of time) to load it.
	 * @param inputStream
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static int getNodeCount(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
		
		// prepare the SAX parser
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser parser = saxParserFactory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		
		// the NodeCounterHandler will be used to count the node tags
		NodeCounterHandler nodeCounterHandler = new NodeCounterHandler();
		reader.setContentHandler(nodeCounterHandler);
		reader.parse(new InputSource(inputStream));
		
		// return the node count
		return nodeCounterHandler.getNodeCount();
	}
	

	public static int getNodeCount(RandomAccessFile raf) throws ParserConfigurationException, SAXException, IOException {
		
		// prepare the SAX parser
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser parser = saxParserFactory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		
		// the NodeCounterHandler will be used to count the node tags
		NodeCounterHandler nodeCounterHandler = new NodeCounterHandler();
		reader.setContentHandler(nodeCounterHandler);
		InputSource rafi = new InputSource(new RandomAccessFileReader(raf));
		rafi.setEncoding("UTF8");
		reader.parse(rafi);
		
		// return the node count
		return nodeCounterHandler.getNodeCount();
	}
	
	
	/**
	 * Returns the Uri which is currently loaded in document.
	 * @return Uri
	 */
	public Uri getUri() {
		return this.uri;
	}

	/**
	 * Set the Uri after loading a new document.
	 * @param uri
	 */
	public void setUri(Uri uri) {
		this.uri = uri;
	}
	
	/**
	 * Loads a mind map (*.mm) XML document into its internal DOM tree
	 * @param inputStream the inputStream to load
	 */
	public void loadDocument(InputStream inputStream) {
		
        // start measuring the document load time
		long loadDocumentStartTime = System.currentTimeMillis();
		
		// XML document builder
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;

        // load the Mindmap from the InputStream
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			document = docBuilder.parse(inputStream);
		} catch (ParserConfigurationException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "ParserConfigurationException");
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "SAXException");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "IOException");
			e.printStackTrace();
			return;
		}
		
		long loadDocumentEndTime = System.currentTimeMillis();
	    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
		Log.d(MainApplication.TAG, "Document loaded");
	    
		long numNodes = document.getElementsByTagName("node").getLength();
		EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);
		
		rootNode = new MindmapNode(document.getDocumentElement().getElementsByTagName("node").item(0));
	}
	
	
	
	public void loadDocument(RandomAccessFile raf) {
		
        // start measuring the document load time
		long loadDocumentStartTime = System.currentTimeMillis();

		// prepare the SAX parser
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		XMLReader reader = null;
		try {
			parser = saxParserFactory.newSAXParser();
			reader = parser.getXMLReader();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXEndNodeFoundException e) {
			/* empty */
			// TODO
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// the LazyLoaderHandler will load only the highest level of nodes, which means the root node
		// TODO: can we factor this "loadDocument" out? because it is really just loading the "node" tags with seek=0, nodelevel=0. Its the same at MindmapNode#getChildNodes()
		LazyLoaderHandler lazyLoaderHandler = new LazyLoaderHandler(raf);
		reader.setContentHandler(lazyLoaderHandler);
		InputSource rafi = new InputSource(new RandomAccessFileReader(raf));
		rafi.setEncoding("UTF8");
		try {
			reader.parse(rafi);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// retrieve the list of mindmap nodes
		ArrayList<MindmapNode> mindmapNodes = lazyLoaderHandler.getMindmapNodes();
		
		long loadDocumentEndTime = System.currentTimeMillis();
	    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
		Log.d(MainApplication.TAG, "Document loaded");

		// there should be only one, because we have just loaded the root node
		if ( mindmapNodes.size() > 1 ) {
			Log.e(MainApplication.TAG, "Got more than one node while fetching the root node, that's odd!");
		}

		// TODO: this does not work like this anymore. We should move it out into the getNodeCount methods (also for InputStream)
//		long numNodes = document.getElementsByTagName("node").getLength();
//		EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);
		
		// store the root node
		rootNode = mindmapNodes.get(0);
	}
	
	
	/**
	 * Returns the root node of the currently loaded mind map
	 * @return the root node
	 */
	public MindmapNode getRootNode() {
		return rootNode;
	}


}
