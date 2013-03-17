package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

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
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

/**
 * A column of MindmapNodes, i.e. one level in the mind map.
 */
public class NodeColumn extends ListView {
	
	// the parent node (i.e. the node that is parent to everything we display in this column)
	private Node parent;
	
	// the list of all MindmapNodes which we display in this column
	private ArrayList<MindmapNode> mindmapNodes;
	
	// the adapter for this column
	MindmapNodeAdapter adapter;

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

		
    	// define the layout of the listView
    	// should be as high as the parent (i.e. full screen height)
    	int height = LayoutParams.MATCH_PARENT;
    	int width = getColumnWidth();
    	ViewGroup.LayoutParams listViewLayout = new ViewGroup.LayoutParams(width, height);

    	// set the defined layout
    	// TODO: really shouldn't do this. node column can do it itself.
    	setLayoutParams(listViewLayout);

    	
    	// create adapter (i.e. data provider) for the column
    	adapter = new MindmapNodeAdapter(getContext(), R.layout.mindmap_node_list_item, mindmapNodes);

    	// add the content adapter
    	setAdapter(adapter);
    	
	}

	/**
	 * GUI Helper to detach this ListView from its parent
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
	public void setWidth(int columnWidth) {
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
	// TODO: is this really at the right spot here? It should go into some GUI helper class and might even be static
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
    private int getColumnWidth() {
    	
    	// and R.integer.horizontally_visible_panes defines how many columns should be visible side by side
    	// so we need 1/(horizontall_visible_panes) * displayWidth as column width
    	int horizontallyVisiblePanes = getContext().getResources().getInteger(R.integer.horizontally_visible_panes);
        android.view.Display display = ((android.view.WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
     * Fetches the MindmapNode at the given position
     * @param position the position from which the MindmapNode should be returned
     * @return MindmapNode
     */
    public MindmapNode getNodeAtPosition(int position) {
    	return mindmapNodes.get(position);
    }
    
    


	// Sets the color on the node at the specified position
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
	
	// Clears the item color on all nodes
	public void clearAllItemColor() {
		for (int i = 0; i < mindmapNodes.size(); i++) {
			mindmapNodes.get(i).setSelected(false);
		}
		
		// then notify about the GUI change
		adapter.notifyDataSetChanged();
	}

	

}



/**
 * The MindmapNodeAdapter is the data provider for the NodeColumn (respectively
 * its ListView).
 */
class MindmapNodeAdapter extends ArrayAdapter<MindmapNode> {
	
	// TODO: do we need the mindmapNodes array? this is already in NodeColumn, so we could use it from there
	private ArrayList<MindmapNode> mindmapNodes;
	
	public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNode> mindmapNodes) {
		super(context, textViewResourceId, mindmapNodes);
		this.mindmapNodes = mindmapNodes;
	}
//	
//	public ArrayList<Node> getNodeList() {
//		ArrayList<Node> nodeList = new ArrayList<Node>();
//		for (int i = 0; i < mindmapNodes.size(); i++) {
//			nodeList.add(mindmapNodes.get(i).getNode());
//		}
//		return nodeList;
//	}

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
