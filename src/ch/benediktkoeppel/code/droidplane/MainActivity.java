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

import com.google.analytics.tracking.android.EasyTracker;

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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


// TODO: can we get built-in icons as SVG?
// TODO: properly parse rich text nodes
// TODO: when orientation changes, the onCreate is executed again. This means that we jump back to root. We should stay wherever we were before the orientation change. Maybe check if Uri is still the same, and if it is we don't re-initialize (but reuse).
// TODO: implement OnItemLongClickListener with a context menu (show all icons, follow link, copy text, and ultimately also edit)

public class MainActivity extends Activity implements OnItemClickListener {
	
	private static final String TAG = "DroidPlane";
	
	// MainApplication
	MainApplication application;

	// GUI stuff
	// Components
	ArrayList<ListView> listViews;
	//ArrayAdapter<String> adapter;
	//ArrayList<MindmapNodeAdapter> mindmapNodeAdapters;


	
	// Mindmap stuff
	InputStream mm;
	// XML document builder. The document itself is in the MainApplication
	DocumentBuilderFactory docBuilderFactory;
	DocumentBuilder docBuilder;
	
	@Override
	  public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance().activityStart(this);
	  }

	@Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance().activityStop(this);
	    EasyTracker.getInstance().dispatch();

	  }

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        application = (MainApplication)getApplication();
        int previousNumListViews = application.getNumListViews();

    	
        // initialize android stuff
        // EasyTracker
        EasyTracker.getInstance().setContext(this);
    	EasyTracker.getTracker().sendView("MainActivity");
        // components
        listViews = new ArrayList<ListView>();
        ListView listView0 = (ListView)findViewById(R.id.list_view0);
        ListView listView1 = (ListView)findViewById(R.id.list_view1);
        ListView listView2 = (ListView)findViewById(R.id.list_view2);
        if ( listView0 != null ) {
        	listView0.setOnItemClickListener(this);
        	//listView0.setOnItemLongClickListener(this);
        	listViews.add(listView0);
        }
        if ( listView1 != null ) {
        	listView1.setOnItemClickListener(this);
        	//listView1.setOnItemLongClickListener(this);
        	listViews.add(listView1);
        }
        if ( listView2 != null ) {
        	listView2.setOnItemClickListener(this);
        	//listView2.setOnItemLongClickListener(this);
        	listViews.add(listView2);
        }
        
        // if the number of list views has not changed, we simply re-attach all adapters
        if ( application.getListViews() != null ) {
	        if ( listViews.size() >= previousNumListViews ) {
	        	for (int i = 0; i < application.getListViews().size(); i++) {
	        		listViews.get(i).setAdapter(application.getListViews().get(i).getAdapter());
				}
	        }
	        
	        // we have fewer list views than before, we take the right most adapter
	        else /* if ( listViews.size() < previousNumListViews )*/ {
	        	for (int i = application.getListViews().size()-1; i > 0; i--) {
	        		if ( application.getListViews().get(i).getAdapter() != null ) {
	        			listViews.get(0).setAdapter(application.getListViews().get(i).getAdapter());
	        		}
				}
	        }
        }
        
        application.setListView(listViews);

    	// enable the Android home button
        enableHomeButton();
    	
    	// intents (how we are called)
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // start measuring the document load time
		long loadDocumentStartTime = System.currentTimeMillis();
		
		if ( application.document == null ) {
	        
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
				application.document = docBuilder.parse(mm);
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
			
			long loadDocumentEndTime = System.currentTimeMillis();
		    EasyTracker.getTracker().sendTiming("document", loadDocumentEndTime-loadDocumentStartTime, "loadDocument", "loadTime");
			Log.d(TAG, "Document loaded");
		    
			long numNodes = application.document.getElementsByTagName("node").getLength();
			EasyTracker.getTracker().sendEvent("document", "loadDocument", "numNodes", numNodes);
			
			// navigate down into the root node
			down(application.document.getDocumentElement());
			
		}
        
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

		// TODO: add "Find" button and menu -> should search underneath the
		// current node (or with an option, under the root node)

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;

	}
       
    
    // navigates to the top of the Mindmap
    public void top() {
    	
    	// discard all MindmapNodeAdapters
    	for (int i = 0; i < listViews.size(); i++) {
    		listViews.get(i).setAdapter(null);
		}

    	// clear the parents stack and re-add the document root node
    	application.parents.clear();
    	application.parents.push(application.document.getDocumentElement());
    	
    	// redraw display
    	listChildren();
    }
    
    // navigates back up one level in the Mindmap, if possible (otherwise does nothing)
    public void up() {
    	up(false);
    }

    // navigates back up one level in the Mindmap. If we already display the root node, the application will finish
	public void upOrClose() {
		up(true);
	}
    
    // navigates back up one level in the Mindmap, if possible. If force is true, the application closes if we can't go further up
	public void up(boolean force) {
				
		// make sure that there is more than 1 node in the parents stack: the current one, and it's parent
		// we pop the current node, and then display it's parent's child nodes
		if ( application.parents.size() > 1 ) {
			application.parents.pop();
			Log.d(TAG, "parents has " + application.parents.size() + " elements after popping");
			
			// remove the adapter as far right as possible
			for (int i = listViews.size()-1; i >= 0; i--) {
				if ( listViews.get(i).getAdapter() != null ) {
					listViews.get(i).setAdapter(null);
					Log.d(TAG, "Wiped ListView " + i);
					break;
				}
			}
			// TODO: this is a hack! listChildren() will add one adapter, so we
			// remove two. Proper solution would be if we tell the
			// listChildren() into which listView it has to write.
			for (int i = listViews.size()-1; i >= 0; i--) {
				if ( listViews.get(i).getAdapter() != null ) {
					listViews.get(i).setAdapter(null);
					Log.d(TAG, "Wiped ListView " + i);
					break;
				}
			}
			

			// TODO: in up(), if there are more than listViews.size() elements in
			// parents, but not all listViews have an adapter (i.e. the last listView
			// has adapter null), we can shift all adapters one to the right
			
			// TODO if we have enough parents to fill all listviews, we slide everything by one
//			if ( parents.size() >= listViews.size() ) {
//				
//			}
			
			// if we have not enough, we fill from the left as much as we can
			
			listChildren();
		}
		
		// there was no remaining node in parents, and force was specified, so we exit
		else if (force) {
			finish();
		}
		
		// otherwise (no remaining nodes, but force==false), we do nothing
	}
	
	// open up Node node, and display all its child nodes
    public void down(Node node) {
    	application.parents.push(node);
    	listChildren();
    }
    
    
    // lists the child nodes of the latest parent (i.e. of parents.peek() Node)
    private void listChildren() {
    	
    	// enable the up navigation with the Home (app) button (top left corner)
    	// if we only have one parent (i.e. this is the root node), then we disable the home button
    	if ( application.parents.size() > 1 ) {
    		enableHomeButton();
    	} else {
    		disableHomeButton();
    	}
    	
		// set activity title to current parent's text. This is only possible if
		// the parent is indeed an Element, a tag "node" and has a "TEXT"
		// attribute. This should be always the case, but just be on the safe
		// side.
    	Node parent_n = application.parents.peek();
    	if ( isMindmapNode(parent_n) ) {
    		Element parent_e = getMindmapNode(parent_n);
   			String text = parent_e.getAttribute("TEXT");
			if ( !text.equals("") ) {
				setTitle(text);
			} else {
				setTitle(R.string.app_name);
			}
    	} else {
			setTitle(R.string.app_name);
		}
    	

    	// fetch the children nodes of the current parent
    	ArrayList<MindmapNode> mindmapNodes = new ArrayList<MainActivity.MindmapNode>();
    	NodeList tmp_children = application.parents.peek().getChildNodes();
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
    			
    			mindmapNodes.add(new MindmapNode(text, icon, icon_id, expandable, tmp_n));
    		}
 		}
    	
    	// create adapter (i.e. data provider) for the listView
    	MindmapNodeAdapter adapter = new MindmapNodeAdapter(this, R.layout.mindmap_node_list_item, mindmapNodes);

    	// all ListViews already have a MindmapNodeAdapter
    	if ( listViews.get(listViews.size()-1).getAdapter() != null ) {
        	// need to shift all one to the left
    		for (int i = 1; i < listViews.size(); i++) {
    			listViews.get(i-1).setAdapter(listViews.get(i).getAdapter());
			}
    		// and attach our new adapter at the far right
    		listViews.get(listViews.size()-1).setAdapter(adapter);    		
    	} 
    	
    	// find the first free listView (i.e. listView with no adapter)
    	else {
    		for (int i = 0; i < listViews.size(); i++) {
    			if ( listViews.get(i).getAdapter() == null ) {
    				// and attach our adapter at the first free ListView
    				listViews.get(i).setAdapter(adapter);
    				break;
    			}
			}
    	}

    }

    
    public int getNumListViews() {
    	return listViews.size();
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
		
		// find out which view it was, then null all adapters to the right
		// also, because clicking on a ListView which is not the right-most ListView is like going "up", hence we have to pop elements from parents. 
		for (int i = listViews.lastIndexOf((ListView)parent)+1; i < listViews.size(); i++) {
			if ( listViews.get(i).getAdapter() != null ) {
				listViews.get(i).setAdapter(null);
				application.parents.pop();
			}
		}

		// then get all nodes from this adapter
		MindmapNodeAdapter adapter = (MindmapNodeAdapter)((ListView)parent).getAdapter();
		ArrayList<Node> currentListedNodes = adapter.getNodeList();
	
		// extract the pushed node
		Node pushedNode = currentListedNodes.get(position);
		
		// give the pushed node a special color
		adapter.setItemColor(position);
		
		// and drill down (if it has child nodes)
		if ( getNumChildMindmapNodes(pushedNode) > 0 ) {
			down(pushedNode);
		}
		
	}
	
	

	// Handler when an item is long clicked
	// TODO do this!
