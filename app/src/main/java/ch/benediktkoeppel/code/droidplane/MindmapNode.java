package ch.benediktkoeppel.code.droidplane;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;


/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a
 * MindMapNode if it has type ELEMENT, and tag "node".
 */
public class MindmapNode extends LinearLayout {
	
	/**
	 * the Text of the node (TEXT attribute).
	 */
	public String text;

    /**
     * bold style
     */
    private boolean isBold = false;

    /**
     * italic style
     */
    private boolean isItalic = false;
	
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
	 * if the node has a LINK attribute, it will be stored in Uri link
	 */
	public Uri link;
	
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
	 * True, when this MindmapNode has an inflated layout. Otherwise false.
	 */
	private Boolean isLayoutInflated = false;
	
	/**
	 * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always throw a IllegalArgumentException.
	 * @deprecated
	 * @param context
	 */
	public MindmapNode(Context context) {
		super(context);
		if ( !isInEditMode() ) {
			throw new IllegalArgumentException(
					"The constructor public MindmapNode(Context context) may only be called by graphical layout tools, i.e. when View#isInEditMode() is true. In production, use the constructor public MindmapNode(Context context, Node node).");
		}
	}

	/**
	 * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". 
	 * Throws a {@link ClassCastException} if the Node can not be converted to a MindmapNode. 
	 * @param node
	 */
	public MindmapNode(Context context, Node node) {
		
		super(context);
		
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

        // extract styles
        NodeList styleNodeList = tmp_element.getChildNodes();
        for (int i = 0; i < styleNodeList.getLength(); i++) {
            Node n = styleNodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("font")) {
                Element fontElement = (Element) n;
                if (fontElement.hasAttribute("BOLD") && fontElement.getAttribute("BOLD").equals("true")) {
                    Log.d(MainApplication.TAG, "Found bold node");
                    this.isBold = true;
                }
                if (fontElement.hasAttribute("ITALIC") && fontElement.getAttribute("ITALIC").equals("true")) {
                    this.isItalic = true;
                }
            }
        }

        // if no text, use richcontent (HTML)
        if (text == null || text.equals("")) {
            // find 'richcontent TYPE="NODE"' subnode, which will contain the rich text content
            NodeList richtextNodeList = tmp_element.getChildNodes();
            for (int i = 0; i < richtextNodeList.getLength(); i++) {
                Node n = richtextNodeList.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("richcontent")) {
                    Element richcontentElement = (Element) n;
                    if (richcontentElement.getAttribute("TYPE").equals("NODE")) {
                        text = Html.fromHtml(richcontentElement.getTextContent()).toString();
                    }
                }
            }
        }

        // extract icons
		ArrayList<String> icons = getIcons();
		String icon="";
		icon_res_id = 0;
		if ( icons.size() > 0 ) {
			icon = icons.get(0);
			icon_res_id = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
		}

        // extract link and set link icon if node has a link
        String linkAttribute = tmp_element.getAttribute("LINK");
        if ( !linkAttribute.equals("") ) {
            link = Uri.parse(linkAttribute);
            icon_res_id = MainApplication
                    .getStaticApplicationContext()
                    .getResources()
                    .getIdentifier("@drawable/link", "id",
                            MainApplication.getStaticApplicationContext().getPackageName());
        }


        // find out if it has sub nodes
		isExpandable = ( getNumChildMindmapNodes() > 0 );
		
		// load the layout from the XML file
		// TODO: we should not inflate the layout straight away, but only when the MindmapNode is displaye (lazy loading)
