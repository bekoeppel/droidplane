package ch.benediktkoeppel.code.droidplane;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
/**
 * The DroidPlane main application. It stores the loaded Uri and document, so
 * that we can recreate the MainActivity after a screen rotation.
 */
@ReportsCrashes(formKey = "dE1VQVpQN2FNTWlLQXg1UUQ1b1VSN3c6MQ") 
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
	 * the application context
	 */
	private static Context context;
	
	/**
	 * the main application instance
	 */
	private static MainApplication instance;

	/**
	 * the main activity instance
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
	public Mindmap mindmap;
	
	

	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
		
		// save the context
		MainApplication.context = getApplicationContext();
		
		// save the instance
		MainApplication.instance = this;

		// set up Google Analytics
		googleAnalytics = GoogleAnalytics.getInstance(this);
		tracker = googleAnalytics.newTracker(R.xml.global_tracker);

	}
	
	/**
	 * Helper to return the application context, even for static methods.
	 * @return
	 */
	public static Context getStaticApplicationContext() {
		return MainApplication.context;
	}

	/**
	 * Get the main application instance
	 * @return the main application
	 */
	public static MainApplication getInstance() {
		return MainApplication.instance;
	}
	
	/**
	 * Stores the MainActivity instance
	 * @param mainActivityInstance
	 */
	public static void setMainActivityInstance(MainActivity mainActivityInstance) {
		MainApplication.mainActivityInstance = mainActivityInstance;
	}
	
	public static MainActivity getMainActivityInstance() {
		return MainApplication.mainActivityInstance;
	}
	
	

	public static Tracker getTracker() {
		return tracker;
	}

	public static GoogleAnalytics getGoogleAnalytics() {
		return googleAnalytics;
	}
}
