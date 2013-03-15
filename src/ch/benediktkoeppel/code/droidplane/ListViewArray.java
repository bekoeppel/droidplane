package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import android.widget.ListAdapter;
import android.widget.ListView;


public class ListViewArray {
	
	/**
	 * listViews holds all visible ListViews. Once it is initialized, it can't
	 * be modified. These are the visible ListViews.
	 */
	private final ArrayList<ListView> listViews;
	
	
	/**
	 * TODO: the fact that I don't know how to write the comment for
	 * listViewContents makes it very clear that I have no clue what I'm doing.
	 * 
	 * listViewContents holds the content for each ListView. ListViewContents
	 * can be associated with any of the ListViews, and they will be moved
	 * around when we add or remove stuff.
	 */
	private ArrayList<ListViewContent> listViewContents;
	
	public ListViewArray(ArrayList<ListView> listViews) {
		this.listViews = listViews;
		this.listViewContents = new ArrayList<ListViewContent>();
	}
	
	public void wipeListViewsRightOf(int index) {
		for (int i = index+1; i < listViews.size(); i++) {
			listViews.get(i).setAdapter(null);
		}
	}
	
	public void wipeAll() {
		for (int i = 0; i < listViews.size(); i++) {
			listViews.get(i).setAdapter(null);
		}
	}
	
	public void removeOne() {
		
		
	}
	
	public void addItem(ListViewContent item) {
		
		// if the last listViews already has an adapter, we have to shift everything
		if ( listViews.get(listViews.size()-1).getAdapter() != null ) {
			for (int i = 1; i < listViews.size(); i++) {
				listViews.get(i-1).setAdapter(listViews.get(i).getAdapter());
			}
		}
		
		// then add the adapter to the first free listView
		for (int i = 0; i < listViews.size(); i++) {
			if ( listViews.get(i).getAdapter() == null ) {
				listViews.get(i).setAdapter(item.getAdapter());
				break;
			}
		}
	}
	
}

