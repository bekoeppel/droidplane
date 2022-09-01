package ch.benediktkoeppel.code.droidplane;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The MainActivity can be started from the App Launcher, or with a File Open intent. If the MainApplication was
 * already running, the previously used document is re-used. Also, most of the information about the mind map and the
 * currently opened views is stored in the MainApplication. This enables the MainActivity to resume wherever it was
 * before it got restarted. A restart can happen when the screen is rotated, and we want to continue wherever we were
 * before the screen rotate.
 */
public class MainActivity extends FragmentActivity {

    public final static String INTENT_START_HELP = "ch.benediktkoeppel.code.droidplane.INTENT_START_HELP";

    private Mindmap mindmap;

    /**
     * HorizontalMindmapView that contains all NodeColumns
     */
    private HorizontalMindmapView horizontalMindmapView;

    @Override
    public void onStart() {

        super.onStart();
        ((MainApplication)getApplication()).getGoogleAnalytics().reportActivityStart(this);
    }

    @Override
    public void onStop() {

        super.onStop();
        ((MainApplication)getApplication()).getGoogleAnalytics().reportActivityStop(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize android stuff
        // Google Analytics
        Tracker tracker = ((MainApplication)getApplication()).getTracker();
        tracker.setScreenName("MainActivity");
        tracker.send(new HitBuilders.EventBuilder().build());

        // enable the Android home button
        enableHomeButton();

        // get the Mindmap ViewModel
        mindmap = ViewModelProviders.of(this).get(Mindmap.class);

        // intents (how we are called)
        Intent intent = getIntent();
        String action = intent.getAction();

        // if we're opening a file: check what file we're opening, and when it was last modified
        // sadly it seems files opened from the Files app (or Root Explorer) don't have the last modified field set
        // so in that case, we have to assume that the file is new
        Uri intentUri = intent.getData();
        Date lastModifiedDate = null;
        if (intentUri != null) {
            ContentResolver cr = getContentResolver();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // query document
                String[] p = new String[]{"last_modified"};
                Cursor c = cr.query(intentUri, p, null, null);

                // check if we got last_modified
                int lastModifiedColumnIndex = c.getColumnIndex("last_modified");
                String lastModifiedString = null;
                if (lastModifiedColumnIndex >= 0) {
                    c.moveToFirst();
                    lastModifiedString = c.getString(lastModifiedColumnIndex);

                }

                // parse if we got last_modified
                // from Dropbox, the path looks like this: Thu, 01 Sep 2022 10:25:22 +02:00
                if (lastModifiedString != null) {
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
                        lastModifiedDate = dateFormat.parse(lastModifiedString);
                    } catch (ParseException e) {
                        Log.v(MainApplication.TAG, "Could not parse last modified date: " + lastModifiedString);
                    }

                }

            } else {
                Log.d(MainApplication.TAG, "Can't query ContentResolver due to too low SDK_INT: " + android.os.Build.VERSION.SDK_INT + " < " + android.os.Build.VERSION_CODES.O);
            }
        }

        // we didn't load a mindmap yet, we open it
        // otherwise, we already have a mindmap in the ViewModel, so we can just show the mindmap view again if it is the same URI and hasn't been modified since
        // TODO: check if URI of the intent is the same as URI we opened last time (and probably also modification times...)
        boolean noPreviousMindmapLoaded = mindmap.getRootNode() == null;
        boolean previousMindmapUriDiffers = mindmap.getUri() == null || !mindmap.getUri().equals(intentUri);
        boolean previousMindmapModifiedEarlier = mindmap.getLastModified() == null || lastModifiedDate == null || mindmap.getLastModified().before(lastModifiedDate);

        Log.v(MainApplication.TAG, "noPreviousMindmapLoaded: " + noPreviousMindmapLoaded);
        Log.v(MainApplication.TAG, "previousMindmapUriDiffers: " + previousMindmapUriDiffers);
        Log.v(MainApplication.TAG, "previousMindmapModifiedEarlier: " + previousMindmapModifiedEarlier);

