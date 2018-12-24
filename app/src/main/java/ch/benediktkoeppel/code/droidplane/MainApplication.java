package ch.benediktkoeppel.code.droidplane;

import android.app.Application;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

/**
 * The DroidPlane main application. It stores the loaded Uri and document, so that we can recreate the MainActivity
 * after a screen rotation.
 */
public class MainApplication extends Application {

    /**
     * Android Logging TAG
     */
    public static final String TAG = "DroidPlane";

    /**
     * HorizontalMindmapView that contains all NodeColumns
     */
    public HorizontalMindmapView horizontalMindmapView;

    /**
     * The main application instance
     */
    private static MainApplication instance;

    /**
     * The main activity instance
     */
    private static MainActivity mainActivityInstance;

    /**
     * Google Analytics
     */
    private static GoogleAnalytics googleAnalytics;
    private static Tracker tracker;

    /**
     * A reference to the Mindmap document
     */
    private Mindmap mindmap;


    @Override
    public void onCreate() {

        super.onCreate();

        // save the context
        //MainApplication.context = getApplicationContext();

        // save the instance
        MainApplication.instance = this;

        // set up Google Analytics
        googleAnalytics = GoogleAnalytics.getInstance(this);
        tracker = googleAnalytics.newTracker(R.xml.global_tracker);

    }

    /**
     * Get the main application instance
     *
     * @return the main application
     */
    public static MainApplication getInstance() {

        return MainApplication.instance;
    }

    /**
     * Stores the MainActivity instance
     *
     * @param mainActivityInstance
     */
    public static void setMainActivityInstance(MainActivity mainActivityInstance) {

        MainApplication.mainActivityInstance = mainActivityInstance;
    }

    /**
     * Get the main activity
     *
     * @return main activity
     */
    public static MainActivity getMainActivityInstance() {

        return MainApplication.mainActivityInstance;
    }

    /**
     * Get the Google Analytics tracker
     *
     * @returnGoogle Analytics tracker
     */
    public static Tracker getTracker() {

        return tracker;
    }

    /**
     * Get the Google Analytics API
     * @return Google Analytics API
     */
    public static GoogleAnalytics getGoogleAnalytics() {

        return googleAnalytics;
    }

    /**
     * Returns the open mindmap
     * @return
     */
    public Mindmap getMindmap() {

        return mindmap;
    }

    public void setMindmap(Mindmap mindmap) {
        this.mindmap = mindmap;
    }
}
