package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

public class HorizontalMindmapView extends HorizontalScrollView implements OnTouchListener {
	
	/**
	 * HorizontalScrollView can only have one view, so we need to add a
	 * LinearLayout underneath it, and then stuff all NodeColumns into this
	 * linearLayout.
	 */
	private LinearLayout linearLayout;
	
	/**
	 * nodeColumns holds the list of columns that are displayed in this
	 * HorizontalScrollView.
	 */
	private ArrayList<NodeColumn> nodeColumns;
	
	/**
	 * constants to determine the minimum swipe distance and speed
	 */
	private static final int SWIPE_MIN_DISTANCE = 5;
	private static final int SWIPE_THRESHOLD_VELOCITY = 300;
	
	/**
	 * Gesture detector
	 */
	//private GestureDetector gestureDetector;
	
	
	/**
	 * The index of the rightmost column that is visible
	 */
	// TODO: should it maybe be the leftmost column?
	private int rightmostVisibleColumn;


	
	
	/**
	 * Setting up a HorizontalMindmapView. We initialize the nodeColumns, define
	 * the layout parameters for the HorizontalScrollView and create the
	 * LinearLayout view inside the HorizontalScrollView.
	 * @param context the Application Context
	 */
	public HorizontalMindmapView(Context context) {
		super(context);
		
		// list where all columns are stored
		nodeColumns = new ArrayList<NodeColumn>();
		
		// set the layout for the HorizontalScrollView itself
		setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		// create the layout parameters for a new LinearLayout
    	int height = LayoutParams.MATCH_PARENT;
    	int width = LayoutParams.MATCH_PARENT;
		ViewGroup.LayoutParams linearLayoutParams = new ViewGroup.LayoutParams(width, height);
		
		// create a LinearLayout in this HorizontalScrollView. All NodeColumns will go into that LinearLayout.
		linearLayout = new LinearLayout(context);
    	linearLayout.setLayoutParams(linearLayoutParams);
    	this.addView(linearLayout);
    	    	
    	// register HorizontalMindmapView to receive all touch events on itself
    	setOnTouchListener(this);
    	

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
	 * GUI Helper to scroll the HorizontalMindmapView all the way to the right.
	 * Should be called after adding a NodeColumn.
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
	public void resizeAllColumns() {
		for (NodeColumn nodeColumn : nodeColumns) {
			nodeColumn.resizeColumnWidth();
		}
	}
	
	/**
	 * Removes the rightmost column and returns true. If there was no column to
	 * remove, returns false. It never removes the last column, i.e. it never
	 * removes the root node of the mind map.
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
	
	/**
	 * Remove all columns at the right of the specified column. 
	 * @param nodeColumn
	 */
	public void removeAllColumnsRightOf(NodeColumn nodeColumn) {
		
		// we go from right to left, from the end of nodeColumns back to one
		// element after nodeColumn
		//		
		// nodeColumns = [ col1, col2, col3, col4, col5 ];
		// removeAllColumnsRightOf(col2) will do:
		//     nodeColumns.size()-1 => 4
		//     nodeColumns.lastIndexOf(col2)+1 => 2
		//
		// for i in (4, 3, 2): remove rightmost column
		//     i = 4: remove col5
		//     i = 3: remove col4
		//     i = 2: remove col3
		//
		// so at the end, we have
		// nodeColumns = [ col1, col2 ];
		for (int i = nodeColumns.size()-1; i >= nodeColumns.lastIndexOf(nodeColumn)+1; i--) {
			
			// remove this column
			removeRightmostColumn();
		}
	}

	/*
	 * (non-Javadoc)
	 * Will be called whenever the HorizontalScrollView is
	 * touched. We have to capture the move left and right events here, and snap
	 * to the appropriate column borders.
	 * 
	 * @see android.view.View.OnTouchListener#onTouch(android.view.View,
	 * android.view.MotionEvent)
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		
		// first, we let the gestureDetector examine the event. It will process
		// the event if it was a gesture, i.e. if it was fast enough to trigger
		// a Fling. If it handled the event, we don't process it further.
		// This gesture can be triggered if the user moves the finger fast
		// enough. He does not necessarily have to move so far that the next
		// column is mostly visible.
		/*if ( gestureDetector.onTouchEvent(event) ) {
			return true;
		}
		
		// If it was not a gesture (i.e. the user moved his finger too slow), we
		// simply snap to the next closest column border.
		else*/ if ( event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ) {
			
			// now we need to find out where the HorizontalMindmapView is horizontally scrolled
			int scrollX = getScrollX();
			Log.d(MainApplication.TAG, "HorizontalMindmapView is scrolled horizontally to " + scrollX);
			
			int viewWidth = getWidth();
						
			// and then get the leftmost visible column (just a little bit is
			// enough)
			// how many columns fit into less than scrollX space?
			// sum the width of all columns until we go over scrollX. then the
			// last column that we added to the sum is partially visible
			int sumColumnWidths = 0;
			NodeColumn leftmostVisibleColumn = null;
			int numVisiblePixelsOnColumn = 0;
			for (int i = 0; i < nodeColumns.size(); i++) {
				sumColumnWidths += nodeColumns.get(i).getWidth();
				
				// if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit) visible
				if (sumColumnWidths >= scrollX) {
					leftmostVisibleColumn = nodeColumns.get(i);
					
					// how many pixels are visible of this column?
					numVisiblePixelsOnColumn = sumColumnWidths - scrollX;
					break;
				}
			}
			
			// if we couldn't find a column, we could not process this event. I'm not sure how this might ever happen
			if ( leftmostVisibleColumn == null ) {
				Log.e(MainApplication.TAG, "No leftmost visible column was detected. Not sure how this could happen!");
				return false;
			}
			
			// and then determine if the leftmost visible column shows more than 50% of its full width
			// if it shows more than 50%, then we scroll to the left, so that we can see it fully
			if ( numVisiblePixelsOnColumn < leftmostVisibleColumn.getWidth()/2 ) {
				
				Log.d(MainApplication.TAG, "Scrolling to the left, so that we can see the column fully");
				smoothScrollTo(sumColumnWidths, 0);

			}
			
			// if it shows less than 50%, then we scroll to the right, so that is not visible anymore 
			else {

				Log.d(MainApplication.TAG, "Scrolling to the right, so that the column is not visible anymore");
				smoothScrollTo(sumColumnWidths-leftmostVisibleColumn.getWidth(), 0);
				
			}
			
			// we have processed this event
			return true;
			
		}
		
		// if we did not process the event ourself we let the caller know
		else {
			return false;
		}
//		
//		// TODO Auto-generated method stub
//		return false;
//		
//    	
//    	setOnTouchListener(new View.OnTouchListener() {
//    		
//    		@Override
//    		public boolean onTouch(View view, MotionEvent event) {
//    			if (mGestureDetector.onTouchEvent(event)) {
//    				return true;
//    			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ) {
//    				int scrollX = getScrollX();
//    				int featureWidth = view.getMeasuredWidth();
//    				mActiveFeature = ((scrollX + (featureWidth/2))/featureWidth);
//    				int scrollTo = mActiveFeature*featureWidth;
//    				smoothScrollTo(scrollTo, 0);
//    				return true;
//    			} else {
//    				return false;
//    			}
//    		}
//    	});
	}
}

