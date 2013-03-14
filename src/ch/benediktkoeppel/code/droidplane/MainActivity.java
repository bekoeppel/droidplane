package ch.benediktkoeppel.code.droidplane;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity implements OnItemClickListener {
	
	private static final String TAG = "DroidPlane";

	// GUI stuff
	// Menu identifiers
	private static final int MENU_UP = 0;
	
	// Components
	ListView listView;
	ArrayAdapter<String> adapter;


	
	// Mindmap stuff
	InputStream mm;
	// XML document
	DocumentBuilderFactory docBuilderFactory;
	DocumentBuilder docBuilder;
	Document document;
	// stack of parent nodes
	// the latest parent node (all visible nodes are child of this currentParent) is parents.peek()
	Stack<Node> parents = new Stack<Node>();
	// the currently visible nodes
	ArrayList<Node> currentListedNodes = new ArrayList<Node>();
	// the text of the currently visible nodes
	ArrayList<String> str_currentListedNodes = new ArrayList<String>();
	
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // initialize android stuff
        // components
        listView = (ListView)findViewById(R.id.list_view);
    	listView.setOnItemClickListener(this);
    	
        // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
    	
    	// intents (how we are called)
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        
        // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the launcher
        // started from ACTION_EDIT/VIEW intent
        if ((Intent.ACTION_EDIT.equals(action)||Intent.ACTION_VIEW.equals(action)) && type != null) {
        	
        	Log.d(TAG, "started from ACTION_EDIT/VIEW intent");
        	
        	// get the URI to the target document (the Mindmap we are opening) and open the InputStream
        	Uri uri = intent.getData();
        	if ( uri != null ) {
        		ContentResolver cr = getContentResolver();
        		try {
					mm = cr.openInputStream(uri);
				} catch (FileNotFoundException e) {
					// TODO: show a pop up that the file does not exist
					e.printStackTrace();
				}
        	}
        } 
        
        // started from the launcher
        else {
        	
        	Log.d(TAG, "started from app launcher intent");
        	
        	// display the default Mindmap "example.mm", from the resources
	    	mm = this.getResources().openRawResource(R.raw.example);
        }
        
        Log.d(TAG, "InputStream fetched, now starting to load document");
        
        // load the Mindmap from the InputStream
        docBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			document = docBuilder.parse(mm);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		Log.d(TAG, "Document loaded");
		

		// TODO: in the beginning, don't only display the root node. display it as if the root node was already opened
		// TODO: in the menu bar, the name of the root node should be displayed (truncated to fit the width)
    	down(document.getDocumentElement());
        
    }

    // sets the menu
    @Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {

    	// TODO: shouldn't this be in some kind of menu.xml?
    	// add one menu item "Up"
    	// TODO: what does this line actually do? what do the two 0 mean?
		menu.add(0, MENU_UP, 0, "Up");
		
		// TODO: add "Go to Top" button
		// TODO: add "Find" button and menu -> should search underneath the current node (or with an option, under the root node)
		return true;
	}
       
    

    // navigates back up one level in the Mindmap, if possible (otherwise does nothing)
	public void up() {
		
		// make sure that there is more than 1 node in the parents stack: the current one, and it's parent
		// we pop the current node, and then display it's parent's child nodes
		if ( parents.size() > 1 ) {
			parents.pop();
			listChildren();
		}
	}
	
	// navigates back up one level in the Mindmap. If we already display the root node, the application will finish
	public void upOrClose() {
		if ( parents.size() > 1 ) {
			parents.pop();
			listChildren();
		} else {
			finish();
		}
	}
    
	// open up Node node, and display all its child nodes
    public void down(Node node) {
    	parents.push(node);
    	listChildren();
    }
    
    
    // lists the child nodes of the latest parent (i.e. of parents.peek() Node)
    private void listChildren() {
    	
    	// create new, empty ArrayLists
    	str_currentListedNodes = new ArrayList<String>();
    	currentListedNodes = new ArrayList<Node>();
    	
    	// fetch the children nodes of the current parent
    	NodeList tmp_children = parents.peek().getChildNodes();
    	for (int i = 0; i < tmp_children.getLength(); i++) {
    		Node tmp_n = tmp_children.item(i);
    		
    		// only interested in the elements (no comments, etc.)
    		if ( tmp_n.getNodeType() == Node.ELEMENT_NODE ) {
    			Element tmp_e = (Element)tmp_n;

    			// TODO: handle other tags, such as icons, links etc.
    			
    			// only interested in nodes called "node"
    			if ( tmp_e.getTagName().equals("node") ) {
    				
    				// extract the string (TEXT attribute) of the nodes
	    			str_currentListedNodes.add(tmp_e.getAttribute("TEXT"));
	    			currentListedNodes.add(tmp_e);
    			}
    		}
		}
    	
    	// create adapter (i.e. data provider) for the listView
    	// TODO: maybe we need a special list item layout, so that we can show links, icons and text
    	adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, str_currentListedNodes);
    	listView.setAdapter(adapter); 
    	  	
    }

    
    
    // Handler of all menu events
    // Home button: navigate one level up, and exit the application if the home button is pressed at the root node
    // Menu Up: navigate one level up, and stay at the root node
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
    	// "Up" menu action
    	case MENU_UP:
    		up();
    		break;
    		
		// App button (top left corner)
    	case android.R.id.home:
    		upOrClose();
        	break;
    	}
    	
    	return true;
    }
    
    // Handler for the back button
    // Navigate one level up, and stay at the root node
    @Override
    public void onBackPressed() {
    	up();
    }
    
    // Handler when one of the ListItem's item is clicked
    // Find the node which was clicked, and redraw the screen with this node as new parent
    // TODO: menu items should be clickable across the full width!
    // TODO: menu items with no children should not be clickable
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	
		down(currentListedNodes.get(position));
	}


}