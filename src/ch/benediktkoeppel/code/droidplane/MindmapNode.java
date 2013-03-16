package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class MindmapNode {
	
	public String text;
	public String icon_name;
	public int icon_res_id;
	public boolean isExpandable;
	public Node node;
//	public View view;
	public boolean selected;
	
	public MindmapNode(Node node) {
		
		Element tmp_element;
		if ( isMindmapNode(node) ) {
			tmp_element = (Element)node;
		} else {
			throw new ClassCastException("Can not convert Node to MindmapNode");
		}
		
		this.node = node;
			
		// extract the string (TEXT attribute) of the nodes
		// TODO: how do we handle rich text nodes?
		String text = tmp_element.getAttribute("TEXT");

		// extract icons
		// TODO: how do we handle multiple icons?
		ArrayList<String> icons = getIcons();
		String icon="";
		int icon_id = 0;
		if ( icons.size() > 0 ) {
			icon = icons.get(0);
			icon_id = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
		}

		// find out if it has sub nodes
		boolean expandable = ( getNumChildMindmapNodes() > 0 );
	}
	
//	public MindmapNode(String text, String icon_name, int icon_res_id, boolean isExpandable, Node node) {
//		this.text = text;
//		this.icon_name = icon_name;
//		this.icon_res_id = icon_res_id;
//		this.isExpandable = isExpandable;
//		this.node = node;
//	}
	
	public Node getNode() {
		return this.node;
	}
//
//	public void setView(View view) {
//		this.view = view;
//	}
//	
//	public View getView() {
//		return this.view;
//	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean getIsSelected() {
		return this.selected;
	}
	
	/**
	 * Checks whether a given node can be converted to a Mindmap node, i.e. whether it has type ELEMENT_NODE and tag "node"
	 * @param node
	 * @return
	 */
	public static boolean isMindmapNode(Node node) {
		
		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			Element element = (Element)node;
			
			if ( element.getTagName().equals("node") ) {
				return true;
			}
		}
		
		return false;
		
	}
	



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
	
	
	

	// Mindmap icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]
	private String getDrawableNameFromMindmapIcon(String iconName) {
		Locale locale = MainApplication.getStaticApplicationContext().getResources().getConfiguration().locale;
		String name = "icon_" + iconName.toLowerCase(locale).replaceAll("[^a-z0-9_.]", "_");
		name.replaceAll("_$", "");
		return name;
	}
	
	

	// returns the number of child Mindmap nodes 
	// "Mindmap node" = XML node && ELEMENT_NODE && "node" tag
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
	
	
	
	
}
