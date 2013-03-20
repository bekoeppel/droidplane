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
 * Mindmap handles the loading and storing of a mind map document. Technically,
 * Mindmap can operate in two modi. In the simple one, it loads a XML file as
 * DOM document. All operations happen then on the DOM document. This is good if
 * the XML has to be modified. But it is a performance problem, because we
 * rarely need all nodes of the map in RAM. In the second modus, Mindmap uses a
 * SAX parser to only extract nodes it really needs. This is a bit trickier
 * because we implement our own SAX parser/handler, and because we need to
 * rewind the input document. For this, we use a RandomAccessFile, which itself
 * can not be read directly by the SAX parser, and is thus wrapped in our
 * RandomAccessFileReader (extends Reader).
 */
public class Mindmap {
	
	// TODO: the mindmap has to know whether it is running DOM based or SAX
	// based. Basically, we should have a Mindmap interface, and then provide a
	// DOM-backed and a SAX-backed implementation of the interface. The same
	// applies for MindmapNode.
	
	// TODO: currently, there is no boolean variable that defines whether this
	// Mindmap is DOM or SAX based. When loading a document, it is distinguished
	// by the input argument (InputStream generates DOM based, RandomAccessFile
	// generates SAX based), and later the MindmapNode knows whether it is DOM
	// or SAX based.
	
	/**
	 * the XML DOM document, the mind map. This is only valid if we use DOM.
	 * TODO: do we really need to store the document? It might be handy when we
	 * want to edit the mindmap. But technically we can reach all nodes by
	 * storing the root node, so we don't really need the document right now.
	 */
	public Document document;

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
	 * @param inputStream the XML document as Input Stream 
	 * @return the number of nodes
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

	/**
	 * Similar to {@link Mindmap#getNodeCount(InputStream)}, this counts the
	 * nodes in a RandomAccessFile.
	 * @param raf the XML document as Random Access File
	 * @return the number of nodes
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
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
	 * Loads a mind map (*.mm) XML document into its internal DOM tree. This
	 * uses the InputStream and will be a DOM based mindmap.
	 * 
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
		
		// stop the time measurement and send it to Google Analytics
		long loadDocumentEndTime = System.currentTimeMillis();
	    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
		Log.d(MainApplication.TAG, "Document loaded");
	    
		// sent the number of nodes to Google Analytics
		long numNodes = document.getElementsByTagName("node").getLength();
		EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);
		
		// store the root node of the document
		rootNode = new MindmapNode(document.getDocumentElement().getElementsByTagName("node").item(0));
	}
	
	/**
	 * Similar to {@link Mindmap#loadDocument(InputStream)}, this loads a XML
	 * document. However, the XML document is backed by a RandomAccessFile, and
	 * the SAX parser will be used to dynamically reload more nodes as needed.
	 * 
	 * TODO: initially, the idea was that we need a RandomAccessFile so that we
	 * can seek to a node's start position. However, our current implementation
	 * does not seek, but just skip elements in the {@link LazyLoaderHandler}
	 * SAX parser until it has found the start position. This is certainly not
	 * as efficient as a seek would be. We do this because at the moment I don't
	 * know how to get the current file position of a tag, I only get the
	 * locator line/column in the SAX parser.
	 * 
	 * TODO: can we factor this "loadDocument" out and merge it with the
	 * getChildNodes method for a SAX-based MindmapNode? Because loading the
	 * root node is really just doing a getChildNodes with parameters
	 * startLine=0, startColumn=0, targetLevel=0.
	 * 
	 * @param raf the XML document as RandomAccessFile
	 */
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
			// TODO: we should pass this out somewhere, so that the MainActivity
			// knows that something went wrong. The MainActivity could then
			// switch to the InputStream based implementation.
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO: we should pass this out somewhere, so that the MainActivity
			// knows that something went wrong. The MainActivity could then
			// switch to the InputStream based implementation.
			e.printStackTrace();
		}
		
		// the LazyLoaderHandler will load only the highest level of nodes, which means the root node
		LazyLoaderHandler lazyLoaderHandler = new LazyLoaderHandler(raf);
		reader.setContentHandler(lazyLoaderHandler);
		InputSource rafi = new InputSource(new RandomAccessFileReader(raf));
		rafi.setEncoding("UTF8");
		try {
			reader.parse(rafi);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXEndNodeFoundException e) {
			/* empty */
			// the SAXEndNodeFoundException is not a real exception. It
			// merely tells us that our Lazy handler has found the end of the
			// interesting node and decided to stop the parsing there (for
			// efficiency). So we consume this Exception here silently.
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// retrieve the list of mindmap nodes
		ArrayList<MindmapNode> mindmapNodes = lazyLoaderHandler.getMindmapNodes();
		
		// stop the time measurement and send it to Google Analytics
		long loadDocumentEndTime = System.currentTimeMillis();
	    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
		Log.d(MainApplication.TAG, "Document loaded");

		// TODO: send the number of nodes to Google Analytics. We will need to
		// add a getNumReadNodes getter to the Lazy SAX handler. The SAX handler
		// then counts all nodes it reads (but maybe discards), and we can get
		// the count here (lazyLoaderHandler.getNumReadNodes()).

		// there should be only one, because we have just loaded the root node
		if ( mindmapNodes.size() > 1 ) {
			Log.e(MainApplication.TAG, "Got more than one node while fetching the root node, that's odd!");
		}

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
