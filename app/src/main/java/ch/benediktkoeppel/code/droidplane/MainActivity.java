package ch.benediktkoeppel.code.droidplane;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.acra.ACRA;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

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
	
	public final static String INTENT_START_HELP = "ch.benediktkoeppel.code.droidplane.INTENT_START_HELP";

    private ProgressDialog progressDialog;

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
        
		// if the application was reset, or the document has changed, we need to re-initialize everything
		if ( application.mindmap == null || application.mindmap.document == null
				|| (application.mindmap.getUri()!=intent.getData() && intent.getData()!=null)
				|| (intent.getBooleanExtra(INTENT_START_HELP,false))
		) {

            // create a new Mindmap
            application.mindmap = new Mindmap();

            // create a new HorizontalMindmapView
            application.horizontalMindmapView = new HorizontalMindmapView(getApplicationContext());

            // create a ProgressDialog, and load the file asynchronously
            this.progressDialog = ProgressDialog.show(this, "DroidPlane", "Opening Mindmap File...", true, false);
            new FileOpenTask(intent, action, type).execute();

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

    private class FileOpenTask extends AsyncTask<String, Void, Object> {

        private final Intent intent;
        private final String action;
        private final String type;

        FileOpenTask(Intent intent, String action, String type) {
            this.intent = intent;
            this.action = action;
            this.type = type;
        }

        @Override
        protected Object doInBackground(String... strings) {

            // prepare loading of the Mindmap file
            InputStream mm = null;

            // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the launcher
            // started from ACTION_EDIT/VIEW intent
            if ((Intent.ACTION_EDIT.equals(action)||Intent.ACTION_VIEW.equals(action))) {

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
                application.mindmap.setUri(uri);
            }

            // started from the launcher
            else {
                Log.d(MainApplication.TAG, "started from app launcher intent");

                // display the default Mindmap "example.mm", from the resources
                mm = getApplicationContext().getResources().openRawResource(R.raw.example);
            }

            // load the mindmap
            Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");
            application.mindmap.loadDocument(mm);

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

            // add the HorizontalMindmapView to the Layout Wrapper
            ((LinearLayout)findViewById(R.id.layout_wrapper)).addView(application.horizontalMindmapView);

            // navigate down into the root node
            application.horizontalMindmapView.down(application.mindmap.getRootNode());

            if (progressDialog != null) {
                progressDialog.dismiss();
            }
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
			
		// "Help" menu action
		case R.id.help:
			
			// create a new intent (without URI)
			Intent helpIntent = new Intent(this, MainActivity.class);
			helpIntent.putExtra(INTENT_START_HELP, true);
			startActivity(helpIntent);

		// App button (top left corner)
		case android.R.id.home:
			application.horizontalMindmapView.up();
			break;
		}

		return true;
	}
    
	/*
	 * (non-Javadoc)
	 * 
	 * It looks like the onContextItemSelected has to be overwritten in a class
	 * extending Activity. It was not possible to have this callback in the
	 * NodeColumn. As a result, we have to find out here again where the event happened
	 * 
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		
		// contextMenuInfo.position is the position in the ListView where the
		// context menu was loaded, i.e. the index of the item in our
		// mindmapNodes list
		
		// MindmapNode extends LinearView, so we can cast targetView back to MindmapNode
		MindmapNode mindmapNode = (MindmapNode)contextMenuInfo.targetView;
		Log.d(MainApplication.TAG, "mindmapNode.text = " + mindmapNode.text);
		
		Log.d(MainApplication.TAG, "contextMenuInfo.position = " + contextMenuInfo.position);
		Log.d(MainApplication.TAG, "item.getTitle() = " + item.getTitle());
		
		switch (item.getItemId()) {

		// copy node text to clipboard
		case R.id.contextcopy:
			Log.d(MainApplication.TAG, "Copying text to clipboard");
			ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			
	    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				ClipData clipData = ClipData.newPlainText("node", mindmapNode.text);
				clipboardManager.setPrimaryClip(clipData);
	    	} else {
	    		clipboardManager.setText(mindmapNode.text);
	    	}
			
			break;
			
		// open the URI specified in the "LINK" tag
		case R.id.contextopenlink:
			Log.d(MainApplication.TAG, "Opening node link " + mindmapNode.link);
            mindmapNode.openLink();

            break;

		default:
			break;
		}

		return true;
	}
	
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
