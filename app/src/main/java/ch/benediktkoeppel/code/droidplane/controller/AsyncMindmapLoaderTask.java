package ch.benediktkoeppel.code.droidplane.controller;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.MainApplication;
import ch.benediktkoeppel.code.droidplane.R;
import ch.benediktkoeppel.code.droidplane.model.Mindmap;
import ch.benediktkoeppel.code.droidplane.view.HorizontalMindmapView;

public class AsyncMindmapLoaderTask extends AsyncTask<String, Void, Object> {

    private final MainActivity mainActivity;
    private final Intent intent;
    private final String action;
    private final HorizontalMindmapView horizontalMindmapView;

    private final Mindmap mindmap;

    public AsyncMindmapLoaderTask(MainActivity mainActivity,
                                  Mindmap mindmap,
                                  HorizontalMindmapView horizontalMindmapView,
                                  Intent intent) {

        this.mainActivity = mainActivity;
        this.intent = intent;
        this.action = intent.getAction();
        this.horizontalMindmapView = horizontalMindmapView;
        this.mindmap = mindmap;
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
                ContentResolver cr = mainActivity.getContentResolver();
                try {
                    mm = cr.openInputStream(uri);
                } catch (FileNotFoundException e) {

                    mainActivity.abortWithPopup(R.string.filenotfound);
                    e.printStackTrace();
                }
            } else {
                mainActivity.abortWithPopup(R.string.novalidfile);
            }

            // store the Uri. Next time the MainActivity is started, we'll
            // check whether the Uri has changed (-> load new document) or
            // remained the same (-> reuse previous document)
            this.mindmap.setUri(uri);
        }

        // started from the launcher
        else {
            Log.d(MainApplication.TAG, "started from app launcher intent");

            // display the default Mindmap "example.mm", from the resources
            mm = mainActivity.getApplicationContext().getResources().openRawResource(R.raw.example);
        }

        // load the mindmap
        Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");
        this.mindmap.loadDocument(mm, new Runnable() {
            @Override
            public void run() {
                // TODO: this doesn't have to be two threads nested into each other
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        horizontalMindmapView.setMindmap(mindmap);
                        horizontalMindmapView.onRootNodeLoaded();
                    }
                });

            }
        });

        return null;
    }
}
