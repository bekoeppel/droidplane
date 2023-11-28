package ch.benediktkoeppel.code.droidplane.model;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import ch.benediktkoeppel.code.droidplane.MainApplication;

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
public class Mindmap extends ViewModel {

    /**
     * The currently loaded Uri
     */
    private Uri uri;

    /**
     * The root node of the document.
     */
    private MindmapNode rootNode;

    /**
     * A map that resolves node IDs to Node objects
     */
    private Map<String, MindmapNode> nodesById;
    private Map<Integer, MindmapNode> nodesByNumericId;

    /**
     * The deepest selected mindmap node
     */
    // TODO: this is not really part of the mindmap model
    private MindmapNode deepestSelectedMindmapNode;

    // whether the mindmap has finished loading
    private boolean isLoaded = false;

    /**
     * Returns the Uri which is currently loaded in document.
     *
     * @return Uri
     */
    public Uri getUri() {

        return this.uri;
    }

    /**
     * Set the Uri after loading a new document.
     *
     * @param uri
     */
    public void setUri(Uri uri) {

        this.uri = uri;
    }

    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    // TODO: this should probably be a controller instead?
    public void loadDocument(InputStream inputStream, Runnable onRootNodeLoaded) {

        // idea: maybe move to a streaming parser, and just append elements to the view as they become available
        // https://github.com/FasterXML/woodstox

        // start measuring the document load time
        long loadDocumentStartTime = System.currentTimeMillis();

        // XML document builder
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        // load the Mindmap from the InputStream
        Document document;
        try {
            docBuilderFactory.setNamespaceAware(false);
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);

            docBuilder = docBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(new BufferedInputStream(inputStream));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return;
        }


        // get the root node
        rootNode = new MindmapNode(
                document.getDocumentElement().getElementsByTagName("node").item(0),
                null,
                this
        );

        // by default, the root node is the deepest node that is expanded
        deepestSelectedMindmapNode = rootNode;

        // now set up the view
        onRootNodeLoaded.run();

        // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
        // other idea: already finish the mindmap loading, and do this in the background
        MindmapIndexes mindmapIndexes = loadAndIndexNodesByIds(rootNode);

        nodesById = mindmapIndexes.getNodesByIdIndex();
        nodesByNumericId = mindmapIndexes.getNodesByNumericIndex();


        // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
        // now set the corresponding links
        fillArrowLinks();


        long loadDocumentEndTime = System.currentTimeMillis();
        Tracker tracker = MainApplication.getTracker();
        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory("document")
                .setValue(loadDocumentEndTime - loadDocumentStartTime)
                .setVariable("loadDocument")
                .setLabel("loadTime")
                .build());
        Log.d(MainApplication.TAG, "Document loaded");

        // TODO: currently we have to click on "top" once in the UI to make the view show up, after the document is loaded

        long numNodes = document.getElementsByTagName("node").getLength();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("document")
                .setAction("loadDocument")
                .setLabel("numNodes")
                .setValue(numNodes)
                .build()
        );

        this.isLoaded = true;

    }

    /**
     * Returns the root node of the currently loaded mind map
     *
     * @return the root node
     */
    public MindmapNode getRootNode() {

        return rootNode;
    }

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    private MindmapIndexes loadAndIndexNodesByIds(MindmapNode root) {

        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        Stack<MindmapNode> stack = new Stack<>();
        stack.push(root);

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        List<Pair<String, MindmapNode>> idAndNode = new ArrayList<>();
        List<Pair<Integer, MindmapNode>> numericIdAndNode = new ArrayList<>();

        while (!stack.isEmpty()) {
            MindmapNode node = stack.pop();

            idAndNode.add(new Pair<>(node.getId(), node));
            numericIdAndNode.add(new Pair<>(node.getNumericId(), node));

            for (MindmapNode mindmapNode : node.getChildNodes()) {
                stack.push(mindmapNode);
            }
        }

        Map<String, MindmapNode> newNodesById = new HashMap<>(idAndNode.size());
        Map<Integer, MindmapNode> newNodesByNumericId = new HashMap<>(numericIdAndNode.size());

        for (Pair<String, MindmapNode> i : idAndNode) {
            newNodesById.put(i.first, i.second);
        }
        for (Pair<Integer, MindmapNode> i : numericIdAndNode) {
            newNodesByNumericId.put(i.first, i.second);
        }

        return new MindmapIndexes(newNodesById, newNodesByNumericId);

    }

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    public MindmapNode getNodeByID(String id) {
        return nodesById.get(id);
    }

    public MindmapNode getNodeByNumericID(Integer numericId) {
        return nodesByNumericId.get(numericId);
    }

    public MindmapNode getDeepestSelectedMindmapNode() {

        return deepestSelectedMindmapNode;
    }

    public void setDeepestSelectedMindmapNode(MindmapNode deepestSelectedMindmapNode) {

        this.deepestSelectedMindmapNode = deepestSelectedMindmapNode;
    }

    private void fillArrowLinks() {

        for (String nodeId : nodesById.keySet()) {
            MindmapNode mindmapNode = nodesById.get(nodeId);
            for (String linkDestinationId : mindmapNode.getArrowLinkDestinationIds()) {
                MindmapNode destinationNode = nodesById.get(linkDestinationId);
                if (destinationNode != null) {
                    mindmapNode.getArrowLinkDestinationNodes().add(destinationNode);
                    destinationNode.getArrowLinkIncomingNodes().add(mindmapNode);
                }
            }
        }
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    private class MindmapIndexes {

        private final Map<String, MindmapNode> nodesById;
        private final Map<Integer, MindmapNode> nodesByNumericId;

        private MindmapIndexes(Map<String, MindmapNode> nodesById, Map<Integer, MindmapNode> nodesByNumericId) {
            this.nodesById = nodesById;
            this.nodesByNumericId = nodesByNumericId;
        }

        public Map<String, MindmapNode> getNodesByIdIndex() {
            return this.nodesById;
        }

        public Map<Integer, MindmapNode> getNodesByNumericIndex() {
            return this.nodesByNumericId;
        }
    }
}
