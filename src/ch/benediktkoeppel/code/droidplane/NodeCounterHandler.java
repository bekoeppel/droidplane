package ch.benediktkoeppel.code.droidplane;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * The NodeCounterHandler is a simple SAX handler that counts the "<node.../>"
 * tags. It does not generate any MindmapNodes.
 */
class NodeCounterHandler extends DefaultHandler {
	
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
		if (localName.equalsIgnoreCase("node")) {
			nodeCount++;
		}
	}
}
