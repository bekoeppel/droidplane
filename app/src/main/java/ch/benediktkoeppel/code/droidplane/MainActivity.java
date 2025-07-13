package ch.benediktkoeppel.code.droidplane;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import ch.benediktkoeppel.code.droidplane.controller.AsyncMindmapLoaderTask;
import ch.benediktkoeppel.code.droidplane.controller.OnRootNodeLoadedListener;
import ch.benediktkoeppel.code.droidplane.model.Mindmap;
import ch.benediktkoeppel.code.droidplane.model.MindmapNode;
import ch.benediktkoeppel.code.droidplane.view.HorizontalMindmapView;
import ch.benediktkoeppel.code.droidplane.view.MindmapNodeLayout;

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
    private Menu menu;
    private boolean mindmapIsLoading;

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

        // Edge-to-edge: disable decor fitting system windows
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // initialize android stuff
        // Google Analytics
        Tracker tracker = ((MainApplication)getApplication()).getTracker();
        tracker.setScreenName("MainActivity");
        tracker.send(new HitBuilders.EventBuilder().build());

        // enable the Android home button
        enableHomeButton();

        // set up horizontal mindmap view first
        setUpHorizontalMindmapView();

        // get the Mindmap ViewModel
        mindmap = ViewModelProviders.of(this).get(Mindmap.class);

        // then populate view with mindmap
        // if we already have a loaded mindmap, use this; otherwise load from the intent
        if (mindmap.isLoaded()) {
            horizontalMindmapView.setMindmap(mindmap);
            horizontalMindmapView.setDeepestSelectedMindmapNode(mindmap.getRootNode());
            horizontalMindmapView.onRootNodeLoaded();
            mindmap.getRootNode().subscribeNodeRichContentChanged(this);

        } else {

            OnRootNodeLoadedListener onRootNodeLoadedListener = new OnRootNodeLoadedListener() {
                @Override
                public void rootNodeLoaded(Mindmap mindmap, MindmapNode rootNode) {
                    // now set up the view
                    MindmapNode finalRootNode = rootNode;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            horizontalMindmapView.setMindmap(mindmap);

                            // by default, the root node is the deepest node that is expanded
                            horizontalMindmapView.setDeepestSelectedMindmapNode(finalRootNode);

                            horizontalMindmapView.onRootNodeLoaded();

                        }
                    });

                }
            };

            // load the file asynchronously
            new AsyncMindmapLoaderTask(
                    this,
                    onRootNodeLoadedListener,
                    mindmap,
                    horizontalMindmapView,
                    getIntent()
            ).execute();

        }

    }

    private void setUpHorizontalMindmapView() {

        // create a new HorizontalMindmapView
        horizontalMindmapView = new HorizontalMindmapView(this);

        ((LinearLayout)findViewById(R.id.layout_wrapper)).addView(horizontalMindmapView);

        // enable the up navigation with the Home (app) button (top left corner)
        horizontalMindmapView.enableHomeButtonIfEnoughColumns(this);

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        horizontalMindmapView.setApplicationTitle(this);
    }

    public HorizontalMindmapView getHorizontalMindmapView() {

        return horizontalMindmapView;
    }


    /**
     * Enables the home button if the Android version allows it
     */
    @SuppressLint("NewApi")
    public void enableHomeButton() {
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
    public void disableHomeButton() {
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
        this.menu = menu;
        updateLoadingIndicatorOnUiThread();
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
                
            case R.id.search:
                horizontalMindmapView.startSearch();
                break;
            case R.id.search_next:
                horizontalMindmapView.searchNext();
                break;
            case R.id.search_prev:
                horizontalMindmapView.searchPrevious();
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
                                "Opening rich text of node " + mindmapNodeLayout.getMindmapNode().getRichTextContents()
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
                horizontalMindmapView.downTo(this, nodeByNumericID, true);

        }

        return true;
    }

    /**
     * Shows a popup with an error message and then closes the application
     *
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
        super.onActivityResult(requestCode, resultCode, resultData);
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


    public void setMindmapIsLoading(boolean mindmapIsLoading) {

        this.mindmapIsLoading = mindmapIsLoading;

        // update the loading indicator in the menu
        updateLoadingIndicatorOnUiThread();
    }

    private void updateLoadingIndicatorOnUiThread() {
        if (menu != null && menu.findItem(R.id.mindmap_loading) != null) {

            MenuItem mindmapLoadingIndicator = menu.findItem(R.id.mindmap_loading);

            runOnUiThread(() -> mindmapLoadingIndicator.setVisible(mindmapIsLoading));
        }
    }

    public void notifyNodeRichContentChanged() {
        this.horizontalMindmapView.notifyNodeContentChanged(this);
    }
}
