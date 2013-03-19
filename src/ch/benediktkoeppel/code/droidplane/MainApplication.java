package ch.benediktkoeppel.code.droidplane;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Context;

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
	 * A reference to the Mindmap document
	 */
	public Mindmap mindmap;
	
	/*
	 * (non-Javadoc)
	 * When the application is created, we set up ACRA (crash reporting) and
	 * store the application instance so that we can fetch the application and
	 * its context later in static methods.
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
		
		// save the context
		MainApplication.context = getApplicationContext();
		
		// save the instance
		MainApplication.instance = this;
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
	
	/**
	 * Retrieves the instance of the main activity
	 * @return
	 */
	public static MainActivity getMainActivityInstance() {
		return MainApplication.mainActivityInstance;
	}
	
	
}
