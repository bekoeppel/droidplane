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
import android.content.DialogInterface;
import android.content.Intent;
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

// TODO: create horizontally snapping stuff http://blog.velir.com/index.php/2010/11/17/android-snapping-horizontal-scroll/
// TODO: stop using DOM Nodes, and switch to MindmapNodes
// TODO: start using a SAX parser and build my own MindMap, dynamically build branches when user drills down, truncate branches when they are not used anymore. How will we do Edit Node / Insert Node, if we are using a SAX parser? Maybe we should not go for a SAX parser but find a more efficient DOM parser?

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
		// TODO: factor this stuff out. we really should have a loadDocument(InputStream) method somewhere
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
		
		// otherwise, we can display the existing HorizontalMindmapView again
		else {
			
	        // add the HorizontalMindmapView to the Layout Wrapper
			LinearLayout tmp_parent = ((LinearLayout)application.horizontalMindmapView.getParent());
			if ( tmp_parent != null ) {
				tmp_parent.removeView(application.horizontalMindmapView);
			}
	        ((LinearLayout)findViewById(R.id.layout_wrapper)).addView(application.horizontalMindmapView);

	        // fix the widths of all columns
			application.horizontalMindmapView.resizeAllColumns();
			
			// and then scroll to the right
			application.horizontalMindmapView.scrollToRight();
		}
    }

	/**
	 * enables the home button if the Android version allows it
	 */
	@SuppressLint("NewApi")
	private void enableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
	}
	
	/**
	 * disables the home button if the Android version allows it
	 */
	@SuppressLint("NewApi")
	private void disableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(false);
    	}
	}
	

    /* (non-Javadoc)
     * Creates the options menu
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
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
       
    /**
     * navigates to the top of the Mindmap
     */
    public void top() {
    	
    	// remove all ListView layouts in linearLayout parent_list_view
    	application.horizontalMindmapView.removeAllColumns();
    	
    	// go down into the root node
    	down(application.document.getDocumentElement());
    }
    
    /**
     * navigates back up one level in the Mindmap, if possible (otherwise does nothing)
     */
    public void up() {
    	up(false);
    }

	/**
	 * navigates back up one level in the Mindmap. If we already display the root node, the application will finish
	 */
	public void upOrClose() {
		up(true);
	}
    
	/**
	 * navigates back up one level in the Mindmap, if possible. If force is true, the application closes if we can't go further up
	 * @param force
	 */
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
     * open up Node node, and display all its child nodes
     * @param node
     */
    public void down(Node node) {
		
		// add a new column for this node and add it to the HorizontalMindmapView
    	NodeColumn nodeColumn = new NodeColumn(getApplicationContext(), node);
		nodeColumn.setOnItemClickListener(this);
		application.horizontalMindmapView.addColumn(nodeColumn);
		
		// then scroll all the way to the right
		application.horizontalMindmapView.scrollToRight();
		
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
	

    
    /* (non-Javadoc)
     * Handler of all menu events
     * Home button: navigate one level up, and exit the application if the home button is pressed at the root node
     * Menu Up: navigate one level up, and stay at the root node
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
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
    
    /* (non-Javadoc)
     * Handler for the back button
     * Navigate one level up, and stay at the root node
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
    	upOrClose();
    }
    
	/* (non-Javadoc)
	 * Handler when one of the ListItem's item is clicked
	 * Find the node which was clicked, and redraw the screen with this node as new parent
	 * if the clicked node has no child, then we stop here
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		// the clicked column
		// TODO: parent is the ListView in which the user clicked. Because
		// NodeColumn does not extend ListView (it only wrapps a ListView), we
		// have to find out which NodeColumn it was. We can do so because
		// NodeColumn.getNodeColumnFromListView uses a static HashMap to do the
		// translation.
		NodeColumn clickedNodeColumn = NodeColumn.getNodeColumnFromListView((ListView)parent);
		
		// remove all columns right of the column which was clicked
		application.horizontalMindmapView.removeAllColumnsRightOf(clickedNodeColumn);
		
		// then get the clicked node
		MindmapNode clickedNode = clickedNodeColumn.getNodeAtPosition(position);
		
		// if the clicked node has child nodes, we set it to selected and drill down
		if ( clickedNode.getNumChildMindmapNodes() > 0 ) {
			
			// give it a special color
			clickedNodeColumn.setItemColor(position);
			
			// and drill down
			down(clickedNode.getNode());
		}

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
	
	/**
	 * Shows a popup with an error message and then closes the application
	 * @param stringResourceId
	 */
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