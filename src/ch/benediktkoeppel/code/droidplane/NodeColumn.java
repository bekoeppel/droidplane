package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import android.util.Log;

/**
 * A column of MindmapNodes, i.e. one level in the mind map. It extends
 * LinearLayout, and then embedds a ListView. This is because we want to have a
 * fine border around the ListView and we can only achieve this by having it
 * wrapped in a LinearLayout with a padding.
 */
public class NodeColumn extends LinearLayout {
	
	/**
	 * This translates ListViews to NodeColumns. We need this because the
	 * OnItemClicked Events come with a ListView (i.e. the ListView which was
	 * clicked) as parent, but we need to find out which NodeColumn was clicked.
	 * This would have been a simple cast if NodeColumn extended ListView, but
	 * we extend LinearLayout and wrap the ListView.
	 */
	private static HashMap<ListView, NodeColumn> listViewToNodeColumn = new HashMap<ListView, NodeColumn>();
	
	/**
	 * the parent node (i.e. the node that is parent to everything we display in this column)
	 */
	private Node parent;
	
	/**
	 * the list of all MindmapNodes which we display in this column
	 */
	private ArrayList<MindmapNode> mindmapNodes;
	
	/**
	 * the adapter for this column
	 */
	MindmapNodeAdapter adapter;
	
	/**
	 * the actual ListView that we'll display
	 */
	ListView listView;
	
	/**
	 * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always throw a IllegalArgumentException.
	 * @deprecated
	 * @param context
	 */
	public NodeColumn(Context context) {
		super(context);
		if ( !isInEditMode() ) {
			throw new IllegalArgumentException(
					"The constructor public NodeColumn(Context context) may only be called by graphical layout tools, i.e. when View#isInEditMode() is true. In production, use the constructor public NodeColumn(Context context, Node parent).");
		}
	}

	/**
	 * Creates a new NodeColumn for a parent node. This NodeColumn is a
	 * LinearLayout, which contains a ListView, which displays all child nodes
	 * of the parent node.
	 * 
	 * @param context
	 * @param parent
	 */
	public NodeColumn(Context context, Node parent) {
		super(context);
		
		// extract all <node.../> elements from the parent node, and add them to the mindmapNodes list
		mindmapNodes = new ArrayList<MindmapNode>();
		NodeList tmp_children = parent.getChildNodes();
		for (int i = 0; i < tmp_children.getLength(); i++) {
			Node tmp_node = tmp_children.item(i);
			
			if ( MindmapNode.isMindmapNode(tmp_node) ) {
				MindmapNode mindmapNode = new MindmapNode(tmp_node);
    			mindmapNodes.add(mindmapNode);
			}
		}
		
		// store the parent node
		this.parent = parent;
		
    	
    	// define the layout of this LinearView
    	int linearViewHeight = LayoutParams.MATCH_PARENT;
    	int linearViewWidth = getOptimalColumnWidth();
    	LinearLayout.LayoutParams linearViewLayout = new LinearLayout.LayoutParams(linearViewWidth, linearViewHeight);
    	setLayoutParams(linearViewLayout);
    	setPadding(0, 0, 1, 0);
    	setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));


		
		// create a ListView
		listView = new ListView(context);
		
    	// define the layout of the listView
    	// should be as high as the parent (i.e. full screen height)
    	int listViewHeight = LayoutParams.MATCH_PARENT;
    	int listViewWidth = LayoutParams.MATCH_PARENT;
    	ViewGroup.LayoutParams listViewLayout = new ViewGroup.LayoutParams(listViewWidth, listViewHeight);
    	listView.setLayoutParams(listViewLayout);
    	listView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));

    	
    	// create adapter (i.e. data provider) for the column
    	adapter = new MindmapNodeAdapter(getContext(), R.layout.mindmap_node_list_item, mindmapNodes);

    	// add the content adapter
    	listView.setAdapter(adapter);
    	
    	// store the ListView to NodeColumn mapping in listViewToNodeColumn
    	listViewToNodeColumn.put(listView, this);
    	
    	// add the listView to the linearView
    	this.addView(listView);
	}
	
	/**
	 * Finds the NodeColumn which contains the given listView.
	 * @param listView
	 * @return the node column which contains listView
	 */
	public static NodeColumn getNodeColumnFromListView(ListView listView) {
		return listViewToNodeColumn.get(listView);
	}

	/**
	 * GUI Helper to detach this LinearView from its parent
	 */
	public void removeFromParent() {
		if ( this.getParent() != null ) {
			((ViewGroup)this.getParent()).removeView(this);
		}
	}
	
	/**
	 * Sets the width of this column to columnWidth
	 * @param columnWidth width of the column
	 */
	private void setWidth(int columnWidth) {
		Log.d(MainApplication.TAG, "Setting column width to " + columnWidth);
		
		ViewGroup.LayoutParams listViewParam = this.getLayoutParams();
		listViewParam.width = columnWidth;
		this.setLayoutParams(listViewParam);
	}
	
	/**
	 * Deselects all nodes of this column
	 */
	public void deselectAllNodes() {
		for (MindmapNode mindmapNode : mindmapNodes) {
			mindmapNode.setSelected(false);
		}
	}
	
	/**
	 * Returns the parent node of this column.
	 * @return the parent node of this colunn
	 */
	public Node getParentNode() {
		return parent;
	}
	
	/**
     * Calculates the column width which this column should have
     * @return
     */
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
    private static int getOptimalColumnWidth() {
    	
    	// and R.integer.horizontally_visible_panes defines how many columns should be visible side by side
    	// so we need 1/(horizontall_visible_panes) * displayWidth as column width
    	int horizontallyVisiblePanes = MainApplication.getStaticApplicationContext().getResources().getInteger(R.integer.horizontally_visible_panes);
        android.view.Display display = ((android.view.WindowManager)MainApplication.getStaticApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	int displayWidth;
        
		// get the Display width. Before HONEYCOMB_MR2, this was display.getWidth, now it is display.getSize
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
        	Point displaySize = new Point();
        	display.getSize(displaySize);
        	displayWidth = displaySize.x;
    	} else {
    		displayWidth = (int)display.getWidth();
    	}
    	int columnWidth = displayWidth / horizontallyVisiblePanes;
    	
    	Log.d(MainApplication.TAG, "Calculated column width = " + columnWidth);
    	
    	return columnWidth;
    }
    
    /**
     * Resizes the column to its optimal column width
     */
    public void resizeColumnWidth() {
    	setWidth(getOptimalColumnWidth());
    }
    
    /**
     * Fetches the MindmapNode at the given position
     * @param position the position from which the MindmapNode should be returned
     * @return MindmapNode
     */
    public MindmapNode getNodeAtPosition(int position) {
    	return mindmapNodes.get(position);
    }
    
	/**
	 * Sets the color on the node at the specified position
	 * @param position
	 */
	public void setItemColor(int position) {
		
		// deselect all nodes
		for (int i = 0; i < mindmapNodes.size(); i++) {
			mindmapNodes.get(i).setSelected(false);
		}
		
		// then select node at position
		mindmapNodes.get(position).setSelected(true);
		
		// then notify about the GUI change
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Clears the item color on all nodes
	 */
	public void clearAllItemColor() {
		for (int i = 0; i < mindmapNodes.size(); i++) {
			mindmapNodes.get(i).setSelected(false);
		}
		
		// then notify about the GUI change
		adapter.notifyDataSetChanged();
	}
	
	/**
	 * Simply a wrapper for ListView's setOnItemClickListener. Technically, the
	 * NodeColumn (which is a LinearView) does not generate OnItemClick Events,
	 * but it's child view (the ListView) does. But it's simpler if the outside
	 * world does not have to care about that detail, so we implement
	 * setOnItemClickListener and just forward the listener to the actual
	 * ListView.
	 * @param listener
	 */
	public void setOnItemClickListener(OnItemClickListener listener) {
		listView.setOnItemClickListener(listener);
	}
	

}



