package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.analytics.tracking.android.EasyTracker;

import android.net.Uri;
import android.util.Log;

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
	 * @throws XmlPullParserException 
	 * @throws IOException 
	 */
	public static int getNodeCount(InputStream inputStream) throws XmlPullParserException, IOException {
		
		int nodeCount = 0;
		
		XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
		pullParserFactory.setNamespaceAware(false);
		
		XmlPullParser parser = pullParserFactory.newPullParser();
		
		parser.setInput(new InputStreamReader(inputStream));

		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if ( eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("node") ) {
				nodeCount++;
			}
            eventType = parser.next();
		}
		
		return nodeCount;
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
