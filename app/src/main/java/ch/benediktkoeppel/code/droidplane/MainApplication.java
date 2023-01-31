package ch.benediktkoeppel.code.droidplane;

import android.app.Application;

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
     * Google Analytics
     */
    //private static GoogleAnalytics googleAnalytics;
    //private static Tracker tracker;

    @Override
    public void onCreate() {

        super.onCreate();

        // set up Google Analytics
        //googleAnalytics = GoogleAnalytics.getInstance(this);
        //tracker = googleAnalytics.newTracker(R.xml.global_tracker);

    }

//    /**
//     * Get the Google Analytics tracker
//     *
//     * @returnGoogle Analytics tracker
//     */
//    public static Tracker getTracker() {
//
//        return tracker;
//    }
//
//    /**
//     * Get the Google Analytics API
//     * @return Google Analytics API
//     */
//    public static GoogleAnalytics getGoogleAnalytics() {
//
//        return googleAnalytics;
//    }
}
