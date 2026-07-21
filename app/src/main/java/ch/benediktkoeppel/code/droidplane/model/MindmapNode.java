package ch.benediktkoeppel.code.droidplane.model;

import android.net.Uri;
import android.text.Html;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.view.MindmapNodeLayout;
import ch.benediktkoeppel.code.droidplane.view.NodeColumn;


/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
//@Builder
public class MindmapNode {

    /**
     * The ID of the node (ID attribute)
     */
    private final String id;

    /**
     * The numeric representation of this ID
     */
    private Integer numericId;

    /**
     * The mindmap, in which this node is
     */
    private final Mindmap mindmap;

    /**
     * The Parent MindmapNode
     */
    private final MindmapNode parentNode;

    /**
     * The Text of the node (TEXT attribute).
     */
    private final String text;

    /**
     * The Rich Text content of the node (if any)
     */
    private final List<String> richTextContents;

    /**
     * Bold style
     */
    private boolean isBold;

    /**
     * Italic style
     */
    private boolean isItalic;

    /**
     * The names of the icon
     */
    private final List<String> iconNames;

    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    private final Uri link;

    /**
     * The XML DOM node from which this MindMapNode is derived
     */
    // TODO: MindmapNode should not need this node
    //private final Node node;

    /**
     * Whether the node is selected or not, will be set after it was clicked by the user
     */
    // TODO: this has nothing to do with the model
    private boolean selected;

    /**
     * The list of child MindmapNodes. We support lazy loading.
     */
    private List<MindmapNode> childMindmapNodes;

    /**
     * If the node clones another node, it doesn't have text or richtext, but a TREE_ID
     */
    private final String treeIdAttribute;

    /**
     * List of outgoing arrow links
     */
    private final List<String> arrowLinkDestinationIds;

    /**
     * List of outgoing arrow MindmapNodes
     */
    private List<MindmapNode> arrowLinkDestinationNodes = new ArrayList<>();

    /**
     * List of incoming arrow MindmapNodes
     */
    private List<MindmapNode> arrowLinkIncomingNodes = new ArrayList<>();
    private WeakReference<NodeColumn> subscribedNodeColumn = null;
    private WeakReference<MainActivity> subscribedMainActivity = null;
    private WeakReference<MindmapNodeLayout> subscribedNodeLayout = null;
    private boolean loaded;

    public MindmapNode(Mindmap mindmap, MindmapNode parentNode, String id, int numericId, String text, Uri link, String treeIdAttribute) {
        this.mindmap = mindmap;
        this.parentNode = parentNode;
        this.id = id;
        this.numericId = numericId;
        this.text = text;
        // the mindmap is parsed on a background thread, while the views read these lists on the UI thread. Copy on
        // write lists are cheap enough here (these lists are short and only ever appended to) and give the UI thread
        // a consistent view of them.
        this.childMindmapNodes = new CopyOnWriteArrayList<>();
        this.richTextContents = new CopyOnWriteArrayList<>();
        isBold = false;
        isItalic = false;
        iconNames = new CopyOnWriteArrayList<>();
        this.link = link;
        this.treeIdAttribute = treeIdAttribute;
        arrowLinkDestinationIds = new ArrayList<>();
        //node = null;
    }


    /**
     * Selects or deselects this node
     *
     * @param selected
     */
    public void setSelected(boolean selected) {

        this.selected = selected;
    }

    /**
     * Returns whether this node is selected
     */
    public boolean getIsSelected() {

        return this.selected;
    }



    public List<String> getIconNames() {

        return iconNames;
    }

    // TODO: this should probably live in a view controller, not here
    public String getText() {

        // If this is a cloned node, get the text from the original node. While the document is still being parsed
        // this returns nothing: the node we clone can be anywhere in the document, so it can only be resolved once
        // the whole document is indexed. The loader lets us redraw when that has happened.
        if (treeIdAttribute != null && !treeIdAttribute.equals("")) {
            MindmapNode linkedNode = mindmap.getNodeByID(treeIdAttribute);
            if (linkedNode != null) {
                return linkedNode.getText();
            }
        }

        // if this is a rich text node, get the HTML content instead
        if (this.text == null && this.getRichTextContents() != null && !this.getRichTextContents().isEmpty()) {

            String richTextContent = this.getRichTextContents().get(0);
            return Html.fromHtml(richTextContent).toString();

        }

        return text;
    }

    public boolean isBold() {

        return isBold;
    }

    public boolean isItalic() {

        return isItalic;
    }

