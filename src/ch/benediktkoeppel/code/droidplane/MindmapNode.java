package ch.benediktkoeppel.code.droidplane;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import ch.benediktkoeppel.code.droidplane.LazyLoaderHandler.SAXEndNodeFoundException;

import android.util.Log;



/**
 * A MindmapNode is one node in the mind map. It can be implemented in two ways,
 * as a DOM based node or as a SAX based node. If {@link Mindmap} is DOM based,
 * all its MindmapNodes will also be DOM based. This means that the whole XML
 * document exists in RAM (als DOM document in Mindmap), and whenever we explore
 * a branch some new MindmapNodes (based on DOM Nodes from the DOM document) are
 * created. This works fine for small documents, and is probably the only way we
 * can implement the edit functionality.
 * 
 * However, for large documents, a SAX based {@link Mindmap} is more efficient,
 * and will generate SAX based MindmapNodes. The specialty is that we have our
 * own SAX handler, which lazily loads and generates MindmapNodes based on the
 * XML input file, as we explore the map. This means that a node occupies no
 * space (not as document in Mindmap, not as DOM Node somewhere in a library,
 * and not as MindmapNode), unless we have explored its branch.
 * 
 * TODO: see the note in {@link Mindmap}, we should have MindmapNode as
 * interface and then get two implementations, a DOM-based and a SAX-based
 * MindmapNode.
 */
public class MindmapNode {
	
	/**
	 * the XML DOM node from which this MindMapNode is derived. This is only defined if we use a DOM backed node.
	 */
	private Node node;
	
	/**
	 * the random access file if we're reading with the SAX parser. This is only
	 * defined if we use a SAX parser. We need the RandomAccessFile to load
	 * further sub nodes of this node. Note that we won't store the content of
	 * the file, this is only a file handle. Also note that we don't store the
	 * InputStream for a DOM based node, because we will never need to reload
	 * sub nodes from the file, because they are already stored in the Mindmap
	 * DOM document.
	 */
	// TODO: why is this public?
	public RandomAccessFile raf; 

	/**
	 * determines whether this node is based on a DOM Node, or created by the SAX parser
	 */
	public boolean isDOMNode;
	
	/**
	 * the Text of the node (TEXT attribute).
	 */
	public String text;
	
	/**
	 * the name of the first icon
	 */
	public String icon_name;
	
	/**
	 * the Android resource ID of the first icon
	 */
	public int icon_res_id;
	
	/**
	 * whether the node is expandable, i.e. whether it has child nodes
	 * TODO: we could get rid of this!
	 */
	public boolean isExpandable;
	
	/**
	 * whether the node is selected or not, will be set after it was clicked by the user
	 */
	private boolean selected;
	
	// locator position where the node started
	

	/**
	 * startLine and startColumn define the location in the XML file, where this
	 * node tag started. They are only defined if this node is SAX based. The
	 * start position will allow us to seek to this position, and then start
	 * reading subnodes from there.
	 */
	public int startLine;
	public int startColumn;
	
	/**
	 * The number of child nodes. In case of a DOM based node, the number of
	 * child nodes will be counted at the first call to getNumChildNodes, and
	 * then cached. In the case of a SAX based MindmapNode, the number of child
	 * nodes is counted when the node was read from the XML file.
	 */
	public Integer numChildNodes;
	
	/**
	 * The list of child MindmapNodes. childMindmapNodes is populated on demand,
	 * i.e. the first time we call getChildNodes(). Afterwards, the same child
	 * nodes are returned.
	 * 
	 * TODO: while this is nice for efficiency, will it also work when we
	 * implement editing functionality?
	 */
	ArrayList<MindmapNode> childMindmapNodes;
	
