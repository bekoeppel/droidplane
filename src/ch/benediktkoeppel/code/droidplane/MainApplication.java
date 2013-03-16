package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.Stack;

import android.app.Application;
import android.net.Uri;
import android.widget.ListView;

import org.acra.*;
import org.acra.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

@ReportsCrashes(formKey = "dE1VQVpQN2FNTWlLQXg1UUQ1b1VSN3c6MQ") 
public class MainApplication extends Application {
	

	// listViews which were created at the last MainActivity.onCreate
	private ArrayList<ListView> listViews;
	
	// the document which is used in MainActivity
	public Document document;
	
	// the parent stack which is used in the MainActivity
	// the latest parent node (all visible nodes are child of this currentParent) is parents.peek()
	Stack<Node> parents = new Stack<Node>();

	private Uri uri;

	

	@Override
	public void onCreate() {
		super.onCreate();

		// initialize ACRA crash reports
		ACRA.init(this);
	}

	public void setListView(ArrayList<ListView> listViews) {
		this.listViews = listViews;
	}

	public ArrayList<ListView> getListViews() {
		return this.listViews;
	}

	public int getNumListViews() {
		if ( this.listViews == null ) {
			return 0;
		} else {
			return this.listViews.size();
		}
	}

	public Uri getUri() {
		return this.uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	
	
}
