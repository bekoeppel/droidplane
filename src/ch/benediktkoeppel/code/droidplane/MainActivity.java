package ch.benediktkoeppel.code.droidplane;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
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
import android.widget.LinearLayout;

import com.google.analytics.tracking.android.EasyTracker;

// TODO: think about a strategy to strip out Log.v and Log.d messages for releases

// TODO: stop using DOM Nodes, and switch to MindmapNodes
// TODO: start using a SAX parser and build my own MindMap, dynamically build branches when user drills down, truncate branches when they are not used anymore. How will we do Edit Node / Insert Node, if we are using a SAX parser? Maybe we should not go for a SAX parser but find a more efficient DOM parser?

// TODO: allow us to open multiple files and display their root nodes and file names in the leftmost column. 
// TODO: long-click on a root node shows a "close file" or "close this mindmap" menu
// TODO: add a progress bar when opening a file (or a spinner or so)
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
public class MainActivity extends Activity {
	
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
        MainApplication.setMainActivityInstance(this);
        
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
		if ( application.document == null || (application.getUri()!=intent.getData() && intent.getData()!=null) ) {
			
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
		    	mm = getApplicationContext().getResources().openRawResource(R.raw.example);
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
			application.horizontalMindmapView.down(application.document.getDocumentElement());
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
			
	    	// enable the up navigation with the Home (app) button (top left corner)
			application.horizontalMindmapView.enableHomeButtonIfEnoughColumns();

			// get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
			application.horizontalMindmapView.setApplicationTitle();
		}
    }
	
	
	

	/**
	 * enables the home button if the Android version allows it
	 */
	@SuppressLint("NewApi") void enableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
	}
	
	/**
	 * disables the home button if the Android version allows it
	 */
	@SuppressLint("NewApi") void disableHomeButton() {
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

    
    /* (non-Javadoc)
     * Handler for the back button
     * Navigate one level up, and stay at the root node
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
    	application.horizontalMindmapView.upOrClose();
    }

	/*
	 * (non-Javadoc) Handler of all menu events Home button: navigate one level
	 * up, and exit the application if the home button is pressed at the root
	 * node Menu Up: navigate one level up, and stay at the root node
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {

		switch (item.getItemId()) {

		// "Up" menu action
		case R.id.up:
			application.horizontalMindmapView.up();
			break;

		// "Top" menu action
		case R.id.top:
			application.horizontalMindmapView.top();
			break;

		// App button (top left corner)
		case android.R.id.home:
			application.horizontalMindmapView.up();
			break;
		}

		return true;
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