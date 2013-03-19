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
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a
 * MindMapNode if it has type ELEMENT, and tag "node".
 */
public class MindmapNode {
	
	/**
	 * the Text of the node (TEXT attribute).
	 */
	public String text;
	
	/**
	 * the name of the icon
	 */
	public String icon_name;
	
	/**
	 * the Android resource ID of the icon
	 */
	public int icon_res_id;
	
	/**
	 * whether the node is expandable, i.e. whether it has child nodes
	 */
	public boolean isExpandable;
	
	/**
	 * the XML DOM node from which this MindMapNode is derived. This is only defined if we use a DOM parser.
	 */
	private Node node;
	
	/**
	 * whether the node is selected or not, will be set after it was clicked by the user
	 */
	public boolean selected;
	
	/**
	 * determines whether this node is based on a DOM Node, or created by the SAX parser
	 */
	public boolean isDOMNode;
	
	/**
	 * the random access file if we're reading with the SAX parser. This is only defined if we use a SAX parser.
	 */
	public RandomAccessFile raf; 
	
	// locator position where the node started
	public int startLine;
	public int startColumn;
	// TOOD: if it is a SAX node, we need a pointer to a inputsource and a seek, where we can continue to load more nodes (subnodes)
	
	public Integer numChildNodes;
	
	/**
	 * The list of child MindmapNodes. We support lazy loading.
	 */
	ArrayList<MindmapNode> childMindmapNodes;
	
	/**
	 * Call this when creating a SAX node
	 * TODO: more documentation
	 */
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
	 * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". 
	 * Throws a {@link ClassCastException} if the Node can not be converted to a MindmapNode. 
	 * TODO: documentation: only call this when creating a new node from a DOM node
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
	 * Returns the XML Node of which this MindmapNode was derived
	 * @return
	 */
//	public Node getNode() {
//		return this.node;
//	}
	
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
	 * whether it has type ELEMENT_NODE and tag "node"
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
	 * as ArrayList.
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
	 * returns the number of child Mindmap nodes 
	 * TODO: support lazy loading from getChildMindmapNodes().size()
	 * @return
	 */
	public int getNumChildMindmapNodes() {

		// TODO: have to differentiate whethe rwe're a DOM or a SAX node
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
		} else {
			if ( numChildNodes != null ) {
				return numChildNodes;
			} else {
				Log.e(MainApplication.TAG, "numChildNodes was null, this shouldn't happen!");
				return 0;
			}
			
		}
	}
	
	/**
	 * Generates and returns the child nodes of this MindmapNode.
	 * getChildNodes() does lazy loading, i.e. it generates the child nodes on
	 * demand and stores them in childMindmapNodes.
	 * @return ArrayList of this MindmapNode's child nodes
	 */
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
				
				// rewind the randomAccessFile
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
				} catch (SAXEndNodeFoundException e) {
					/* empty */
					// TODO
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// use the lazy SAX parser, but instruct it to skip everything before this node tag. We want to load the 1st sub-node level
				LazyLoaderHandler lazyLoaderHandler = new LazyLoaderHandler(raf, startLine, startColumn, 1);
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
				childMindmapNodes = lazyLoaderHandler.getMindmapNodes();

				// returning the lazy loaded sub nodes
				Log.d(MainApplication.TAG, "Returning newly generated childMindmapNodes");
				return childMindmapNodes;
			}
		}
		
		// we already did that before, so return the previous result
		else {
			Log.d(MainApplication.TAG, "Returning cached childMindmapNodes");
			return childMindmapNodes;
		}

		// TODO: we should have a possiblity to truncate nodes after some times, i.e. discard their childMinamapNodes list if we don't use the node anymore.

	}
}
