package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
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
	 * Gesture detector
	 */
	private GestureDetector gestureDetector;
	
	/**
	 * constants to determine the minimum swipe distance and speed
	 */
	private static final int SWIPE_MIN_DISTANCE = 5;
	private static final int SWIPE_THRESHOLD_VELOCITY = 300;
	

	
	
	
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
    	
    	// add a new gesture controller
    	gestureDetector = new GestureDetector(getContext(), new HorizontalMindmapViewGestureDetector());
    	    	
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
		if ( gestureDetector.onTouchEvent(event) ) {
			Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (gesture)");
			return true;
		}
		
		// If it was not a gesture (i.e. the user moved his finger too slow), we
		// simply snap to the next closest column border.
		else if ( event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ) {
			
			// now we need to find out where the HorizontalMindmapView is horizontally scrolled
			int scrollX = getScrollX();
			Log.d(MainApplication.TAG, "HorizontalMindmapView is scrolled horizontally to " + scrollX);
		
			// get the leftmost column that is still (partially) visible
			NodeColumn leftmostVisibleColumn = getLeftmostVisibleColumn();
			
			// get the number of visible pixels of this column
			int numVisiblePixelsOnColumn = getVisiblePixelOfLeftmostColumn();
			
			// if we couldn't find a column, we could not process this event. I'm not sure how this might ever happen
			if ( leftmostVisibleColumn == null ) {
				Log.e(MainApplication.TAG, "No leftmost visible column was detected. Not sure how this could happen!");
				return false;
			}
			
			// and then determine if the leftmost visible column shows more than 50% of its full width
			// if it shows more than 50%, then we scroll to the left, so that we can see it fully
			if ( numVisiblePixelsOnColumn < leftmostVisibleColumn.getWidth()/2 ) {
				Log.d(MainApplication.TAG, "Scrolling to the left, so that we can see the column fully");
				smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0);
			}
			
			// if it shows less than 50%, then we scroll to the right, so that is not visible anymore 
			else {
				Log.d(MainApplication.TAG, "Scrolling to the right, so that the column is not visible anymore");
				smoothScrollTo(scrollX + numVisiblePixelsOnColumn - leftmostVisibleColumn.getWidth(), 0);
			}
			
			// we have processed this event
			Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (no gesture)");
			return true;
			
		}
		
		// if we did not process the event ourself we let the caller know
		else {
			Log.d(MainApplication.TAG, "Touch event was not processed by HorizontalMindmapView");
			return false;
		}
	}
	
	/**
	 * Get the column at the left edge of the screen.
	 * @return NodeColumn
	 */
	private NodeColumn getLeftmostVisibleColumn() {

		// how much we are horizontally scrolled
		int scrollX = getScrollX();
		
		// how many columns fit into less than scrollX space? as soon as
		// sumColumnWdiths > scrollX, we have just added the first visible
		// column at the left.
		int sumColumnWidths = 0;
		NodeColumn leftmostVisibleColumn = null;
		for (int i = 0; i < nodeColumns.size(); i++) {
			sumColumnWidths += nodeColumns.get(i).getWidth();
			
			// if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit) visible
			if (sumColumnWidths >= scrollX) {
				leftmostVisibleColumn = nodeColumns.get(i);
				break;
			}
		}
		
		return leftmostVisibleColumn;
	}
	
	/**
	 * Get the number of pixels that are visible on the leftmost column.
	 * @return
	 */
	// TODO: this is ugly, DRY from getLeftmostVisibleColumn() !
	private int getVisiblePixelOfLeftmostColumn() {
		
		// how much we are horizontally scrolled
		int scrollX = getScrollX();
		
		// how many columns fit into less than scrollX space? as soon as
		// sumColumnWdiths > scrollX, we have just added the first visible
		// column at the left.
		int sumColumnWidths = 0;
		int numVisiblePixelsOnColumn = 0;
		for (int i = 0; i < nodeColumns.size(); i++) {
			sumColumnWidths += nodeColumns.get(i).getWidth();
			
			// if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit) visible
			if (sumColumnWidths >= scrollX) {
				// how many pixels are visible of this column?
				numVisiblePixelsOnColumn = sumColumnWidths - scrollX;
				break;
			}
		}
		
		return numVisiblePixelsOnColumn;
	}
	