    public boolean isExpandable() {

        return !childMindmapNodes.isEmpty();
    }

    public Uri getLink() {

        return link;
    }

    public Mindmap getMindmap() {

        return mindmap;
    }

    public String getId() {

        return id;
    }

    public MindmapNode getParentNode() {

        return parentNode;
    }

    public List<String> getRichTextContents() {

        return richTextContents;
    }

    public void addRichTextContent(String richTextContent) {
        this.richTextContents.add(richTextContent);
    }

    public List<String> getArrowLinkDestinationIds() {

        return arrowLinkDestinationIds;
    }

    public List<MindmapNode> getArrowLinkDestinationNodes() {

        return arrowLinkDestinationNodes;
    }

    public List<MindmapNode> getArrowLinkIncomingNodes() {

        return arrowLinkIncomingNodes;
    }

    public List<MindmapNode> getArrowLinks() {
        ArrayList<MindmapNode> combinedArrowLists = new ArrayList<>();
        combinedArrowLists.addAll(arrowLinkDestinationNodes);
        combinedArrowLists.addAll(arrowLinkIncomingNodes);
        return combinedArrowLists;
    }

    public Integer getNumericId() {

        return numericId;
    }

    public List<MindmapNode> getChildMindmapNodes() {
        return this.childMindmapNodes;
    }

    public void setChildMindmapNodes(List<MindmapNode> childMindmapNodes) {
        this.childMindmapNodes = childMindmapNodes;
    }

    public int getNumChildMindmapNodes() {
        return childMindmapNodes.size();
    }

    public void subscribe(NodeColumn nodeColumn) {
        this.subscribedNodeColumn = new WeakReference<>(nodeColumn);
    }

    public void addChildMindmapNode(MindmapNode newMindmapNode) {
        this.childMindmapNodes.add(newMindmapNode);
    }

    public boolean hasAddedChildMindmapNodeSubscribers() {
        return this.subscribedNodeColumn != null;
    }
    public void notifySubscribersAddedChildMindmapNode(MindmapNode mindmapNode) {
        NodeColumn nodeColumn = this.subscribedNodeColumn != null ? this.subscribedNodeColumn.get() : null;
        if (nodeColumn != null) {
            nodeColumn.notifyNewMindmapNode(mindmapNode);
        }
    }

    public boolean hasNodeRichContentChangedSubscribers() {
        return this.subscribedMainActivity != null;
    }

    public void notifySubscribersNodeRichContentChanged() {
        MainActivity mainActivity = this.subscribedMainActivity != null ? this.subscribedMainActivity.get() : null;
        if (mainActivity != null) {
            mainActivity.notifyNodeRichContentChanged(this);
        }
    }

    // TODO: ugly that MainActivity is needed here. Would be better to introduce an listener interface (same for node column above)
    public void subscribeNodeRichContentChanged(MainActivity mainActivity) {
        this.subscribedMainActivity = new WeakReference<>(mainActivity);
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public void setItalic(boolean italic) {
        isItalic = italic;
    }

    public boolean hasNodeStyleChangedSubscribers() {
        return this.subscribedNodeLayout != null;
    }

    public void subscribeNodeStyleChanged(MindmapNodeLayout nodeLayout) {
        this.subscribedNodeLayout = new WeakReference<>(nodeLayout);
    }

    public void notifySubscribersNodeStyleChanged() {
        MindmapNodeLayout nodeLayout = this.subscribedNodeLayout != null ? this.subscribedNodeLayout.get() : null;
        if (nodeLayout != null) {
            nodeLayout.notifyNodeStyleChanged();
        }
    }

    public void addIconName(String iconName) {
        this.iconNames.add(iconName);
    }

    public void addArrowLinkDestinationId(String destinationId) {
        this.arrowLinkDestinationIds.add(destinationId);
    }

    /** Depth-first search in the core text of the nodes in this sub-tree. */
    // TODO: this doesn't work while mindmap is still loading
    public List<MindmapNode> search(String searchString) {
        var res = new ArrayList<MindmapNode>();

        // a node has no text if it has neither a TEXT attribute nor rich text content (e.g. a node that only holds
        // an icon, or a clone whose original we could not resolve)
        String nodeText = this.getText();
        if (nodeText != null && nodeText.toUpperCase().contains(searchString.toUpperCase())) {
            res.add(this);
        }
        for (MindmapNode child : childMindmapNodes) {
            res.addAll(child.search(searchString));
        }
        return res;
    }
}
