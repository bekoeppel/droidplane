package ch.benediktkoeppel.code.droidplane;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.analytics.tracking.android.EasyTracker;


// TODO: can we get built-in icons as SVG?
// TODO: properly parse rich text nodes
// TODO: implement OnItemLongClickListener with a context menu (show all icons, follow link, copy text, and ultimately also edit)

/**
 * The MainActivity can be started from the App Launcher, or with a File Open
 * intent. If the MainApplication was already running, the previously used
 * document is re-used. Also, most of the information about the mind map and the
 * currently opened views is stored in the MainApplication. This enables the
 * MainActivity to resume wherever it was before it got restarted. A restart
 * can happen when the screen is rotated, and we want to continue wherever we
 * were before the screen rotate.
 */
public class MainActivity extends Activity implements OnItemClickListener {
	
	MainApplication application;
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
		EasyTracker.getInstance().dispatch();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        application = (MainApplication)getApplication();
        
        // initialize android stuff
        // EasyTracker
        EasyTracker.getInstance().setContext(this);
    	EasyTracker.getTracker().sendView("MainActivity");

    	// enable the Android home button
        enableHomeButton();
    	
    	// intents (how we are called)
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        
        // start measuring the document load time
		long loadDocumentStartTime = System.currentTimeMillis();
		
		// if the application was reset, or the document has changed, we need to re-initialize everything
		if ( application.document == null || application.getUri() != intent.getData() ) {
			
			// Mindmap stuff
			InputStream mm = null;
			// XML document builder. The document itself is in the MainApplication
			DocumentBuilderFactory docBuilderFactory;
			DocumentBuilder docBuilder;
			
			// create a new HorizontalMindmapView
			application.horizontalMindmapView = new HorizontalMindmapView(getApplicationContext());
	        
	        // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the launcher
	        // started from ACTION_EDIT/VIEW intent
	        if ((Intent.ACTION_EDIT.equals(action)||Intent.ACTION_VIEW.equals(action)) && type != null) {
	        	
	        	Log.d(MainApplication.TAG, "started from ACTION_EDIT/VIEW intent");
	        	
	        	// get the URI to the target document (the Mindmap we are opening) and open the InputStream
	        	Uri uri = intent.getData();
	        	if ( uri != null ) {
	        		ContentResolver cr = getContentResolver();
	        		try {
						mm = cr.openInputStream(uri);
					} catch (FileNotFoundException e) {
	
				    	abortWithPopup(R.string.filenotfound);
				    	
						ACRA.getErrorReporter().putCustomData("Exception", "FileNotFoundException");
						ACRA.getErrorReporter().putCustomData("Intent", "ACTION_EDIT/VIEW");
						ACRA.getErrorReporter().putCustomData("URI", uri.toString());
						e.printStackTrace();
					}
	        	} else {
	        		abortWithPopup(R.string.novalidfile);
	        	}
	        	
				// store the Uri. Next time the MainActivity is started, we'll
				// check whether the Uri has changed (-> load new document) or
				// remained the same (-> reuse previous document)
	        	application.setUri(uri);
	        } 
	        
	        // started from the launcher
	        else {
	        	
	        	Log.d(MainApplication.TAG, "started from app launcher intent");
	        	
	        	// display the default Mindmap "example.mm", from the resources
		    	mm = this.getResources().openRawResource(R.raw.example);
	        }
	        
	        Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");
	        
	        // load the Mindmap from the InputStream
	        docBuilderFactory = DocumentBuilderFactory.newInstance();
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
				application.document = docBuilder.parse(mm);
			} catch (ParserConfigurationException e) {
				ACRA.getErrorReporter().putCustomData("Exception", "ParserConfigurationException");
				e.printStackTrace();
				return;
			} catch (SAXException e) {
				ACRA.getErrorReporter().putCustomData("Exception", "SAXException");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				ACRA.getErrorReporter().putCustomData("Exception", "IOException");
				e.printStackTrace();
				return;
			}
			
			long loadDocumentEndTime = System.currentTimeMillis();
		    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
			Log.d(MainApplication.TAG, "Document loaded");
		    
