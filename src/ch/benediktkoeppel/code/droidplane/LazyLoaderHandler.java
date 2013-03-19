package ch.benediktkoeppel.code.droidplane;

import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * The LazyLoaderHandler is a SAX handler that only loads one level of node
 * elements from a XML. Each MindmapNode will have a start position ("startLine"
 * and "startColumn") set, which should tell us where we found this node in the
 * random access file. Also, the MindmapNode gets a method to read one further
 * level of nodes, by starting to read at the seek and read until it has found
 * all its subnodes.
 */
// TODO: at some point in time, we will want to pre-load sub-sub-nodes. At the
// moment, the LazyLoaderHandler can only load one level of nodes. We should
// have the "tmpMindmapNode" as a stack, and provide not only a targetNodeLevel,
// but a startNodeLevel and endNodeLevel, and then extract all nodes that are on
// a level between startNodeLevel and endNodeLevel. To do that, we need to have
// multiple tmpMindmapNodes, which we can store in a Stack (push when going
// down, pop from the stack when going up). When popping, we'll have to add them
// also to the child-node-list of it's parent (which is at that time then the
// peek of the Stack).
class LazyLoaderHandler extends DefaultHandler {
	
	/**
	 * the list of MindmapNodes we created from this file
	 */
	private ArrayList<MindmapNode> mindmapNodes;
	
	/**
	 * we are always just building one MindmapNode at a time
	 */
	private MindmapNode tmpMindmapNode;
	
	/**
	 * With locator we can determine the current line and column position of the reader
	 */
	private Locator locator;
	
	/**
	 * stores the current level (into how many levels of node tags are we wrapped right now)?
	 */
	private int nodeLevel;

	/**
	 * startLine and startColumn define the start position of the reader. The
	 * reader will skip until it reaches the tag at the start position. The tag
	 * right at the start position is also read.
	 */
	private int startLine;
	private int startColumn;
	
	/**
	 * we will export only nodes of one level, the targetLevel. To get a root
	 * node, we have to set targetLevel = 0, to get sub nodes N1...Nx of another
	 * node N0, we start at this node N0 and set the targetLevel = 1.
	 */
	private int targetLevel;
	
	/**
	 * this is the randomAccessFile we're reading from. We need this to set it
	 * into the MindmapNodes we create. TODO: do we really really need this?
	 * It's none of LazyLoaderHandler's business whether it's reading from a
	 * random access file or not.
	 */
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
	 * @return
	 */
	public ArrayList<MindmapNode> getMindmapNodes() {
		return mindmapNodes;
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
	
	/* (non-Javadoc)
	 * Called at the start of the document. We reset the variables here.
	 * @see org.xml.sax.helpers.DefaultHandler#startDocument()
	 */
	@Override
	public void startDocument() throws SAXException {
		mindmapNodes = new ArrayList<MindmapNode>();
		nodeLevel = 0;
	}
	
	/*
	 * (non-Javadoc)
	 * Called for each start tag of the XML document. We need to
	 * find out whether it's a node, whether we're interested in that node, and
	 * build up the node's attributes.
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		
		// if we haven't reached the start position yet, we skip this startElement
		if ( !isAfterStartPosition() ) {
			return;
		}
		
		// we are only interested in node tags
		if (localName.equalsIgnoreCase("node")) {
			
			// if we are at the target level, we build up nodes
			if ( nodeLevel == targetLevel ) {
				
				// we found a new interesting node, so we generate a new MindmapNode
				tmpMindmapNode = new MindmapNode();
				
				// store its line and column in the file
				tmpMindmapNode.startLine = locator.getLineNumber();
				tmpMindmapNode.startColumn = locator.getColumnNumber();
				tmpMindmapNode.raf = randomAccessFile;
				
				// so far, we have found 0 children of this node
				tmpMindmapNode.numChildNodes = 0;
				
				// extract the attributes of the node (i.e. TEXT)
				for (int i = 0; i < atts.getLength(); i++) {
					String attrName = atts.getLocalName(i);
					String attrValue = atts.getValue(i);
					
					if ( attrName.equalsIgnoreCase("text") ) {
						tmpMindmapNode.text = attrValue;
					}
				}
			}
			
			// otherwise, if we have found a node on a higher (deeper) level
			// than targetLevel, it's a (sub-...sub-)sub-node of the node we're
			// currently building up. This means that the tmpMindmapNode has one
			// more child, and is certainly expandable.
			// TODO: this is not really true. At the moment, we not only count
			// direct children of the node, but also grand-children and so on.
			// We should check whether nodeLevel = targetLevel + 1.
			else if ( nodeLevel > targetLevel ) {
				tmpMindmapNode.isExpandable = true;
				tmpMindmapNode.numChildNodes++;
			}
			
			// otherwise we're above the start node, so nothing required
			else { /* empty */ }
			
			// in either case, we are going into a node, so the nodeLevel increases
			nodeLevel++;
			
		} else {
			// TODO: here we'll need to parse all other tags, that might occur
			// within a <node> tag. For example icons, links, etc.
		}
	}
	
	/*
	 * (non-Javadoc)
	 * Called when an element ends, i.e. when we get the ending tag. Once we
	 * reach the end tag of the node we're building up, we can push this node to
	 * our list of nodes.
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
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
			
			// if we are at level 0, we are at the level where we started.
			// Everything after here will be in a different tree (i.e. subnode
			// of a completely different node), so we can stop.
			// in order to let the SAX parser know that we're done, we have to
			// raise an Exception. There is no friendlier way to do this.
			if ( nodeLevel == 0 ) {
				throw new SAXEndNodeFoundException();
			}
		}
		
		else {
			// TODO: here goes the closing logic for all other tags (links,
			// icons, ..., see in startElement(...))
		}
	}
	
	/*
	 * (non-Javadoc)
	 * Whenever we start parsing a document, the Locator will get set
	 * automatically. This allows us to query our current position in the
	 * start/endElement callbacks.
	 * @see
	 * org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator
	 * )
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}
	
	/**
	 * The SAXEndNodeFoundException signals that we have found the end of a node
	 * and don't want to continue to read the file.
	 */
	class SAXEndNodeFoundException extends SAXException {
		private static final long serialVersionUID = 7974532653077143185L;
	}

}