	/**
	 * Constructor when creating a SAX based node. All instance fields will be
	 * set to some default, and will have to be set by the SAX handler
	 * afterwards.
	 */
	// TODO: instead of allowing public access to these fields, we should
	// implement setters, but not neccessarily getters.
	public MindmapNode() {
		isExpandable = false;
		text = "";
		icon_name = "";
		icon_res_id = 0;
		node = null;
		selected = false;
		isDOMNode = false;
		numChildNodes = null;
	}
	
	/**
	 * Creates a new MindMapNode from a DOM Node. The node needs to be of type
	 * ELEMENT and have tag "node". Throws a {@link ClassCastException} if the
	 * Node can not be converted to a MindmapNode. Only call this constructor
	 * when the MindmapNode should be DOM based.
	 * 
	 * @param node
	 */
	public MindmapNode(Node node) {
		
		// convert the XML Node to a XML Element
		Element tmp_element;
		if ( isMindmapNode(node) ) {
			tmp_element = (Element)node;
		} else {
			throw new ClassCastException("Can not convert Node to MindmapNode");
		}
		
		// store the Node
		this.node = node;
			
		// extract the string (TEXT attribute) of the nodes
		// TODO: how do we handle rich text nodes?
		text = tmp_element.getAttribute("TEXT");

		// extract icons
		// TODO: how do we handle multiple icons?
		ArrayList<String> icons = getIcons();
		String icon="";
		icon_res_id = 0;
		if ( icons.size() > 0 ) {
			icon = icons.get(0);
			icon_res_id = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
		}

		// find out if it has sub nodes
		isExpandable = ( getNumChildMindmapNodes() > 0 );
		
		// this is based on a DOM node
		isDOMNode = true;
	}
	
	/**
	 * Selects or deselects this node
	 * @param selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Returns whether this node is selected
	 */
	public boolean getIsSelected() {
		return this.selected;
	}
	
	/**
	 * Checks whether a given node can be converted to a Mindmap node, i.e.
	 * whether it has type ELEMENT_NODE and tag "node". This only works for a
	 * DOM based Node.
	 * 
	 * @param node
	 * @return
	 */
	public static boolean isMindmapNode(Node node) {
		
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;

			if (element.getTagName().equals("node")) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Extracts the list of icons from a node and returns the names of the icons
	 * as ArrayList. This currently only works for DOM based nodes.
	 * 
	 * TODO: handle this for SAX based nodes.
	 * 
	 * @return list of names of the icons
	 */
	private ArrayList<String> getIcons() {
		
		ArrayList<String> icons = new ArrayList<String>();
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( n.getNodeType() == Node.ELEMENT_NODE ) {
				Element e = (Element)n;
				
				if ( e.getTagName().equals("icon") && e.hasAttribute("BUILTIN") ) {
					icons.add(getDrawableNameFromMindmapIcon(e.getAttribute("BUILTIN")));
				}
			}
		}
		
		return icons;
	}
	
	/**
	 * Mindmap icons have names such as 'button-ok', but resources have to have
	 * names with pattern [a-z0-9_.]. This method translates the Mindmap icon
	 * names to Android resource names.
	 * 
	 * @param iconName the icon name as it is specified in the XML
	 * @return the name of the corresponding android resource icon
	 */
	private String getDrawableNameFromMindmapIcon(String iconName) {
		Locale locale = MainApplication.getStaticApplicationContext().getResources().getConfiguration().locale;
		String name = "icon_" + iconName.toLowerCase(locale).replaceAll("[^a-z0-9_.]", "_");
		name.replaceAll("_$", "");
		return name;
	}