/**
 * The MindmapNodeAdapter is the data provider for the NodeColumn (respectively
 * its ListView).
 */
class MindmapNodeAdapter extends ArrayAdapter<MindmapNode> {
	
	private ArrayList<MindmapNode> mindmapNodes;
	
	public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNode> mindmapNodes) {
		super(context, textViewResourceId, mindmapNodes);
		this.mindmapNodes = mindmapNodes;
	}

	/* (non-Javadoc)
	 * getView is responsible to return a view for each individual element in the ListView
	 * @param int position: the position in the mindmapNodes array, for which we need to generate a view
	 * @param View convertView: the view we should update/modify. null, if we should create a new view
	 * @param ViewGroup parent: not sure, is this the NodeColumn for which the Adapter is generating views?
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@SuppressLint("InlinedApi")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// if convertView was specified, we will use this. Otherwise, we create
		// a new view based on the R.layout.mindmap_node_list_item layout.
		View view = convertView;
		if ( view == null ) {
			LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = layoutInflater.inflate(R.layout.mindmap_node_list_item, null);
		}
		
		// get the node for which we generate the view
		MindmapNode node = mindmapNodes.get(position);
		if ( node != null) {
			
			// the mindmap_node_list_item consists of a ImageView (icon), a TextView (node text), and another TextView ("+" button)
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			icon.setImageResource(node.icon_res_id);
			icon.setContentDescription(node.icon_name);
			
			TextView text = (TextView) view.findViewById(R.id.label);
			text.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
			text.setText(node.text);
			
			TextView expandable = (TextView) view.findViewById(R.id.expandable);
			expandable.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
			if ( node.isExpandable ) {
				expandable.setText("+");
			} else {
				expandable.setText("");
			}
			
			// if the node is selected and has child nodes, give it a special background
			if ( node.getIsSelected() && node.getNumChildMindmapNodes() > 0 ) {
				int backgroundColor;
				
				// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
		    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
		    		backgroundColor = getContext().getResources().getColor(android.R.color.holo_blue_bright);
		    	} else {
		    		backgroundColor = getContext().getResources().getColor(android.R.color.background_dark);
		    	}
				
				view.setBackgroundColor(backgroundColor);	
			} else {
				view.setBackgroundColor(0);
			}
			
		}
		
		Log.d(MainApplication.TAG, "Created a ListView item view");
		
		return view;
	}
	
	
}
