package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import android.widget.ListAdapter;


class ListViewContent {
	ListAdapter listAdapter;
	ArrayList<MindmapNode> mindmapNodes;
	
	public ListViewContent(ListAdapter listAdapter, ArrayList<MindmapNode> mindmapNodes) {
		this.listAdapter = listAdapter;
		this.mindmapNodes = mindmapNodes;
	}
	
	public ListAdapter getAdapter() {
		return listAdapter;
	}
}