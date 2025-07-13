package ch.benediktkoeppel.code.droidplane;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

/**
 * A column of MindmapNodes, i.e. one level in the mind map. It extends LinearLayout, and then embeds a ListView.
 * This is because we want to have a fine border around the ListView and we can only achieve this by having it
 * wrapped in a LinearLayout with a padding.
 */
public class NodeColumn extends LinearLayout implements OnCreateContextMenuListener {

    /**
     * The parent node (i.e. the node that is parent to everything we display in this column)
     */
    private final MindmapNode parent;

    /**
     * The list of all MindmapNodeLayouts which we display in this column
     */
    private ArrayList<MindmapNodeLayout> mindmapNodeLayouts;

    /**
     * The adapter for this column
     */
    private MindmapNodeAdapter adapter;

    /**
     * The actual ListView that we'll display
     */
    private ListView listView;

    /**
     * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always
     * throw a IllegalArgumentException.
     *
     * @param context
     * @deprecated
     */
    public NodeColumn(Context context) {

        super(context);
        parent = null;
        if (!isInEditMode()) {
            throw new IllegalArgumentException(
                    "The constructor public NodeColumn(Context context) may only be called by graphical layout tools," +
					" i.e. when View#isInEditMode() is true. In production, use the constructor public NodeColumn" +
					"(Context context, Node parent).");
        }
    }

    /**
     * Creates a new NodeColumn for a parent node. This NodeColumn is a LinearLayout, which contains a ListView,
     * which displays all child nodes of the parent node.
     *
     * @param context
     * @param parent
     */
    public NodeColumn(Context context, MindmapNode parent) {

        super(context);

        // extract all <node.../> elements from the parent node, create layouts, and add them to the mindmapNodes list
        mindmapNodeLayouts = new ArrayList<>();
        List<MindmapNode> mindmapNodes = parent.getChildNodes();
        for (MindmapNode mindmapNode : mindmapNodes) {
            mindmapNodeLayouts.add(new MindmapNodeLayout(context, mindmapNode));
        }

        // store the parent node
        this.parent = parent;


        // define the layout of this LinearView
        int linearViewHeight = LayoutParams.MATCH_PARENT;
        int linearViewWidth = getOptimalColumnWidth(context);
        LinearLayout.LayoutParams linearViewLayout = new LinearLayout.LayoutParams(linearViewWidth, linearViewHeight);
        setLayoutParams(linearViewLayout);
        setPadding(0, 0, 1, 0);
        setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));


        // create a ListView
        listView = new ListView(context);
        listView.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(listView, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // define the layout of the listView
        // should be as high as the parent (i.e. full screen height)
        int listViewHeight = LayoutParams.MATCH_PARENT;
        int listViewWidth = LayoutParams.MATCH_PARENT;
        ViewGroup.LayoutParams listViewLayout = new ViewGroup.LayoutParams(listViewWidth, listViewHeight);
        listView.setLayoutParams(listViewLayout);
        listView.setBackgroundColor(context.getResources().getColor(android.R.color.background_light));


        // create adapter (i.e. data provider) for the column
        adapter = new MindmapNodeAdapter(getContext(), R.layout.mindmap_node_list_item, mindmapNodeLayouts);

        // add the content adapter
        listView.setAdapter(adapter);

        // call NodeColumn's onCreateContextMenu when a context menu for one of the listView items should be generated
        listView.setOnCreateContextMenuListener(this);

        // add the listView to the linearView
        this.addView(listView);
    }

    /**
     * Sets the width of this column to columnWidth
     *
     * @param columnWidth width of the column
     */
    private void setWidth(int columnWidth) {

        Log.d(MainApplication.TAG, "Setting column width to " + columnWidth);

        ViewGroup.LayoutParams listViewParam = this.getLayoutParams();
        listViewParam.width = columnWidth;
        this.setLayoutParams(listViewParam);
    }

    /**
     * Deselects all nodes of this column
     */
    public void deselectAllNodes() {
        // deselect all nodes
        for (MindmapNodeLayout mindmapNodeLayout : mindmapNodeLayouts) {
            MindmapNode mindmapNode = mindmapNodeLayout.getMindmapNode();
            mindmapNode.setSelected(false);
        }

        // then notify about the GUI change
        adapter.notifyDataSetChanged();
    }

    /**
     * Returns the parent node of this column.
     *
     * @return the parent node of this colunn
     */
    public MindmapNode getParentNode() {

        return parent;
    }

    /**
     * Calculates the column width which this column should have
     *
     * @return
     */
    private static int getOptimalColumnWidth(Context context) {

        // and R.integer.horizontally_visible_panes defines how many columns should be visible side by side
        // so we need 1/(horizontally_visible_panes) * displayWidth as column width
        Resources resources = context.getResources();
        int horizontallyVisiblePanes = resources.getInteger(R.integer.horizontally_visible_panes);
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int displayWidth;

        // get the Display width
        Point displaySize = new Point();
        display.getSize(displaySize);
        displayWidth = displaySize.x;
        int columnWidth = displayWidth / horizontallyVisiblePanes;

        Log.d(MainApplication.TAG, "Calculated column width = " + columnWidth);

        return columnWidth;
    }

    /**
     * Resizes the column to its optimal column width
     */
    public void resizeColumnWidth(Context context) {

        setWidth(getOptimalColumnWidth(context));
    }

    /**
     * Fetches the MindmapNodeLayout at the given position
     *
     * @param position the position from which the MindmapNodeLayout should be returned
     * @return MindmapNodeLayout
     */
    public MindmapNodeLayout getNodeAtPosition(int position) {

        return mindmapNodeLayouts.get(position);
    }
    
    private int getPositionOf(MindmapNode node) {
        for (int i = 0; i < mindmapNodeLayouts.size(); i++) {
            if (mindmapNodeLayouts.get(i).getMindmapNode() == node) {
                return i;
            }
        }
        return 0;
    }
    
    void scrollTo(MindmapNode node) {
        post(() -> listView.smoothScrollToPosition(getPositionOf(node)));
    }

    /**
     * Sets the color on the node at the specified position
     *
     * @param position
     */
    public void setItemColor(int position) {

        // deselect all nodes
        for (int i = 0; i < mindmapNodeLayouts.size(); i++) {
            mindmapNodeLayouts.get(i).setSelected(false);
        }

        // then select node at position
        mindmapNodeLayouts.get(position).setSelected(true);

        // then notify about the GUI change
        adapter.notifyDataSetChanged();
    }

    /**
     * Simply a wrapper for ListView's setOnItemClickListener. Technically, the NodeColumn (which is a LinearView)
     * does not generate OnItemClick Events, but it's child view (the ListView) does. But it's simpler if the outside
     * world does not have to care about that detail, so we implement setOnItemClickListener and just forward the
     * listener to the actual ListView.
     *
     * @param listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {

        listView.setOnItemClickListener(listener);
    }


    /* (non-Javadoc)
     *
     * This is called when a context menu for one of the list items is generated.
     *
     * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(android.view.ContextMenu, android.view
     * .View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu);

        // get the menu information
        AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;

        // get the clicked node
        MindmapNodeLayout clickedNode = mindmapNodeLayouts.get(contextMenuInfo.position);

        // forward the event to the clicked node
        clickedNode.onCreateContextMenu(menu, v, menuInfo);

    }

    /**
     * Returns the ListView of this NodeColumn
     * @return the ListView of this NodeColumn
     */
    public ListView getListView() {

        return listView;
    }
}


