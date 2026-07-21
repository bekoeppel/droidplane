package ch.benediktkoeppel.code.droidplane.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.MainApplication;
import ch.benediktkoeppel.code.droidplane.R;
import ch.benediktkoeppel.code.droidplane.model.MindmapNode;


/**
 * A MindmapNodeLayout is the UI (layout) part of a MindmapNode.
 */
public class MindmapNodeLayout extends LinearLayout {

    public static final int CONTEXT_MENU_NORMAL_GROUP_ID = 0;
    public static final int CONTEXT_MENU_ARROWLINK_GROUP_ID = 1;

    /**
     * The MindmapNode, to which this layout belongs
     */
    private final MindmapNode mindmapNode;

    /**
     * The Android resource IDs of the icon
     */
    private List<Integer> iconResourceIds;

    /**
     * Whether the mindmap_node_list_item layout was already inflated into this view
     */
    private boolean isInflated;

    /**
     * How many icons and rich text contents the node had when we last calculated iconResourceIds. While the mindmap
     * is loading these only ever grow, so this tells us whether we have to calculate them again.
     */
    private int renderedIconNamesCount = -1;
    private int renderedRichTextContentsCount = -1;

    /**
     * Resources.getIdentifier() is a slow, string based lookup, and the same handful of icons appears over and over
     * in a mindmap, so we resolve each drawable name only once. Only ever accessed from the UI thread.
     */
    private static final Map<String, Integer> drawableIdsByName = new HashMap<>();

    /**
     * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always
     * throw a IllegalArgumentException.
     *
     * @param context
     * @deprecated
     */
    public MindmapNodeLayout(Context context) {

        super(context);
        mindmapNode = null;
        if (!isInEditMode()) {
            throw new IllegalArgumentException(
                    "The constructor public MindmapNode(Context context) may only be called by graphical layout " +
					"tools, i.e. when View#isInEditMode() is true. In production, use the constructor public " +
					"MindmapNode(Context context, Node node).");
        }
    }

    public MindmapNodeLayout(Context context, MindmapNode mindmapNode) {

        super(context);

        this.mindmapNode = mindmapNode;
        mindmapNode.subscribeNodeStyleChanged(this);

        updateIconResourceIds();
    }

    /**
     * (Re-)calculates the Android resource IDs of the icons of this node. This has to happen every time the view is
     * refreshed: while the mindmap is still streaming in, a node is created (and this layout with it) before its
     * &lt;icon&gt; and &lt;richcontent&gt; children were parsed. So the icons of a node are typically only known
     * after this layout was created.
     */
    private void updateIconResourceIds() {

        Context context = getContext();
        List<String> iconNames = mindmapNode.getIconNames();
        int richTextContentsCount = mindmapNode.getRichTextContents().size();

        // this runs on every single bind of every visible row, so don't calculate anything if nothing was added to
        // the node since we last did
        if (iconResourceIds != null
                && iconNames.size() == renderedIconNamesCount
                && richTextContentsCount == renderedRichTextContentsCount) {
            return;
        }

        List<Integer> newIconResourceIds = new ArrayList<>();
        for (String iconName : iconNames) {
            int iconResourceId = getDrawableId(getDrawableNameFromMindmapIcon(iconName, context), context);

            // getIdentifier returns 0 if we don't ship a drawable for this mindmap icon. Skip it, otherwise it would
            // take up the space of an icon that we can actually display.
            if (iconResourceId != 0) {
                newIconResourceIds.add(iconResourceId);
            }
        }

        // set link icon if node has a link. The link icon will be the first icon shown
        if (mindmapNode.getLink() != null) {
            newIconResourceIds.add(0, getDrawableId("link", context));
        }

        // set the rich text icon if it has
        if (richTextContentsCount > 0) {
            newIconResourceIds.add(0, getDrawableId("richtext", context));
        }

        iconResourceIds = newIconResourceIds;
        renderedIconNamesCount = iconNames.size();
        renderedRichTextContentsCount = richTextContentsCount;
    }

    /**
     * Resolves a drawable name to its resource ID, remembering the result
     *
     * @return the resource ID, or 0 if we don't have a drawable with that name
     */
    private static int getDrawableId(String drawableName, Context context) {

        Integer cachedId = drawableIdsByName.get(drawableName);
        if (cachedId != null) {
            return cachedId;
        }

        Resources resources = context.getResources();
        int drawableId = resources.getIdentifier("@drawable/" + drawableName, "id", context.getPackageName());
        drawableIdsByName.put(drawableName, drawableId);
        return drawableId;
    }

