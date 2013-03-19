package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
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
	 * TODO: documentation in the whole file!
	 */
	static class RandomAccessFileReader extends Reader {
		
		private RandomAccessFile randomAccessFile;

		/*
		 * (non-Javadoc) the SAX parser is calling close() when it has finished
		 * the parsing. We don't want that. To close the file, we provide
		 * another method close(boolean force).
		 * 
		 * @see java.io.Reader#close()
		 */
		@Override
		public void close() throws IOException {
			Log.d(MainApplication.TAG, "RandomAccessFileReader close() called - ignoring");
			//randomAccessFile.close();
		}
		
		public void close(boolean force) throws IOException {
			Log.d(MainApplication.TAG, "RandomAccessFileReader close(force=true) called - closing");
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

				// return the number of bytes read
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
//			Log.d(MainApplication.TAG, "NodeCounterHandler got startElement for " + localName);
			if (localName.equalsIgnoreCase("node")) {
				nodeCount++;
			}
		}
	}
	
	static class LazyLoaderHandler extends DefaultHandler {
		
		/**
		 * the list of MindmapNodes we created
		 */
		private ArrayList<MindmapNode> mindmapNodes;
		
		/**
		 * we are always just building one MindmapNode at a time
		 */
		private MindmapNode tmpMindmapNode;
		
		/**
		 * 
		 */
		private Locator locator;
		
		private int nodeLevel;
		
		/**
		 * Returns the generated MindmapNodes. 
		 * TODO: Each MindmapNode will have a "startseek" and "column" set, which should tell us where we found this node in the random access file. Also, the MindmapNode needs to have a method to read one further level of nodes, by starting to read at the seek and read until I don't know where.
		 * @return
		 */
		public ArrayList<MindmapNode> getMindmapNodes() {
			return mindmapNodes;
		}
		
		@Override
		public void startDocument() throws SAXException {
			mindmapNodes = new ArrayList<MindmapNode>();
			nodeLevel = 0;
		}
		
		// TODO: constructor should take a startLine and startColumn, and we don't parse any tag if it is not at least >= these start locators
		// TODO: maybe start with a InputStream and rewind/reopen always when we would do a seek (and remove the additional permission again!!!)
		
		@Override
		public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			
			// TODO: skip if the locator is less then the startLocation specified
			
			// we are only interested in node tags
			if (localName.equalsIgnoreCase("node")) {
				
//				Log.d(MainApplication.TAG, "Found starting node tag at nodeLevel " + nodeLevel);
				
				// if we are at level 0 (not within another node)
				if ( nodeLevel == 0 ) {
					
					// we found a new interesting node, so we generate a new MindmapNode
					tmpMindmapNode = new MindmapNode();
					
					// store its line and column in the file
					tmpMindmapNode.line = locator.getLineNumber();
					tmpMindmapNode.column = locator.getColumnNumber();
					
					// extract the attributes
					for (int i = 0; i < atts.getLength(); i++) {
						String attrName = atts.getLocalName(i);
						String attrValue = atts.getValue(i);
						
						if ( attrName.equalsIgnoreCase("text") ) {
							tmpMindmapNode.text = attrValue;
						}
					}
					
				}
				
				// otherwise, we have found a node within another node, so the node is certainly expandable
				else {
					tmpMindmapNode.isExpandable = true;
				}
				
				// in either case, we are going into a node, so the nodeLevel increases
				nodeLevel++;
				
			} else {
				// TODO: will need to parse icons!
				// get icons (these are sub-attributes, that's a problem) but we can do it later
			}
		}
		
		@Override
		public void endElement(String namespaceURI, String localName, String qName) throws SAXException {

			// we are only interested in node tags
			if (localName.equalsIgnoreCase("node")) {
				
				// this is a closing tag, so the nodeLevel decreases
				nodeLevel--;
				
				// if this closed the node at level 0, then we conclude the tmpMindmapNode and push it to the mindmapNodes
				if ( nodeLevel == 0 ) {
					mindmapNodes.add(tmpMindmapNode);
				}
			}
		}
		

		@Override
		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
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
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// the LazyLoaderHandler will load only the highest level of nodes, which means the root node
		// TODO: can we factor this "loadDocument" out? because it is really just loading the "node" tags with seek=0, nodelevel=0. 
		LazyLoaderHandler lazyLoaderHandler = new LazyLoaderHandler();
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