//	@Override
//	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//		
//		Node pushedNode = currentListedNodes.get(position);
//		
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage("Not yet implemented");
//        builder.setCancelable(true);
//        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//        	public void onClick(DialogInterface dialog, int which) {
//        		return;
//        	}
//        });
//
//        AlertDialog alert = builder.create();
//        alert.show();
//		
//		return true;
//		
//	}

	
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
		private String text;
		private String icon_name;
		private int icon_res_id;
		private boolean isExpandable;
		private Node node;
		private View view;
		private boolean selected;
		
		
		public MindmapNode(String text, String icon_name, int icon_res_id, boolean isExpandable, Node node) {
			this.text = text;
			this.icon_name = icon_name;
			this.icon_res_id = icon_res_id;
			this.isExpandable = isExpandable;
			this.node = node;
		}
		
		public Node getNode() {
			return this.node;
		}

		public void setView(View view) {
			this.view = view;
		}
		
		public View getView() {
			return this.view;
		}
		
		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public boolean getIsSelected() {
			return this.selected;
		}
		
	}
	
	class MindmapNodeAdapter extends ArrayAdapter<MindmapNode> {
		private ArrayList<MindmapNode> mindmapNodes;
		
		public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNode> mindmapNodes) {
			super(context, textViewResourceId, mindmapNodes);
			this.mindmapNodes = mindmapNodes;
		}
		
		public ArrayList<Node> getNodeList() {
			ArrayList<Node> nodeList = new ArrayList<Node>();
			for (int i = 0; i < mindmapNodes.size(); i++) {
				nodeList.add(mindmapNodes.get(i).getNode());
			}
			return nodeList;
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
				
				// if the node is selected, give it a special background
				if ( node.getIsSelected() ) {
					int backgroundColor;
					
					// menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
			    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			    		backgroundColor = getResources().getColor(android.R.color.holo_blue_bright);
			    	} else {
			    		backgroundColor = getResources().getColor(android.R.color.background_dark);
			    	}
					
					v.setBackgroundColor(backgroundColor);	
				} else {
					v.setBackgroundColor(0);
				}
				
				node.setView(v);
			}
			
			return v;
		}
		

		// Sets the color on the node at the specified position
		@SuppressLint("InlinedApi")
		public void setItemColor(int position) {
			
			// unselect all nodes
			for (int i = 0; i < mindmapNodes.size(); i++) {
				mindmapNodes.get(i).setSelected(false);
			}
			
			// then select node at position
			mindmapNodes.get(position).setSelected(true);
			
			// then notify about the GUI change
			this.notifyDataSetChanged();
		}
	}


}