//        MindmapNode.inflate(context, R.layout.mindmap_node_list_item, this);
//        refreshView();
		
	}
	
	/**
	 * Inflates the layout from the XML file if it is not yet inflated
	 * @param context
	 */
	private void inflateLayout(Context context) {
		synchronized (isLayoutInflated ) {
			if (!isLayoutInflated) {
				MindmapNode.inflate(context, R.layout.mindmap_node_list_item, this);
			}
		}
	}
	
	@SuppressLint("InlinedApi")
	public void refreshView() {
		
		// inflate the layout if we haven't done so yet
		inflateLayout(getContext());
		
		// the mindmap_node_list_item consists of a ImageView (icon), a TextView (node text), and another TextView ("+" button)
		ImageView iconView = (ImageView)findViewById(R.id.icon);
		iconView.setImageResource(icon_res_id);
		iconView.setContentDescription(icon_name);
		
		TextView textView = (TextView) findViewById(R.id.label);
		textView.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
        SpannableString spannableString = new SpannableString(text);
        if (isBold) {
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableString.length(), 0);
        }
        if (isItalic) {
            spannableString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannableString.length(), 0);
        }
		textView.setText(spannableString);

		ImageView expandable = (ImageView) findViewById(R.id.expandable);
		if ( isExpandable ) {
			if ( getIsSelected() ) {
				expandable.setImageResource(R.drawable.minus_alt);
			} else {
				expandable.setImageResource(R.drawable.plus_alt);
			}
		}
		
		// if the node is selected and has child nodes, give it a special background
		if ( getIsSelected() && getNumChildMindmapNodes() > 0 ) {
			int backgroundColor;
			
			// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
	    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
	    		backgroundColor = getContext().getResources().getColor(android.R.color.holo_blue_bright);
	    	} else {
	    		backgroundColor = getContext().getResources().getColor(android.R.color.darker_gray);
	    	}
			
			setBackgroundColor(backgroundColor);	
		} else {
			setBackgroundColor(0);
		}	
		
		// set the layout parameter
		// TODO: this should not be necessary. The problem is that the inflate
		// (in the constructor) loads the XML as child of this LinearView, so
		// the MindmapNode-LinearView wraps the root LinearView from the
		// mindmap_node_list_item XML file.
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT));
		setGravity(Gravity.LEFT | Gravity.CENTER);
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
	 * The NodeColumn forwards the CreateContextMenu event to the appropriate
	 * MindmapNode, which can then generate the context menu as it likes. Note
	 * that the MindmapNode itself is not registered as the listener for such
	 * events per se, because the NodeColumn first has to decide for which
	 * MindmapNode the event applies.
	 * 
	 * @param menu
	 * @param v
	 * @param menuInfo
	 */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		// build the menu
		menu.setHeaderTitle(text);
		menu.setHeaderIcon(icon_res_id);
		
		// TODO: add a submenu showing all icons
		// Context menus do not support icons.
//		SubMenu iconMenu = menu.addSubMenu("All icons");
//		ArrayList<String> icons = getIcons();
//		for (int i = 0; i < icons.size(); i++) {
//			String icon = icons.get(i);
//			MenuItem iconMenuItem = iconMenu.add(icon);
//			int iconMenuItemResId = MainApplication.getStaticApplicationContext().getResources().getIdentifier("@drawable/" + icon, "id", MainApplication.getStaticApplicationContext().getPackageName());
//			iconMenuItem.setIcon(MainApplication.getStaticApplicationContext().getResources().getDrawable(iconMenuItemResId));
//		}

		// allow copying the node text
		menu.add(0, R.id.contextcopy, 0, R.string.copynodetext);

		// add menu to open link, if the node has a hyperlink
		if ( link != null ) {
			menu.add(0, R.id.contextopenlink, 0, R.string.openlink);
		}
	}
		
		
	/*
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
					MindmapNode mindmapNode = new MindmapNode(getContext(), tmpNode);
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

    /**
     * Opens the link of this node (if any)
     */
    public void openLink() {

        // try opening the link normally with an intent
        try {
            Intent openUriIntent = new Intent(Intent.ACTION_VIEW);
            openUriIntent.setData(this.link);
            MainApplication.getMainActivityInstance().startActivity(openUriIntent);
            return;
        } catch (ActivityNotFoundException e) {
            Log.d(MainApplication.TAG, "ActivityNotFoundException when opening link as normal intent");
        }

        // try to open as relative file
        try {
            // get path of mindmap file
            String fileName = "no file";
            if (this.link.getPath().startsWith("/")) {
                // absolute filename
                fileName = this.link.getPath();
            } else {

                // link is relative to mindmap file
                String mindmapPath = MainApplication.getInstance().mindmap.getUri().getPath();
                Log.d(MainApplication.TAG, "Mindmap path " + mindmapPath);
                String mindmapDirectoryPath = mindmapPath.substring(0, mindmapPath.lastIndexOf("/"));
                Log.d(MainApplication.TAG, "Mindmap directory path " + mindmapDirectoryPath);
                fileName = mindmapDirectoryPath + "/" + this.link.getPath();

            }
            File file = new File(fileName);
            if (!file.exists()) {
                Toast.makeText(getContext(), "File " + fileName + " does not exist.", Toast.LENGTH_SHORT).show();
                Log.d(MainApplication.TAG, "File " + fileName + " does not exist.");
                return;
            }
            if (!file.canRead()) {
                Toast.makeText(getContext(), "Can not read file " + fileName + ".", Toast.LENGTH_SHORT).show();
                Log.d(MainApplication.TAG, "Can not read file " + fileName + ".");
                return;
            }
            Log.d(MainApplication.TAG, "Opening file " + Uri.fromFile(file));
            // http://stackoverflow.com/a/3571239/1067124
            String extension = "";
            int i = fileName.lastIndexOf('.');
            int p = fileName.lastIndexOf('/');
            if (i > p) {
                extension = fileName.substring(i+1);
            }
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mime);
            MainApplication.getMainActivityInstance().startActivity(intent);
        } catch (Exception e1) {
            Toast.makeText(getContext(), "No application found to open " + this.link, Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
        }

    }
}
