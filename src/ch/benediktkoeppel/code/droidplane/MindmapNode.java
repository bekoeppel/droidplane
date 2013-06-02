package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	 * the XML DOM node from which this MindMapNode is derived
	 */
	private Node node;
	
	/**
	 * whether the node is selected or not, will be set after it was clicked by the user
	 */
	public boolean selected;
	
	/**
	 * The list of child MindmapNodes. We support lazy loading.
	 */
	ArrayList<MindmapNode> childMindmapNodes;
	
	
	/**
	 * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". 
	 * Throws a {@link ClassCastException} if the Node can not be converted to a MindmapNode. 
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
		text = tmp_element.getAttribute("TEXT");

		// extract icons
		ArrayList<String> icons = getIcons();
		String icon="";
		icon_res_id = 0;
		if ( icons.size() > 0 ) {
			icon = icons.get(0);
			icon_res_id = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
		}

		// find out if it has sub nodes
		isExpandable = ( getNumChildMindmapNodes() > 0 );
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
	 * @return
	 */
	public int getNumChildMindmapNodes() {
		
		int numMindmapNodes = 0;
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( isMindmapNode(n) ) {
				numMindmapNodes++;
			}
		}
		
		return numMindmapNodes;
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
			Log.d(MainApplication.TAG, "Returning newly generated childMindmapNodes");
			return childMindmapNodes;
		}
		
		// we already did that before, so return the previous result
		else {
			Log.d(MainApplication.TAG, "Returning cached childMindmapNodes");
			return childMindmapNodes;
		}
	}
}