/**
 * The MindmapNodeAdapter is the data provider for the NodeColumn (respectively its ListView).
 */
class MindmapNodeAdapter extends ArrayAdapter<MindmapNodeLayout> {

    private final List<MindmapNodeLayout> mindmapNodeLayouts;

    public MindmapNodeAdapter(Context context, int textViewResourceId, ArrayList<MindmapNodeLayout> mindmapNodeLayouts) {

        super(context, textViewResourceId, mindmapNodeLayouts);
        this.mindmapNodeLayouts = mindmapNodeLayouts;
    }

    /* (non-Javadoc)
     * getView is responsible to return a view for each individual element in the ListView
     * @param int position: the position in the mindmapNodes array, for which we need to generate a view
     * @param View convertView: the view we should recycle
     * @param ViewGroup parent: not sure, is this the NodeColumn for which the Adapter is generating views?
     * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @NonNull
    @SuppressLint("InlinedApi")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        // when convertView != null, we should take the convertView and update it appropriately. Android is
        // optimizing the performance and thus recycling GUI elements. However, we don't want to recycle anything,
        // because these are genuine Mindmap nodes. Recycling the view here would show one node twice in the tree,
        // while leaving out the actual node we should display.

        MindmapNodeLayout view = mindmapNodeLayouts.get(position);

        // tell the node to refresh it's view
        view.refreshView();

        return view;
    }
}
