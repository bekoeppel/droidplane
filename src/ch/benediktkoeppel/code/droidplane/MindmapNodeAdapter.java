package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class MindmapNodeAdapter extends ArrayAdapter<MindmapNode> {
	private ArrayList<MindmapNode> mindmapNodes;
	private LayoutInflater layoutInflater;

	public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNode> mindmapNodes, LayoutInflater layoutInflater) {
		super(context, textViewResourceId, mindmapNodes);
		this.mindmapNodes = mindmapNodes;
		this.layoutInflater = layoutInflater;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = layoutInflater.inflate(R.layout.mindmap_node_list_item, null);
		}

		MindmapNode node = mindmapNodes.get(position);
		if (node != null) {

			TextView text = (TextView) v.findViewById(R.id.label);
			text.setText(node.text);

			ImageView icon = (ImageView) v.findViewById(R.id.icon);
			icon.setImageResource(node.icon_res_id);
			icon.setContentDescription(node.icon_name);

			TextView expandable = (TextView) v.findViewById(R.id.expandable);
			if (node.isExpandable) {
				expandable.setText("+");
			} else {
				expandable.setText("");
			}
		}

		return v;
	}
}