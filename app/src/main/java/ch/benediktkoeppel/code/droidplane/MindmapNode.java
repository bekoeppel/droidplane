package ch.benediktkoeppel.code.droidplane;

import android.net.Uri;
import android.text.Html;
import android.util.Log;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
public class MindmapNode {

    /**
     * The ID of the node (ID attribute)
     */
    private final String id;

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
    private final String richTextContent;

    /**
     * Bold style
     */
    private final boolean isBold;

    /**
     * Italic style
     */
    private final boolean isItalic;

    /**
     * The names of the icon
     */
    private final List<String> iconNames;

    /**
     * Whether the node is expandable, i.e. whether it has child nodes
     */
    private boolean isExpandable;

    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    private final Uri link;

    /**
     * The XML DOM node from which this MindMapNode is derived
     */
    private final Node node;

    /**
     * Whether the node is selected or not, will be set after it was clicked by the user
     */
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
     * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". Throws a
     * {@link ClassCastException} if the Node can not be converted to a MindmapNode.
     *
     * @param node
     */
    public MindmapNode(Node node, MindmapNode parentNode, Mindmap mindmap) {

        this.mindmap = mindmap;

        // store the parentNode
        this.parentNode = parentNode;

        // convert the XML Node to a XML Element
        Element tmpElement;
        if (isMindmapNode(node)) {
            tmpElement = (Element)node;
        } else {
            throw new ClassCastException("Can not convert Node to MindmapNode");
        }

        // store the Node
        this.node = node;

        // extract the ID of the node
        id = tmpElement.getAttribute("ID");


        // extract the string (TEXT attribute) of the nodes
        String text = tmpElement.getAttribute("TEXT");

        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
        // (TYPE="NODE"), and for "Notes" (TYPE="NOTE").
        String richTextContent = null;
        // find 'richcontent TYPE="NODE"' subnode, which will contain the rich text content
        NodeList richtextNodeList = tmpElement.getChildNodes();
        for (int i = 0; i < richtextNodeList.getLength(); i++) {
            Node n = richtextNodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("richcontent")) {
                Element richcontentElement = (Element)n;
                String typeAttribute = richcontentElement.getAttribute("TYPE");
                if (typeAttribute.equals("NODE") || typeAttribute.equals("NOTE")) {

                    // extract the whole rich text (XML), to show in a WebView activity
                    try {
                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
                        transformer.transform(new DOMSource(richtextNodeList.item(0)), new StreamResult(boas));
                        richTextContent = boas.toString();
                    } catch (TransformerException e) {
                        e.printStackTrace();
                    }

                    // if the node has no text itself, then convert the rich text content to a text
                    if (text == null || text.equals("")) {
                        // convert the content (text only) into a string, to show in the normal list view
                        text = Html.fromHtml(richcontentElement.getTextContent()).toString();
                    }
                }
            }
        }
        this.richTextContent = richTextContent;
        this.text = text;


        // extract styles
        NodeList styleNodeList = tmpElement.getChildNodes();
        boolean isBold = false;
        boolean isItalic = false;
        for (int i = 0; i < styleNodeList.getLength(); i++) {
            Node n = styleNodeList.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("font")) {
                Element fontElement = (Element)n;
                if (fontElement.hasAttribute("BOLD") && fontElement.getAttribute("BOLD").equals("true")) {
                    Log.d(MainApplication.TAG, "Found bold node");
                    isBold = true;
                }
                if (fontElement.hasAttribute("ITALIC") && fontElement.getAttribute("ITALIC").equals("true")) {
                    isItalic = true;
                }
            }
        }
        this.isBold = isBold;
        this.isItalic = isItalic;

        // extract icons
        iconNames = getIcons();

        // find out if it has sub nodes
        isExpandable = (getNumChildMindmapNodes() > 0);

        // extract link
        String linkAttribute = tmpElement.getAttribute("LINK");
        if (!linkAttribute.equals("")) {
            link = Uri.parse(linkAttribute);
        } else {
            link = null;
        }

        // get cloned node's info
        treeIdAttribute = tmpElement.getAttribute("TREE_ID");

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

    /**
     * Checks whether a given node can be converted to a Mindmap node, i.e. whether it has type ELEMENT_NODE and tag
     * "node"
     *
     * @param node
     * @return
     */
    private static boolean isMindmapNode(Node node) {

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element)node;

            return element.getTagName().equals("node");
        }
        return false;
    }


    /**
     * Extracts the list of icons from a node and returns the names of the icons as ArrayList.
     *
     * @return list of names of the icons
     */
    private ArrayList<String> getIcons() {

        ArrayList<String> iconsNames = new ArrayList<>();

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {

            Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element)n;

                if (e.getTagName().equals("icon") && e.hasAttribute("BUILTIN")) {
                    iconsNames.add(e.getAttribute("BUILTIN"));
                }
            }
        }

        return iconsNames;
    }

    /**
     * Returns the number of child Mindmap nodes
     *
     * @return
     */
    public int getNumChildMindmapNodes() {

        int numMindmapNodes = 0;

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {

            Node n = childNodes.item(i);
            if (isMindmapNode(n)) {
                numMindmapNodes++;
            }
        }

        return numMindmapNodes;
    }


    /**
     * Generates and returns the child nodes of this MindmapNode. getChildNodes() does lazy loading, i.e. it
     * generates the child nodes on demand and stores them in childMindmapNodes.
     *
     * @return ArrayList of this MindmapNode's child nodes
     */
    public List<MindmapNode> getChildNodes() {

        // if we haven't loaded the childMindmapNodes before
        if (childMindmapNodes == null) {

            // fetch all child DOM Nodes, convert them to MindmapNodes
            childMindmapNodes = new ArrayList<>();
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node tmpNode = childNodes.item(i);

                if (isMindmapNode(tmpNode)) {
                    MindmapNode mindmapNode = new MindmapNode(tmpNode, this, mindmap);
                    childMindmapNodes.add(mindmapNode);
                }
            }
            return childMindmapNodes;
        }

        // we already did that before, so return the previous result
        else {
            Log.d(MainApplication.TAG, "Returning cached childMindmapNodes");
            return childMindmapNodes;
        }
    }

    public List<String> getIconNames() {

        return iconNames;
    }

    public String getText() {

        // if this is a cloned node, get the text from the original node
        if (treeIdAttribute != null) {
            MindmapNode linkedNode = mindmap.getNodeByID(treeIdAttribute);
            if (linkedNode != null) {
                return linkedNode.getText();
            }
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

        return isExpandable;
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

    public String getRichTextContent() {

        return richTextContent;
    }
}