			long numNodes = application.document.getElementsByTagName("node").getLength();
			EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);
			

	        // add the HorizontalMindmapView to the Layout Wrapper
			((LinearLayout)findViewById(R.id.layout_wrapper)).addView(application.horizontalMindmapView);
			
			
			// navigate down into the root node
			down(application.document.getDocumentElement());
		}
		
		else {
			
			
	        // add the HorizontalMindmapView to the Layout Wrapper
			LinearLayout tmp_parent = ((LinearLayout)application.horizontalMindmapView.getParent());
			if ( tmp_parent != null ) {
				tmp_parent.removeView(application.horizontalMindmapView);
			}
	        ((LinearLayout)findViewById(R.id.layout_wrapper)).addView(application.horizontalMindmapView);

	        // fix the widths of all columns
			application.horizontalMindmapView.resizeAllColumns(calculateColumnWidth());
			
			
			
		}
//		
//		// otherwise, we can display the existing ListViews again
//		else {
//			
//			Log.d(MainApplication.TAG, "Restarted Activity, but Application remained intact");
//			Log.d(MainApplication.TAG, "Re-Adding " + application.nodeColumns.size() + " existing ListViews");
//			
//			// add all listViews back
//			int columnWidth = getColumnWidth();
//			Log.d(MainApplication.TAG, "columnWidth = " + columnWidth);
//			for (int i = 0; i < application.nodeColumns.size(); i++) {
//				NodeColumn nodeColumn = application.nodeColumns.get(i);
//				
//				// remove the column from it's GUI parent (the GUI layout has been recreated, so its parent is not valid anymore anyway)
//				nodeColumn.removeFromParent();
//				
//				// then resize it
//				nodeColumn.setWidth(columnWidth);
//				
//				// TODO CLEANUP: who should be the on click listener for the column? the column itself maybe?
//				nodeColumn.setOnItemClickListener(this);
//				
//				// then re-add it to the linearLayout we have now
//				application.horizontalMindmapView.addColumn(nodeColumn);
//			}
//			
//			// then scroll to right
//			application.horizontalMindmapView.scrollToRight();
//			
//		}

        
    }

    // enables the home button
	@SuppressLint("NewApi")
	private void enableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
	}
	
    // disables the home button
	@SuppressLint("NewApi")
	private void disableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(false);
    	}
	}
	

    // sets the menu
    @Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {

		// TODO: add "Find" button and menu -> should search underneath the
		// current node (or with an option, under the root node)
    	
    	// TODO: menu "Open"
    	
    	// TODO: settings (to set the number of horizontal and vertical columns)

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;

	}
       
    
    // navigates to the top of the Mindmap
    public void top() {
    	
    	// remove all ListView layouts in linearLayout parent_list_view
    	application.horizontalMindmapView.removeAllColumns();
    	
    	// go down into the root node
    	down(application.document.getDocumentElement());
    }
    
    // navigates back up one level in the Mindmap, if possible (otherwise does nothing)
    public void up() {
    	up(false);
    }

    // navigates back up one level in the Mindmap. If we already display the root node, the application will finish
	public void upOrClose() {
		up(true);
	}
    
    // navigates back up one level in the Mindmap, if possible. If force is true, the application closes if we can't go further up
	public void up(boolean force) {
		
		boolean wasColumnRemoved = application.horizontalMindmapView.removeRightmostColumn();
		
		// close the application if no column was removed, and the force switch was on
		if (!wasColumnRemoved && force ) {
			finish();
		}
		
    	// enable the up navigation with the Home (app) button (top left corner)
		enableHomeButtonIfEnoughColumns();

		// get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
		setApplicationTitle();

	}

	/**
	 * Sets the application title to the name of the parent node of the rightmost column, which is the most recently clicked node.
	 */
	private void setApplicationTitle() {
		// get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
		// set the application title to this nodeTitle. If the nodeTitle is empty, we set the default Application title
		String nodeTitle = application.horizontalMindmapView.getTitleOfRightmostParent();
		if ( nodeTitle.equals("") ) {
			setTitle(R.string.app_name);
		} else {
			setTitle(nodeTitle);
		}
	}

	/**
	 * Enables the Home button in the application if we have enough columns, i.e. if "Up" will remove a column.
	 */
	private void enableHomeButtonIfEnoughColumns() {
		// if we only have one column (i.e. this is the root node), then we disable the home button
		int numberOfColumns = application.horizontalMindmapView.getNumberOfColumns();
		if ( numberOfColumns >= 2 ) {
    		enableHomeButton();
    	} else {
    		disableHomeButton();
    	}
	}
	
	// open up Node node, and display all its child nodes
    public void down(Node node) {
		
		// add a new column for this node and add it to the HorizontalMindmapView
    	NodeColumn nodeColumn = new NodeColumn(getApplicationContext(), node);
		nodeColumn.setOnItemClickListener(this);
		application.horizontalMindmapView.addColumn(nodeColumn);
		
    	// enable the up navigation with the Home (app) button (top left corner)
		enableHomeButtonIfEnoughColumns();

		// get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
		setApplicationTitle();

    }
    
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
    public int calculateColumnWidth() {
    	// and R.integer.horizontally_visible_panes defines how many columns should be visible side by side
    	// so we need 1/(horizontall_visible_panes) * screen width as column width
    	int horizontallyVisiblePanes = getResources().getInteger(R.integer.horizontally_visible_panes);
        android.view.Display display = ((android.view.WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
    	
    	return columnWidth;
    }
    
    
    // Handler of all menu events
    // Home button: navigate one level up, and exit the application if the home button is pressed at the root node
    // Menu Up: navigate one level up, and stay at the root node
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
    	// "Up" menu action
    	case R.id.up:
    		up();
    		break;
    		
		// "Top" menu action
    	case R.id.top:
    		top();
    		break;
    		
		// App button (top left corner)
    	case android.R.id.home:
    		up();
        	break;
    	}
    	
    	return true;
    }
    
    // Handler for the back button
    // Navigate one level up, and stay at the root node
    @Override
    public void onBackPressed() {
    	upOrClose();
    }
    
    // Handler when one of the ListItem's item is clicked
    // Find the node which was clicked, and redraw the screen with this node as new parent
    // if the clicked node has no child, then we stop here
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		// find out which view it was, then null all adapters to the right
		// also, because clicking on a ListView which is not the right-most ListView is like going "up", hence we have to pop elements from parents.
		application.horizontalMindmapView.removeAllColumnsRightOf((ListView)view);
		
				

		// TODO implement this
