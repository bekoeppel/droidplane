package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.net.Uri;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
public class Mindmap {
	
	/**
	 * the XML DOM document, the mind map
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
	 * TODO: documentation in the whole file!
	 */
	static class RandomAccessFileReader extends Reader {
		
		private RandomAccessFile randomAccessFile;

		@Override
		public void close() throws IOException {
			Log.d(MainApplication.TAG, "RandomAccessFileReader close() called");
			randomAccessFile.close();
		}

		@Override
		public int read(char[] buffer, int byteOffset, int byteCount) throws IOException {
			
			// first read into a byteBuffer
			byte[] byteBuffer = new byte[byteCount];
			int numBytesRead = randomAccessFile.read(byteBuffer, byteOffset, byteCount);

			// check if we have reached the end of the file
			if (numBytesRead == -1) {
				return -1;
			}
			
			// translate the read bytes into characters
			else {
				
				// the input file has US-ASCII encoding, and we transfer all characters into the buffer
				new String(byteBuffer, "US-ASCII").getChars(0, numBytesRead, buffer, 0);

				// return the number of bytes read TODO
				return numBytesRead;
			}
		}
		
		public RandomAccessFileReader(RandomAccessFile randomAccessFile) {
			this.randomAccessFile = randomAccessFile;
		}
		
	}
	
	/**
	 * The NodeCounterHandler is a SAX handler that counts the "<node.../>" tags.
	 */
	static class NodeCounterHandler extends DefaultHandler {
		
		/**
		 * the total count of Node elements
		 */
		private int nodeCount;
		
		/**
		 * Returns the count of nodes. First, call parse(...)!
		 * @return
		 */
		public int getNodeCount() {
			return nodeCount;
		}
		
		/* (non-Javadoc)
		 * Will be called at the start of the document. We reset the count to zero.
		 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
		 */
		@Override
		public void startDocument() throws SAXException {
			nodeCount = 0;
		}
		
		/* (non-Javadoc)
		 * Will be called for every element in the mind map. If the tag name is "node", we increment the counter.
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override	
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException { 
			Log.d(MainApplication.TAG, "NodeCounterHandler got startElement for " + localName);
			if (localName.equalsIgnoreCase("node")) {
				nodeCount++;
			}
		}
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
	
	// TODO: support loading the document from a RandomAccessFile with a SAX parser
	
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
	
	/**
	 * Returns the root node of the currently loaded mind map
	 * @return the root node
	 */
	public MindmapNode getRootNode() {
		return rootNode;
	}


}
