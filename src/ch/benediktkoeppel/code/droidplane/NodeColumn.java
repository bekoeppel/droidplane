package ch.benediktkoeppel.code.droidplane;

import org.w3c.dom.Node;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * A column of MindmapNodes, i.e. one level in the mind map.
 */
public class NodeColumn extends ListView {
	
	public Node parent;

	public NodeColumn(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	/**
	 * GUI Helper to detach this ListView from its parent
	 */
	public void removeFromParent() {
		if ( this.getParent() != null ) {
			((ViewGroup)this.getParent()).removeView(this);
		}
	}
	
	public void setWidth(int columnWidth) {
		ViewGroup.LayoutParams listViewParam = this.getLayoutParams();
		listViewParam.width = columnWidth;
		this.setLayoutParams(listViewParam);
	}
	

}