    @SuppressLint("InlinedApi")
    public void refreshView() {

        // inflate the layout if we haven't done so yet. Inflating it more than once would stack multiple copies of
        // the layout into this view, and all further updates would only ever reach the first copy.
        if (!isInflated) {
            inflate(getContext(), R.layout.mindmap_node_list_item, this);
            isInflated = true;
        }

        // the node might have received icons or rich text content since we last drew it
        updateIconResourceIds();

        // the mindmap_node_list_item consists of a ImageView (icon), a TextView (node text), and another TextView
		// ("+" button)
        ImageView icon0View = findViewById(R.id.icon0);
        ImageView icon1View = findViewById(R.id.icon1);

        if (iconResourceIds.size() > 0) {
            icon0View.setImageResource(iconResourceIds.get(0));
            icon0View.setVisibility(VISIBLE);

        } else {

            // don't waste space, there are no icons
            icon0View.setVisibility(GONE);
        }

        // second icon
        if (iconResourceIds.size() > 1) {
            icon1View.setImageResource(iconResourceIds.get(1));
            icon1View.setVisibility(VISIBLE);

        } else {

            // no second icon, don't waste space
            icon1View.setVisibility(GONE);
        }

        TextView textView = findViewById(R.id.label);
        textView.setTextColor(getContext().getResources().getColor(android.R.color.primary_text_light));
        // while the mindmap is still loading, a node can have no text yet (e.g. if its rich text content was not
        // parsed yet). We'll be called again once the content arrives.
        String nodeText = mindmapNode.getText();
        SpannableString spannableString = new SpannableString(nodeText != null ? nodeText : "");
        if (mindmapNode.isBold()) {
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, spannableString.length(), 0);
        }
        if (mindmapNode.isItalic()) {
            spannableString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannableString.length(), 0);
        }
        textView.setText(spannableString);

        ImageView expandable = findViewById(R.id.expandable);
        if (mindmapNode.isExpandable()) {
            if (mindmapNode.getIsSelected()) {
                expandable.setImageResource(R.drawable.minus_alt);
            } else {
                expandable.setImageResource(R.drawable.plus_alt);
            }
        } else {

            // the node might have had children when we last drew it (this view is re-used while the mindmap loads)
            expandable.setImageDrawable(null);
        }

        // if the node is selected, give it a special background
        if (mindmapNode.getIsSelected()) {
            int backgroundColor;

            // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
            backgroundColor = getContext().getResources().getColor(android.R.color.holo_blue_bright);

            setBackgroundColor(backgroundColor);
        } else {
            setBackgroundColor(0);
        }

        // set the layout parameter
        // TODO: this should not be necessary. The problem is that the inflate
        // (in the constructor) loads the XML as child of this LinearView, so
        // the MindmapNode-LinearView wraps the root LinearView from the
        // mindmap_node_list_item XML file.
        setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT
        ));
        setGravity(Gravity.LEFT | Gravity.CENTER);
    }

    /**
     * Mindmap icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]. This
     * method translates the Mindmap icon names to Android resource names.
     *
     * @param iconName the icon name as it is specified in the XML
     * @return the name of the corresponding android resource icon
     */
    private String getDrawableNameFromMindmapIcon(String iconName, Context context) {

        Locale locale = context.getResources().getConfiguration().locale;
        String name = "icon_" + iconName.toLowerCase(locale).replaceAll("[^a-z0-9_.]", "_");
        name = name.replaceAll("_$", "");

        Log.d(MainApplication.TAG, "converted icon name " + iconName + " to " + name);

        return name;
    }

    /**
     * The NodeColumn forwards the CreateContextMenu event to the appropriate MindmapNode, which can then generate
     * the context menu as it likes. Note that the MindmapNode itself is not registered as the listener for such
     * events per se, because the NodeColumn first has to decide for which MindmapNode the event applies.
     *
     * @param menu
     * @param v
     * @param menuInfo
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

        // build the menu
        menu.setHeaderTitle(mindmapNode.getText());
        if (iconResourceIds.size() > 0) {
            menu.setHeaderIcon(iconResourceIds.get(0));
        }

        // allow copying the node text
        menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextcopy, 0, R.string.copynodetext);

        // add menu to open link, if the node has a hyperlink
        if (mindmapNode.getLink() != null) {
            menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextopenlink, 0, R.string.openlink);
        }

        // add menu to show rich text, if the node has
        if (mindmapNode.getRichTextContents() != null && !mindmapNode.getRichTextContents().isEmpty()) {
            menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.openrichtext, 0, R.string.openrichtext);
        }

        // add menu for each arrow link
        for (MindmapNode linkedNode : mindmapNode.getArrowLinks()) {
            menu.add(CONTEXT_MENU_ARROWLINK_GROUP_ID, linkedNode.getNumericId(), 0, linkedNode.getText());
        }
    }

    /**
     * Opens the link of this node (if any)
     */
    public void openLink(MainActivity mainActivity) {

        // TODO: if link is internal, substring ID

        Log.d(MainApplication.TAG, "Opening link (to string): " + mindmapNode.getLink().toString());
        Log.d(MainApplication.TAG, "Opening link (fragment, everything after '#'): " + mindmapNode.getLink().getFragment());

        // if the link has a "#ID123", it's an internal link within the document
        if (mindmapNode.getLink().getFragment() != null && mindmapNode.getLink().getFragment().startsWith("ID")) {
            openInternalFragmentLink(mainActivity);

        }

        // otherwise, we try to open it as intent
        else {
            openIntentLink(mainActivity);
        }
    }

    /**
     * Open this node's link as internal fragment
     */
    private void openInternalFragmentLink(MainActivity mainActivity) {

        // internal link, so this.link is of the form "#ID_123234534" this.link.getFragment() should give everything
        // after the "#" it is null if there is no "#", which should be the case for all other links
        String fragment = mindmapNode.getLink().getFragment();

        MindmapNode linkedInternal = mindmapNode.getMindmap().getNodeByID(fragment);

        if (linkedInternal != null) {
            Log.d(MainApplication.TAG, "Opening internal node, " + linkedInternal + ", with ID: " + fragment);

            // the internal linked node might be anywhere in the mindmap, i.e. on a completely separate branch than
            // we are on currently. We need to go to the Top, and then descend into the mindmap to reach the right
            // point
            HorizontalMindmapView mindmapView = mainActivity.getHorizontalMindmapView();
            mindmapView.downTo(mainActivity, linkedInternal, true);

        } else {
            Toast.makeText(getContext(),
                    "This internal link to ID " + fragment + " seems to be broken.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Open this node's link as intent
     */
    private void openIntentLink(MainActivity mainActivity) {

        // try opening the link normally with an intent
        try {
            Intent openUriIntent = new Intent(Intent.ACTION_VIEW);
            openUriIntent.setData(mindmapNode.getLink());
            mainActivity.startActivity(openUriIntent);
            return;
        } catch (ActivityNotFoundException e) {
            Log.d(MainApplication.TAG, "ActivityNotFoundException when opening link as normal intent");
        }

        // try to open as relative file
        try {
            // get path of mindmap file
            String fileName;
            if (mindmapNode.getLink().getPath().startsWith("/")) {
                // absolute filename
                fileName = mindmapNode.getLink().getPath();
            } else {

                // link is relative to mindmap file
                String mindmapPath = mindmapNode.getMindmap().getUri().getPath();
                Log.d(MainApplication.TAG, "Mindmap path " + mindmapPath);
                String mindmapDirectoryPath = mindmapPath.substring(0, mindmapPath.lastIndexOf("/"));
                Log.d(MainApplication.TAG, "Mindmap directory path " + mindmapDirectoryPath);
                fileName = mindmapDirectoryPath + "/" + mindmapNode.getLink().getPath();

            }
            File file = new File(fileName);
            if (!file.exists()) {
                Toast.makeText(getContext(), "File " + fileName + " does not exist.", Toast.LENGTH_SHORT).show();
                Log.d(MainApplication.TAG, "File " + fileName + " does not exist.");
                return;
            }
            if (!file.canRead()) {
                Toast.makeText(getContext(), "Can not read file " + fileName + ".", Toast.LENGTH_SHORT).show();
                Log.d(MainApplication.TAG, "Can not read file " + fileName + ".");
                return;
            }
            Log.d(MainApplication.TAG, "Opening file " + Uri.fromFile(file));
            // http://stackoverflow.com/a/3571239/1067124
            String extension = "";
            int i = fileName.lastIndexOf('.');
            int p = fileName.lastIndexOf('/');
            if (i > p) {
                extension = fileName.substring(i + 1);
            }
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), mime);
            mainActivity.startActivity(intent);
        } catch (Exception e1) {
            Toast.makeText(getContext(), "No application found to open " + mindmapNode.getLink(), Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
        }
    }

    public MindmapNode getMindmapNode() {

        return mindmapNode;
    }

    public void openRichText(MainActivity mainActivity) {

        String richTextContent = getMindmapNode().getRichTextContents().get(0);
        Intent intent = new Intent(mainActivity, RichTextViewActivity.class);
        intent.putExtra("richTextContent", richTextContent);
        mainActivity.startActivity(intent);

    }

    public void notifyNodeStyleChanged() {
        this.refreshView();
    }
}