//	// Darn snap, you ain't gonna need it.
//	/**
//	 * Get the number of visible pixels of a column
//	 * @param nodeColumn
//	 * @return visible pixel count
//	 */
//	// TODO: clean up
//	private int getVisiblePixelOfColumn(NodeColumn nodeColumn) {
//		
//		// screen width
//		int screenWidth = getWidth();
//		
//		// scroll position
//		int scrollX = getScrollX();
//		
//		// find the index of the nodeColumn
//		int indexOfNodeColumn = nodeColumns.indexOf(nodeColumn);
//		
//		// get the width of the column
//		int columnWidth = nodeColumn.getWidth();
//		
//		// find out how many pixels are left of this column
//		int pixelLeftOfColumn = 0;
//		for (int i = 0; i < indexOfNodeColumn; i++) {
//			pixelLeftOfColumn += nodeColumns.get(i).getWidth();
//		}
//		
//		// TODO: clean up, I'm sure this could be stated in a much simpler way.
//		// if the pixels left of the column are bigger than scrollX+width, the column is far off the right screen
//		if ( pixelLeftOfColumn < screenWidth + scrollX ) {
//			return 0;
//		}
//		
//		// if pixelLeftOfColumn+columnWidth is less than scrollX, the column is far off the left screen
//		else if ( pixelLeftOfColumn+columnWidth < scrollX) {
//			return 0;
//		}
//		
//		// here's the tricky part. If the 
//		else {
//			
//		}
//				
//	}
	
	

	class HorizontalMindmapViewGestureDetector extends SimpleOnGestureListener {
		
	    private MotionEvent lastOnDownEvent;

		@Override
	    public boolean onDown(MotionEvent e) {
			if (e == null) {
				Log.e(MainApplication.TAG, "MotionEvent e is null");
			} else {
				lastOnDownEvent = e;
			}
	        return true;
	    }

		@Override
		// TODO cleanup
		public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
			
			// TODO: do we really need this? event1 == null is no problem
			try {
				
				// how much we are horizontally scrolled
				int scrollX = getScrollX();

				
				if (event1 == null) {
					if (lastOnDownEvent == null) {
						Log.d(MainApplication.TAG, "Event1 and lastOnDownEvent are null");
						return false;
					}
					Log.d(MainApplication.TAG, "Event1 is null, set to lastOnDownEvent");
					event1 = lastOnDownEvent;
				}
				if (event2 == null) {
					Log.e(MainApplication.TAG, "Event2 is null");
				}
				
				float distance = event1.getX() - event2.getX();
				Log.d(MainApplication.TAG, "Moved distance = " + distance);
				Log.d(MainApplication.TAG, "Velocity = " + velocityX);
				
				// get the leftmost column that is still (partially) visible
				NodeColumn leftmostVisibleColumn = getLeftmostVisibleColumn();
				
				// get the number of visible pixels of this column
				int numVisiblePixelsOnColumn = getVisiblePixelOfLeftmostColumn();

				
				// if we have moved at least the SWIPE_MIN_DISTANCE to the right and at faster than SWIPE_THRESHOLD_VELOCITY
				if (distance > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					
					// TODO: process the event
					smoothScrollTo(scrollX + numVisiblePixelsOnColumn - leftmostVisibleColumn.getWidth(), 0);
					
					
					Log.d(MainApplication.TAG, "processing the Fling to Right gesture");
					return true;
				}
				
				// the same as above but from to the left
				else if ( -distance > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
	
					// TODO: process the event
					smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0);
					
					Log.d(MainApplication.TAG, "processing the Fling to Left gesture");
					return true;
				}
				
				// we did not process this gesture
				else {
					
					Log.d(MainApplication.TAG, "Fling was no real fling");
					return false;
				}
				
			} catch (Exception e) {
				Log.d(MainApplication.TAG, "A whole lot of stuff could have gone wrong here");
				e.printStackTrace();
				return false;
			}
		}
		
		
		
	}
	


	

	
}

		
//				// right to left
//				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
//						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//					int featureWidth = getMeasuredWidth();						
//					mActiveFeature = (mActiveFeature < (mItems.size() - 1)) ? mActiveFeature + 1
//							: mItems.size() - 1;
//					smoothScrollTo(mActiveFeature * featureWidth, 0);
//					return true;
//				}
//				// left to right
//				else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
//						&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
//					int featureWidth = getMeasuredWidth();
//					mActiveFeature = (mActiveFeature > 0) ? mActiveFeature - 1
//							: 0;
//					smoothScrollTo(mActiveFeature * featureWidth, 0);
//					return true;
//				}
