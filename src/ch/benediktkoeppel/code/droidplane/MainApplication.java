package ch.benediktkoeppel.code.droidplane;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import org.acra.*;
import org.acra.annotation.*;
import org.w3c.dom.Document;

@ReportsCrashes(formKey = "dE1VQVpQN2FNTWlLQXg1UUQ1b1VSN3c6MQ") 
public class MainApplication extends Application {
	
	public static final String TAG = "DroidPlane";
	
	// TODO: why the hell is this all sitting in here??? should go to an instance variable of the MainActivity I guess
	
	// the document which is used in MainActivity
	public Document document;

	private Uri uri;
	
	// HorizontalMindmapView that contains all NodeColumns
	public HorizontalMindmapView horizontalMindmapView;

	// the application context
	private static Context context;
	
	

	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
		
		// save the context
		MainApplication.context = getApplicationContext();
	}

	public Uri getUri() {
		return this.uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}
	
	public static Context getStaticApplicationContext() {
		return MainApplication.context;
	}

	
	
}
