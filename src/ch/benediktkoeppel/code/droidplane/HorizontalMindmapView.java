package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.content.Context;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.util.Log;

public class HorizontalMindmapView extends HorizontalScrollView {
	
	/**
	 * HorizontalScrollView can only have one view, so we need to add a
	 * LinearLayout underneath it, and then stuff all NodeColumns into this
	 * linearLayout.
	 */
	private LinearLayout linearLayout;
	
	private ArrayList<NodeColumn> nodeColumns;
	
	public HorizontalMindmapView(Context context) {
		super(context);
		
		// list where all columns are stored
		nodeColumns = new ArrayList<NodeColumn>();
		
		// create the layout parameters for a new LinearLayout
    	int height = LayoutParams.MATCH_PARENT;
    	int width = LayoutParams.MATCH_PARENT;
		ViewGroup.LayoutParams linearLayoutParams = new ViewGroup.LayoutParams(width, height);
		
		// create a LinearLayout in this HorizontalScrollView. All NodeColumns will go into that LinearLayout.
		linearLayout = new LinearLayout(context);
    	linearLayout.setLayoutParams(linearLayoutParams);
    	this.addView(linearLayout);
	}
	
	/**
	 * Add a new NodeColumn to the HorizontalMindmapView
	 * @param nodeColumn the NodeColumn to add to the HorizontalMindmapView
	 */
	public void addColumn(NodeColumn nodeColumn) {
		nodeColumns.add(nodeColumn);
		linearLayout.addView(nodeColumn, linearLayout.getChildCount());
		Log.d(MainApplication.TAG, "linearLayout now has " + linearLayout.getChildCount() + " items");
	}
	
	/**
	 * GUI Helper to scroll the HorizontalMindmapView all the way to the right. Should be called after adding a NodeColumn.
	 * @return true if the key event is consumed by this method, false otherwise
	 */
	public void scrollToRight() {
		
		// a runnable that knows "this"
		final class HorizontalMindmapViewRunnable implements Runnable {
			HorizontalMindmapView horizontalMindmapView;
			
			public HorizontalMindmapViewRunnable(HorizontalMindmapView horizontalMindmapView) {
				this.horizontalMindmapView = horizontalMindmapView;
			}

			@Override
			public void run() {
				horizontalMindmapView.fullScroll(FOCUS_RIGHT);
			}
		}
		
		new Handler().postDelayed(new HorizontalMindmapViewRunnable(this), 100L);
	}

	/**
	 * Removes all columns from this HorizontalMindmapView
	 */
	public void removeAllColumns() {
		nodeColumns.clear();
		linearLayout.removeAllViews();
	}

	/**
	 * Adjusts the width of all columns in the HorizontalMindmapView
	 * @param columnWidth the width of each column
	 */
	public void resizeAllColumns(int columnWidth) {
		for (NodeColumn nodeColumn : nodeColumns) {
			nodeColumn.setWidth(columnWidth);
		}
	}
	
	/**
	 * Removes the rightmost column and returns true. If there was no column to
	 * remove, returns false. It never removes the last column, i.e. it never
	 * removes the root node of the mind map.
	 * 
	 * @return True if a column was removed, false if no column was removed.
	 */
	public boolean removeRightmostColumn() {
		
		// only remove a column if we have at least 2 columns. If there is only
		// one column, it will not be removed.
		if ( nodeColumns.size() >= 2 ) {
			
			// the column to remove
			NodeColumn rightmostColumn = nodeColumns.get(nodeColumns.size()-1);
			
			// remove it from the linear layout
			linearLayout.removeView(rightmostColumn);
			
			// remove it from the nodeColumns list
			nodeColumns.remove(nodeColumns.size()-1);
			
			// then deselect all nodes on the now newly rightmost column
			nodeColumns.get(nodeColumns.size()-1).deselectAllNodes();
			
			// a column was removed, so we return true
			return true;
		}
		
		// no column was removed, so we return false
		else {
			return false;
		}
		
	}
		

	/**
	 * Returns the number of columns in the HorizontalMindmapView.
	 * @return
	 */
	public int getNumberOfColumns() {
		return nodeColumns.size();
	}

	/**
	 * Returns the title of the parent node of the rightmost column. This is the
	 * same as the node name of the selected node from the 2nd-rightmost column.
	 * So this is the last node that the user has clicked.
	 * If the rightmost column has no parent, an empty string is returned.
	 * 
	 * @return Title of the right most parent node or an empty string.
	 */
	public String getTitleOfRightmostParent() {
		
		if ( !nodeColumns.isEmpty() ) {
			
			Node parent = nodeColumns.get(nodeColumns.size()-1).getParentNode();
			
			// TODO: this really does not belong here. HorizontalMindmapView
			// should not have to care about Node/Element/MindmapNode stuff.
			// Instead, we should only have MindmapNodes everywhere, and a
			// MindmapNode should have a proper getPlainText() method.
			// we need to check if this node is an ELEMENT_NODE, and if it has tag "node"
			if ( parent.getNodeType()==Node.ELEMENT_NODE && ((Element)parent).getTagName().equals("node") ) {
				return ((Element)parent).getAttribute("TEXT");
			}
			
			// the parent node did not have the "node" tag, or was not an
			// ELEMENT_NODE. In either case, we don't know it's title.
			else {
				return "";
			}
			
		}
		
		// there were no columns
		else {
			return "";
		}
	}
	
	public void removeAllColumnsRightOf(ListView listView) {
		for (int i = nodeColumns.size()-1; i >= 0; i--) {
	
			// TODO: implement this
//			
//			
//			
//			
//			
//			
//			
//
//			for (int i = application.nodeColumns.size()-1; i >= application.nodeColumns.lastIndexOf((ListView)parent)+1; i--) {
//						
//				// remove the list view as far right as possible
//				ListView listViewToRemove = application.nodeColumns.get(i);
//				((ViewGroup)listViewToRemove.getParent()).removeView(listViewToRemove);
//				
//				// remove it from listViews
//				application.nodeColumns.remove(application.nodeColumns.size()-1);
//				
//			}
//
//			
//			
//			
//			
//			
//			
			
		}
		
	}
}