        // open a new document if we (one of):
        // - have no document previously loaded
        // - the previously loaded document has a different URI
        // - the previously loaded document was modified earlier than this document (i.e. other document was modified after)
        if (noPreviousMindmapLoaded || previousMindmapUriDiffers || previousMindmapModifiedEarlier) {

            // create a ProgressDialog, and load the file asynchronously
            ProgressDialog progressDialog = ProgressDialog.show(
                    this,
                    "DroidPlane",
                    "Opening Mindmap File...",
                    true,
                    false
            );

            // Once the FileOpenTask is complete, we want to set up the horizontal mindmap view. If we already have
            // a mindmap, we run it immediately.
            Runnable onPostExecuteTask = new Runnable() {
                @Override
                public void run() {
                    setUpHorizontalMindmapView();
                }
            };

            // open file
            new FileOpenTask(intent, action, lastModifiedDate, progressDialog, onPostExecuteTask).execute();

        } else {
            setUpHorizontalMindmapView();
        }

    }

    private void setUpHorizontalMindmapView() {

        // create a new HorizontalMindmapView
        horizontalMindmapView = new HorizontalMindmapView(mindmap, this);

        ((LinearLayout)findViewById(R.id.layout_wrapper)).addView(horizontalMindmapView);

        // enable the up navigation with the Home (app) button (top left corner)
        horizontalMindmapView.enableHomeButtonIfEnoughColumns(this);

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        horizontalMindmapView.setApplicationTitle(this);
    }

    public HorizontalMindmapView getHorizontalMindmapView() {

        return horizontalMindmapView;
    }

    private class FileOpenTask extends AsyncTask<String, Void, Object> {

        private final Intent intent;
        private final String action;
        private final Date documentLastModified;
        private final ProgressDialog progressDialog;
        private final Runnable onPostExecuteTask;

        FileOpenTask(
                Intent intent,
                String action,
                Date documentLastModified,
                ProgressDialog progressDialog,
                Runnable onPostExecuteTask
        ) {

            this.intent = intent;
            this.action = action;
            this.documentLastModified = documentLastModified;
            this.progressDialog = progressDialog;
            this.onPostExecuteTask = onPostExecuteTask;
        }

        @Override
        protected Object doInBackground(String... strings) {

            // prepare loading of the Mindmap file
            InputStream mm = null;

            // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the
            // launcher started from ACTION_EDIT/VIEW intent
            if ((Intent.ACTION_EDIT.equals(action) || Intent.ACTION_VIEW.equals(action)) ||
                Intent.ACTION_OPEN_DOCUMENT.equals(action)
            ) {

                Log.d(MainApplication.TAG, "started from ACTION_EDIT/VIEW intent");

                // get the URI to the target document (the Mindmap we are opening) and open the InputStream
                Uri uri = intent.getData();
                if (uri != null) {
                    ContentResolver cr = getContentResolver();
                    try {
                        mm = cr.openInputStream(uri);
                    } catch (FileNotFoundException e) {

                        abortWithPopup(R.string.filenotfound);
                        e.printStackTrace();
                    }
                } else {
                    abortWithPopup(R.string.novalidfile);
                }

                // store the Uri. Next time the MainActivity is started, we'll
                // check whether the Uri has changed (-> load new document) or
                // remained the same (-> reuse previous document)
                mindmap.setUri(uri);

                // store the last modified date, so next time we know whether to reload the file
                // or display the same
                mindmap.setLastModified(documentLastModified);
            }

            // started from the launcher
            else {
                Log.d(MainApplication.TAG, "started from app launcher intent");

                // display the default Mindmap "example.mm", from the resources
                mm = getApplicationContext().getResources().openRawResource(R.raw.example);
            }

            // load the mindmap
            Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");
            mindmap.loadDocument(mm);

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

            if (onPostExecuteTask != null) {
                onPostExecuteTask.run();
            }

            if (progressDialog != null) {
                progressDialog.dismiss();
            }
        }
    }


    /**
     * Enables the home button if the Android version allows it
     */
    @SuppressLint("NewApi")
    void enableHomeButton() {
        // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Disables the home button if the Android version allows it
     */
    @SuppressLint("NewApi")
    void disableHomeButton() {
        // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
        ActionBar bar = getActionBar();
        if (bar != null) {
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
     * Handler for the back button, Navigate one level up, and stay at the root node
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {

        horizontalMindmapView.upOrClose();
    }

    /*
     * (non-Javadoc)
     *
     * Handler of all menu events Home button: navigate one level up, and exit the application if the home button is
     * pressed at the root node Menu Up: navigate one level up, and stay at the root node
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {

        switch (item.getItemId()) {

            // "Up" menu action
            case R.id.up:
                horizontalMindmapView.up();
                break;

            // "Top" menu action
            case R.id.top:
                horizontalMindmapView.top();
                break;

            // "Help" menu action
            case R.id.help:

                // create a new intent (without URI)
                Intent helpIntent = new Intent(this, MainActivity.class);
                helpIntent.putExtra(INTENT_START_HELP, true);
                startActivity(helpIntent);
                break;

            // "Open" menu action
            case R.id.open:
                performFileSearch();
                break;

                // App button (top left corner)
            case android.R.id.home:
                horizontalMindmapView.up();
                break;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * It looks like the onContextItemSelected has to be overwritten in a class extending Activity. It was not
     * possible to have this callback in the NodeColumn. As a result, we have to find out here again where the event
     * happened
     *
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        // contextMenuInfo.position is the position in the ListView where the context menu was loaded, i.e. the index
        // of the item in our mindmapNodeLayouts list

        // MindmapNodeLayout extends LinearView, so we can cast targetView back to MindmapNodeLayout
        MindmapNodeLayout mindmapNodeLayout = (MindmapNodeLayout)contextMenuInfo.targetView;
        Log.d(MainApplication.TAG, "mindmapNodeLayout.text = " + mindmapNodeLayout.getMindmapNode().getText());

        Log.d(MainApplication.TAG, "contextMenuInfo.position = " + contextMenuInfo.position);
        Log.d(MainApplication.TAG, "item.getTitle() = " + item.getTitle());

        switch (item.getGroupId()) {
            // normal menu entries
            case MindmapNodeLayout.CONTEXT_MENU_NORMAL_GROUP_ID:
                switch (item.getItemId()) {

                    // copy node text to clipboard
                    case R.id.contextcopy:
                        Log.d(MainApplication.TAG, "Copying text to clipboard");
                        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

                        ClipData clipData = ClipData.newPlainText("node", mindmapNodeLayout.getMindmapNode().getText());
                        clipboardManager.setPrimaryClip(clipData);

                        break;

                    // open the URI specified in the "LINK" tag
                    case R.id.contextopenlink:
                        Log.d(MainApplication.TAG, "Opening node link " + mindmapNodeLayout.getMindmapNode().getLink());
                        mindmapNodeLayout.openLink(this);

                        break;

                    // open RichText content
                    case R.id.openrichtext:
                        Log.d(MainApplication.TAG,
                                "Opening rich text of node " + mindmapNodeLayout.getMindmapNode().getRichTextContent()
                        );
                        mindmapNodeLayout.openRichText(this);


                        break;

                    default:
                        break;
                }
                break;

            // arrow links, resolve by the selected ID and jump to this node
            case MindmapNodeLayout.CONTEXT_MENU_ARROWLINK_GROUP_ID:
                int nodeNumericId = item.getItemId();
                MindmapNode nodeByNumericID = mindmap.getNodeByNumericID(nodeNumericId);
                horizontalMindmapView.downTo(this, nodeByNumericID);

        }

        return true;
    }

    /**
     * Shows a popup with an error message and then closes the application
     *
     * @param stringResourceId
     */
    private void abortWithPopup(int stringResourceId) {

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

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE. If the request code seen
        // here doesn't match, it's the response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent. Instead, a URI to that document
            // will be contained in the return intent provided to this method as a parameter. Pull that URI using
            // resultData.getData().
            if (resultData != null) {
                Uri uri = resultData.getData();
                Log.i(MainApplication.TAG, "Uri: " + uri.toString());

                // create a new intent (with URI) to open this document
                Intent openFileIntent = new Intent(this, MainActivity.class);
                openFileIntent.setData(uri);
                openFileIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                startActivity(openFileIntent);

            }
        }
    }
}
