package ch.benediktkoeppel.code.droidplane;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import org.acra.*;
import org.acra.annotation.*;
import org.w3c.dom.Document;

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
	 * the XML DOM document, the mind map
	 */
	public Document document;

	/**
	 * The currently loaded Uri
	 */
	private Uri uri;
	
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
	 * Returns the Uri which is currently loaded in document.
	 * @return Uri
	 */
	public Uri getUri() {
		return this.uri;
	}

	/**
	 * Set the Uri after loading a new document.
	 * @param uri
	 */
	public void setUri(Uri uri) {
		this.uri = uri;
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
	
	
}