//		for (int i = application.nodeColumns.size()-1; i >= application.nodeColumns.lastIndexOf((ListView)parent)+1; i--) {
//					
//			// remove the list view as far right as possible
//			ListView listViewToRemove = application.nodeColumns.get(i);
//			((ViewGroup)listViewToRemove.getParent()).removeView(listViewToRemove);
//			
//			// remove it from listViews
//			application.nodeColumns.remove(application.nodeColumns.size()-1);
//			
//		}
//
//		// then get all nodes from this adapter
//		MindmapNodeAdapter adapter = (MindmapNodeAdapter)((ListView)parent).getAdapter();
//		ArrayList<Node> currentListedNodes = adapter.getNodeList();
//	
//		// extract the pushed node
//		Node pushedNode = currentListedNodes.get(position);
//		
//		// if the node has child nodes (i.e. should be clickable)
//		if ( getNumChildMindmapNodes(pushedNode) > 0 ) {
//
//			// give the pushed node a special color
//			adapter.setItemColor(position);
//			
//			// and drill down 
//			down(pushedNode);
//		}
//		
	}
	
	

	// Handler when an item is long clicked
	// TODO do this!
//	@Override
//	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//		
//		Node pushedNode = currentListedNodes.get(position);
//		
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage("Not yet implemented");
//        builder.setCancelable(true);
//        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//        	public void onClick(DialogInterface dialog, int which) {
//        		return;
//        	}
//        });
//
//        AlertDialog alert = builder.create();
//        alert.show();
//		
//		return true;
//		
//	}

	
	
	// Shows a popup with an error message and then closes the application
	public void abortWithPopup(int stringResourceId) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(stringResourceId);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		finish();
        	}
        });

        AlertDialog alert = builder.create();
        alert.show();
	}



}