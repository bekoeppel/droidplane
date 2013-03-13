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

	private static final int MENU_UP = 0;
	
	InputStream mm;
	ListView listView;

	
	DocumentBuilderFactory docBuilderFactory;
	DocumentBuilder docBuilder;
	Document document;
	
	Stack<Node> parents = new Stack<Node>();
	Node currentParent;
	ArrayList<Node> currentListedNodes = new ArrayList<Node>();
	ArrayList<String> str_currentListedNodes = new ArrayList<String>();
	
	
	
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		menu.add(0, MENU_UP, 0, "Up");
		return true;
	}
	
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        listView = (ListView)findViewById(R.id.list_view);

    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    	ActionBar bar = getActionBar();
	    	bar.setDisplayHomeAsUpEnabled(true);
    	}
    	
    	
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        InputStream mm = null;
        
        
        if ((Intent.ACTION_EDIT.equals(action)||Intent.ACTION_VIEW.equals(action)) && type != null) {
        	
        	Uri uri = intent.getData();
        	if ( uri != null ) {
        		ContentResolver cr = getContentResolver();
        		try {
					mm = cr.openInputStream(uri);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
  
      
        } else {
	
	        
	        
	        
	        Log.e(TAG, "Before opening resource");
	        
	    	mm = this.getResources().openRawResource(R.raw.example);
	    	
        }
        
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
		
		
		
		Log.e(TAG, "After opening resource");

    	currentParent = document.getDocumentElement();
		listChildren();
        
    }

    
    public void goUp(View view) {
    	up();
    }
    
    public void up() {
		if ( parents.size() > 0 ) {
			currentParent = parents.pop();
			listChildren();
		} 
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
    	
    	switch (item.getItemId()) {
    	case MENU_UP:
    		up();
    	case android.R.id.home:
        	if ( parents.size() > 0 ) {
    			currentParent = parents.pop();
    			listChildren();
        	} else {
        		finish();
        	}
    	}
    	return true;
    }
    
    @Override
    public void onBackPressed() {
    	up();
    }
    
    private void listChildren() {
    	

    	Log.e(TAG, "listChildren called");
    	
    	str_currentListedNodes = new ArrayList<String>();
    	currentListedNodes = new ArrayList<Node>();
    	
    	NodeList tmp_children = currentParent.getChildNodes();
    	int tmp_children_n = tmp_children.getLength();
    	
    	for (int i = 0; i < tmp_children_n; i++) {
    		Node tmp_n = tmp_children.item(i);
    		
    		if ( tmp_n.getNodeType() == Node.ELEMENT_NODE ) {
    			Element tmp_e = (Element)tmp_n;

    			if ( tmp_e.getTagName().equals("node") ) {
	    			str_currentListedNodes.add(tmp_e.getAttribute("TEXT"));
	    			currentListedNodes.add(tmp_e);
    			}
    		}
    					
		}
    	
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, str_currentListedNodes);
    	listView.setAdapter(adapter); 
    	listView.setOnItemClickListener(this);


    	  	
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	
		parents.push(currentParent);
		currentParent = currentListedNodes.get(position);
		
		
		listChildren();
		
		
	}

}