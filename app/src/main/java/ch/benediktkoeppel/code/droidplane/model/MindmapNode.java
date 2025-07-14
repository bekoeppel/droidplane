package ch.benediktkoeppel.code.droidplane.model;

import android.net.Uri;
import android.text.Html;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
        this.childMindmapNodes = new ArrayList<>();
        this.richTextContents = new ArrayList<>();
        isBold = false;
        isItalic = false;
        iconNames = new ArrayList<>();
        this.link = link;
        this.treeIdAttribute = treeIdAttribute;
        arrowLinkDestinationIds = new ArrayList<>();
        //node = null;
    }

    /**
     * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". Throws a
     * {@link ClassCastException} if the Node can not be converted to a MindmapNode.
     *
     * @param node
     */
//    public MindmapNode(Node node, MindmapNode parentNode, Mindmap mindmap) {
//
//        this.mindmap = mindmap;
//
//        // store the parentNode
//        this.parentNode = parentNode;
//
//        // convert the XML Node to a XML Element
//        Element tmpElement;
//        if (isMindmapNode(node)) {
//            tmpElement = (Element)node;
//        } else {
//            throw new ClassCastException("Can not convert Node to MindmapNode");
//        }
//
//        // store the Node
//        this.node = node;
//
//        // extract the ID of the node
//        id = tmpElement.getAttribute("ID");
//
//        try {
//            numericId = Integer.parseInt(id.replaceAll("\\D+", ""));
//        } catch (NumberFormatException e) {
//            numericId = id.hashCode();
//        }
//
//
//        // extract the string (TEXT attribute) of the nodes
//        String text = tmpElement.getAttribute("TEXT");
//
//        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
//        // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").
//        String richTextContent = null;
//        // find 'richcontent TYPE="NODE"' subnode, which will contain the rich text content
//        NodeList richtextNodeList = tmpElement.getChildNodes();
//        for (int i = 0; i < richtextNodeList.getLength(); i++) {
//            Node n = richtextNodeList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("richcontent")) {
//                Element richcontentElement = (Element)n;
//                String typeAttribute = richcontentElement.getAttribute("TYPE");
//                if (typeAttribute.equals("NODE") || typeAttribute.equals("NOTE") || typeAttribute.equals("DETAILS")) {
//
//                    // extract the whole rich text (XML), to show in a WebView activity
//                    try {
//                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
//                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
//                        transformer.transform(new DOMSource(richtextNodeList.item(0)), new StreamResult(boas));
//                        richTextContent = boas.toString();
//                    } catch (TransformerException e) {
//                        e.printStackTrace();
//                    }
//
//                    // if the node has no text itself, then convert the rich text content to a text
//                    if (text == null || text.equals("")) {
//                        // convert the content (text only) into a string, to show in the normal list view
//                        text = Html.fromHtml(richcontentElement.getTextContent()).toString();
//                    }
//                }
//            }
//        }
//        this.richTextContent = richTextContent;
//        this.text = text;
//
//
//        // extract styles
//        NodeList styleNodeList = tmpElement.getChildNodes();
//        boolean isBold = false;
//        boolean isItalic = false;
//        for (int i = 0; i < styleNodeList.getLength(); i++) {
//            Node n = styleNodeList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("font")) {
//                Element fontElement = (Element)n;
//                if (fontElement.hasAttribute("BOLD") && fontElement.getAttribute("BOLD").equals("true")) {
//                    Log.d(MainApplication.TAG, "Found bold node");
//                    isBold = true;
//                }
//                if (fontElement.hasAttribute("ITALIC") && fontElement.getAttribute("ITALIC").equals("true")) {
//                    isItalic = true;
//                }
//            }
//        }
//        this.isBold = isBold;
//        this.isItalic = isItalic;
//
//        // extract icons
//        iconNames = getIcons();
//
//        // find out if it has sub nodes
//        // TODO: this should just go into a getter
//        isExpandable = (getNumChildMindmapNodes() > 0);
//
//        // extract link
//        String linkAttribute = tmpElement.getAttribute("LINK");
//        if (!linkAttribute.equals("")) {
//            link = Uri.parse(linkAttribute);
//        } else {
//            link = null;
//        }
//
//        // get cloned node's info
//        treeIdAttribute = tmpElement.getAttribute("TREE_ID");
//
//        // get arrow link destinations
//        arrowLinkDestinationIds = new ArrayList<>();
//        arrowLinkDestinationNodes = new ArrayList<>();
//        arrowLinkIncomingNodes = new ArrayList<>();
//        NodeList arrowlinkList = tmpElement.getChildNodes();
//        for (int i = 0; i< arrowlinkList.getLength(); i++) {
//            Node n = arrowlinkList.item(i);
//            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("arrowlink")) {
//                Element arrowlinkElement = (Element)n;
//                String destinationId = arrowlinkElement.getAttribute("DESTINATION");
//                arrowLinkDestinationIds.add(destinationId);
//            }
//        }
//
//    }


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

        // if this is a cloned node, get the text from the original node
        if (treeIdAttribute != null && !treeIdAttribute.equals("")) {
            // TODO this now fails when loading, because the background indexing is not done yet - so we maybe should mark this as "pending", and put it into a queue, to be updated once the linked node is there
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
        if (this.subscribedNodeColumn != null) {
            subscribedNodeColumn.get().notifyNewMindmapNode(mindmapNode);
        }
    }

    public boolean hasNodeRichContentChangedSubscribers() {
        return this.subscribedMainActivity != null;
    }

    public void notifySubscribersNodeRichContentChanged() {
        if (this.subscribedMainActivity != null) {
            subscribedMainActivity.get().notifyNodeRichContentChanged();
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
        if (this.subscribedNodeLayout != null) {
            this.subscribedNodeLayout.get().notifyNodeStyleChanged();
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
        if (this.getText().toUpperCase().contains(searchString.toUpperCase())) { // TODO: npe here when text is null, because text is a rich text
            res.add(this);
        }
        for (MindmapNode child : childMindmapNodes) {
            res.addAll(child.search(searchString));
        }
        return res;
    }
}
