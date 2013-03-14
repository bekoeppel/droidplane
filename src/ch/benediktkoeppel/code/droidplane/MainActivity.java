package ch.benediktkoeppel.code.droidplane;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.acra.ACRA;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnItemClickListener {
	
	private static final String TAG = "DroidPlane";

	// GUI stuff
	// Components
	ListView listView;
	//ArrayAdapter<String> adapter;
	MindmapNodeAdapter adapter;


	
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
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // initialize android stuff
        // components
        listView = (ListView)findViewById(R.id.list_view);
    	listView.setOnItemClickListener(this);
    	
        enableHomeButton();
    	
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

			    	abortWithPopup(R.string.filenotfound);
			    	
					ACRA.getErrorReporter().putCustomData("Exception", "FileNotFoundException");
					ACRA.getErrorReporter().putCustomData("Intent", "ACTION_EDIT/VIEW");
					ACRA.getErrorReporter().putCustomData("URI", uri.toString());
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
			ACRA.getErrorReporter().putCustomData("Exception", "ParserConfigurationException");
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "SAXException");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			ACRA.getErrorReporter().putCustomData("Exception", "IOException");
			e.printStackTrace();
			return;
		}
		
		Log.d(TAG, "Document loaded");
		
		// navigate down into the root node
		down(document.getDocumentElement());
        
    }

    // enables the home button
	@SuppressLint("NewApi")
	private void enableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
	}
	
    // disables the home button
	@SuppressLint("NewApi")
	private void disableHomeButton() {
		// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(false);
    	}
	}
	

    // sets the menu
    @Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {

		// TODO: add "Find" button and menu -> should search underneath the current node (or with an option, under the root node)

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;

	}
       
    
    // navigates to the top of the Mindmap
    public void top() {
    	parents.clear();
    	parents.push(document.getDocumentElement());
    	listChildren();
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
    	
    	// enable the up navigation with the Home (app) button (top left corner)
    	// if we only have one parent (i.e. this is the root node), then we disable the home button
    	if ( parents.size() > 1 ) {
    		enableHomeButton();
    	} else {
    		disableHomeButton();
    	}
    	
		// set activity title to current parent's text. This is only possible if
		// the parent is indeed an Element, a tag "node" and has a "TEXT"
		// attribute. This should be always the case, but just be on the safe
		// side.
    	Node parent_n = parents.peek();
    	if ( isMindmapNode(parent_n) ) {
    		Element parent_e = getMindmapNode(parent_n);
   			String text = parent_e.getAttribute("TEXT");
			if ( !text.equals("") ) {
				setTitle(text);
			} else {
				setTitle(R.string.app_name);
			}
    	}
    	
    	// create new, empty ArrayLists
    	str_currentListedNodes = new ArrayList<String>();
    	currentListedNodes = new ArrayList<Node>();
    	ArrayList<MindmapNode> currentListedMindmapNodes = new ArrayList<MindmapNode>();
    	
    	// fetch the children nodes of the current parent
    	NodeList tmp_children = parents.peek().getChildNodes();
    	for (int i = 0; i < tmp_children.getLength(); i++) {
    		Node tmp_n = tmp_children.item(i);
    		
    		if ( isMindmapNode(tmp_n) ) {
    			Element tmp_e = getMindmapNode(tmp_n);
				
				// extract the string (TEXT attribute) of the nodes
    			String text = tmp_e.getAttribute("TEXT");
    			
    			// extract icons
    			// TODO: how do we handle multiple icons?
    			ArrayList<String> icons = getIcons(tmp_e);
    			String icon="";
    			int icon_id = 0;
    			if ( icons.size() > 0 ) {
    				icon = icons.get(0);
    				icon_id = getResources().getIdentifier("@drawable/" + icon, "id", getPackageName());
    			}
    			
    			// find out if it has sub nodes
    			boolean expandable = ( getNumChildMindmapNodes(tmp_e) > 0 );
    			
    			str_currentListedNodes.add(text);
    			currentListedNodes.add(tmp_e);
    			currentListedMindmapNodes.add(new MindmapNode(text, icon, icon_id, expandable));
    			
    			
    		}
 		}
    	
    	// create adapter (i.e. data provider) for the listView
    	adapter = new MindmapNodeAdapter(this, R.layout.mindmap_node_list_item, currentListedMindmapNodes);
    	listView.setAdapter(adapter); 

    }

    
    
    // Handler of all menu events
    // Home button: navigate one level up, and exit the application if the home button is pressed at the root node
    // Menu Up: navigate one level up, and stay at the root node
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
    	
    	switch (item.getItemId()) {
    	
    	// "Up" menu action
    	case R.id.up:
    		up();
    		break;
    		
		// "Top" menu action
    	case R.id.top:
    		top();
    		break;
    		
		// App button (top left corner)
    	case android.R.id.home:
    		up();
        	break;
    	}
    	
    	return true;
    }
    
    // Handler for the back button
    // Navigate one level up, and stay at the root node
    @Override
    public void onBackPressed() {
    	upOrClose();
    }
    
    // Handler when one of the ListItem's item is clicked
    // Find the node which was clicked, and redraw the screen with this node as new parent
    // if the clicked node has no child, then we stop here
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	
		Node pushedNode = currentListedNodes.get(position);
		if ( getNumChildMindmapNodes(pushedNode) > 0 ) {
			down(pushedNode);
		}
		
	}
	
	// returns the number of child Mindmap nodes 
	// "Mindmap node" = XML node && ELEMENT_NODE && "node" tag
	private int getNumChildMindmapNodes(Node node) {
		
		int numMindmapNodes = 0;
		
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( isMindmapNode(n) ) {
				numMindmapNodes++;
			}
		}
		
		return numMindmapNodes;
		
	}
	
	// checks whether node is a Mindmap node, i.e. has type ELEMENT_NODE and tag "node"
	private boolean isMindmapNode(Node node) {
		
		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			Element element = (Element)node;
			
			if ( element.getTagName().equals("node") ) {
				return true;
			}
		}
		
		return false;
		
	}
	
	private ArrayList<String> getIcons(Element node) {
		
		ArrayList<String> icons = new ArrayList<String>();
		
		NodeList childNodes = node.getChildNodes();		
		for (int i = 0; i < childNodes.getLength(); i++) {
			
			Node n = childNodes.item(i);
			if ( n.getNodeType() == Node.ELEMENT_NODE ) {
				Element e = (Element)n;
				
				if ( e.getTagName().equals("icon") && e.hasAttribute("BUILTIN") ) {
					icons.add(getDrawableNameFromMindmapIcon(e.getAttribute("BUILTIN")));
				}
			}
		}
		
		return icons;
		
	}
	
	// converts the node into an Element. If it is no Mindmap node, null is returned
	private Element getMindmapNode(Node node) {
		
		if ( isMindmapNode(node) ) {
			return (Element)node;
		} else {
			return null;
		}
		
	}
	

	// Mindmap icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]
	private String getDrawableNameFromMindmapIcon(String iconName) {
		Locale locale = getResources().getConfiguration().locale;
		String name = "icon_" + iconName.toLowerCase(locale).replaceAll("[^a-z0-9_.]", "_");
		name.replaceAll("_$", "");
		return name;
	}
	
	// Shows a popup with an error message and then closes the application
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
	
	// TODO: maybe we should have proper Mindmap and MindmapNode classes
	class MindmapNode {
		public String text;
		public String icon_name;
		public int icon_res_id;
		public boolean isExpandable;
		
		
		public MindmapNode(String text, String icon_name, int icon_res_id, boolean isExpandable) {
			this.text = text;
			this.icon_name = icon_name;
			this.icon_res_id = icon_res_id;
			this.isExpandable = isExpandable;
		}
	}
	
	class MindmapNodeAdapter extends ArrayAdapter<MindmapNode> {
		private ArrayList<MindmapNode> mindmapNodes;
		
		public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNode> mindmapNodes) {
			super(context, textViewResourceId, mindmapNodes);
			this.mindmapNodes = mindmapNodes;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if ( v == null ) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.mindmap_node_list_item, null);
			}
			
			MindmapNode node = mindmapNodes.get(position);
			if ( node != null) {
				
				TextView text = (TextView) v.findViewById(R.id.label);
				text.setText(node.text);
				
				ImageView icon = (ImageView) v.findViewById(R.id.icon);
				icon.setImageResource(node.icon_res_id);
				icon.setContentDescription(node.icon_name);
				
				TextView expandable = (TextView) v.findViewById(R.id.expandable);
				if ( node.isExpandable ) {
					expandable.setText("+");
				} else {
					expandable.setText("");
				}
			}
			
			return v;
		}
	}

}