package ch.benediktkoeppel.code.droidplane;

import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


class LazyLoaderHandler extends DefaultHandler {
	
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

	private int startLine;
	private int startColumn;
	private int targetLevel;
	private RandomAccessFile randomAccessFile;
	
	/**
	 * starts a normal handler, i.e. reading from the beginning of the file, and fetching the 0th level (i.e. the root node)
	 */
	public LazyLoaderHandler(RandomAccessFile randomAccessFile) {
		this.startLine = 0;
		this.startColumn = 0;
		this.targetLevel = 0;
		this.randomAccessFile = randomAccessFile;
	}
	
	/**
	 * This allows us to read from a custom position, i.e. to find subnodes of a
	 * specific node. startLine/Column is the position where this node started.
	 * If we want to get the subnodes, we'll have to fetch nodes at
	 * targetLevel=1.
	 * @param startLine
	 * @param startColumn
	 * @param targetLevel
	 */
	public LazyLoaderHandler(RandomAccessFile randomAccessFile, int startLine, int startColumn, int targetLevel) {
		this.randomAccessFile = randomAccessFile;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.targetLevel = targetLevel;
	}
	
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
	
	/**
	 * Checks whether the current locator is at or after the start position
	 * which was specified in the constructor.
	 * @return returns true if the locator is at or after the start position
	 */
	private boolean isAfterStartPosition() {
		int currentLine = locator.getLineNumber();
		int currentColumn = locator.getColumnNumber();
		
		if ( currentLine < startLine || (currentLine==startLine && currentColumn<startColumn) ) {
			return false;
		} else {
			return true;
		}
		
	}
	
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		
		// if we haven't reached the start position yet, we skip this startElement
		if ( !isAfterStartPosition() ) {
			return;
		}
		
		// we are only interested in node tags
		if (localName.equalsIgnoreCase("node")) {
			
//			Log.d(MainApplication.TAG, "Found starting node tag at nodeLevel " + nodeLevel);
			
			// if we are at level 0 (not within another node)
			if ( nodeLevel == targetLevel ) {
				
				// we found a new interesting node, so we generate a new MindmapNode
				tmpMindmapNode = new MindmapNode();
				
				// store its line and column in the file
				tmpMindmapNode.startLine = locator.getLineNumber();
				tmpMindmapNode.startColumn = locator.getColumnNumber();
				tmpMindmapNode.raf = randomAccessFile;
				tmpMindmapNode.numChildNodes = 0;
				
				// extract the attributes
				for (int i = 0; i < atts.getLength(); i++) {
					String attrName = atts.getLocalName(i);
					String attrValue = atts.getValue(i);
					
					if ( attrName.equalsIgnoreCase("text") ) {
						tmpMindmapNode.text = attrValue;
					}
				}
				
			}
			
			// otherwise, if we have found a node within another node, so the node is certainly expandable
			else if ( nodeLevel > targetLevel ) {
				tmpMindmapNode.isExpandable = true;
				tmpMindmapNode.numChildNodes++;
			}
			
			// otherwise we're above the start node, so nothing required
			else { /* empty */ }
			
			// in either case, we are going into a node, so the nodeLevel increases
			nodeLevel++;
			
		} else {
			// TODO: will need to parse icons!
			// get icons (these are sub-attributes, that's a problem) but we can do it later
		}
	}
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		
		// if we haven't reached the start position yet, we skip this endElement
		if ( !isAfterStartPosition() ) {
			return;
		}
		
		// we are only interested in node tags
		if (localName.equalsIgnoreCase("node")) {
			
			// this is a closing tag, so the nodeLevel decreases
			nodeLevel--;
			
			// if this closed the node at level the target level, then we conclude the tmpMindmapNode and push it to the mindmapNodes
			if ( nodeLevel == targetLevel ) {
				mindmapNodes.add(tmpMindmapNode);
			}
			
			// we are at level 0, i.e. at the level we started. Everything after here will be in a different tree
			if ( nodeLevel == 0 ) {
				throw new SAXEndNodeFoundException();
			}
		}
	}
	

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}
	
	
	class SAXEndNodeFoundException extends SAXException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7974532653077143185L;

	}

}