	/**
	 * returns the number of child Mindmap nodes. If it is DOM based, we iterate through all direct children in the DOM document and count them. If it is SAX based, the number of sub nodes was counted when this node was read from the XML document, so this number is returned. 
	 * 
	 * TODO: we should cache the numbers in both cases, it's not the case at the moment.
	 * 
	 * @return
	 */
	public int getNumChildMindmapNodes() {

		// have to differentiate whether we're a DOM or a SAX node
		// the DOM node loads it's child nodes from the DOM document and counts them
		if (isDOMNode) {
			int numMindmapNodes = 0;

			NodeList childNodes = node.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {

				Node n = childNodes.item(i);
				if (isMindmapNode(n)) {
					numMindmapNodes++;
				}
			}

			return numMindmapNodes;
		}
		
		// the SAX node knows its count already. If something went wrong and we don't know the count, we write an error and return 0 (node can't be expanded). 
		// TODO: maybe we should not return 0, and rely on the getChildNodes() function?
		else {
			if ( numChildNodes != null ) {
				return numChildNodes;
			} else {
				Log.e(MainApplication.TAG, "numChildNodes was null, this shouldn't happen!");
				return 0;
			}
		}
	}
	
	
	
	/**
	 * Generates and returns the child nodes of this MindmapNode.If this MindmapNode is DOM
	 * based, its sub nodes are fetched from the DOM document. If the
	 * MindmapNode is SAX based, the sub nodes are read from the input file
	 * using the {@link LazyLoaderHandler}. In both cases, sub nodes are only
	 * fetched once, afterwards a cached ArrayList is served.
	 * @return ArrayList of this MindmapNode's child nodes
	 */

	// TODO: we should have a possiblity to truncate nodes after some times, i.e. discard their childMinamapNodes list if we don't use the node anymore.

	public ArrayList<MindmapNode> getChildNodes() {
		
		// if we haven't loaded the childMindmapNodes before
		if ( childMindmapNodes == null ) {
			
			// determine whether this is a DOM or a SAX node
			// it's a DOM node
			if ( isDOMNode == true ) {
				
				// fetch all child DOM Nodes, convert them to MindmapNodes
				childMindmapNodes = new ArrayList<MindmapNode>();
				NodeList childNodes = node.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++) {
					Node tmpNode = childNodes.item(i);
					
					if ( isMindmapNode(tmpNode) ) {
						MindmapNode mindmapNode = new MindmapNode(tmpNode);
						childMindmapNodes.add(mindmapNode);
					}
				}
				Log.d(MainApplication.TAG, "Returning newly generated childMindmapNodes from DOM document");
				return childMindmapNodes;
			} 
			
			// it's a SAX node
			else {
				
				Log.d(MainApplication.TAG, "Fetching childMindmapNodes from Lazy SAX");
				
				// fetch the next level of child nodes with the SAX parser
				childMindmapNodes = new ArrayList<MindmapNode>();
				
				// rewind the randomAccessFile. If a problem happens with
				// rewinding, we'll simply return an empty node list (but not
				// cache it).
				try {
					raf.seek(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return new ArrayList<MindmapNode>();
				}
				
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

				// use the lazy SAX parser, but instruct it to skip everything before this node tag. We want to load the 1st sub-node level.
				// TODO: add support to load multiple levels of subnodes.
				LazyLoaderHandler lazyLoaderHandler = new LazyLoaderHandler(raf, startLine, startColumn, 1);
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
					// as explained in Mindmap#loadDocument(RandomAccessFile),
					// this signals that the Lazy SAX handler found the end of
					// this node and has stopped the operation there.
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// retrieve the list of mindmap nodes and cache them in childMindmapNodes
				childMindmapNodes = lazyLoaderHandler.getMindmapNodes();
				
				// TODO: we have to think where and when we rewind the
				// RandomAccessFile. Do we want to do it whenever we start
				// parsing it? Or do we want to do it whenever we have finished
				// parsing it? At the moment, we do it at arbitrary position in
				// the code, and that's going to introduce problems sometime
				// soon. We should wrap this somewhere away and not have to care
				// about it anymore.

				// returning the lazy loaded sub nodes
				Log.d(MainApplication.TAG, "Returning newly generated childMindmapNodes");
				return childMindmapNodes;
			}
		}
		
		// we already loaded the nodes before, return the previous result
		else {
			Log.d(MainApplication.TAG, "Returning cached childMindmapNodes");
			return childMindmapNodes;
		}
	}
}
