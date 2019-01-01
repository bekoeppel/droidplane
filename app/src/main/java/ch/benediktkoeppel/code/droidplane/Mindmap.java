package ch.benediktkoeppel.code.droidplane;

import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
public class Mindmap extends ViewModel {

    /**
     * the XML DOM document, the mind map
     */
    public Document document;

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

    /**
     * The deepest selected mindmap node
     */
    private MindmapNode deepestSelectedMindmapNode;

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
    public void loadDocument(InputStream inputStream, Context context) {

        // start measuring the document load time
        long loadDocumentStartTime = System.currentTimeMillis();

        // XML document builder
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;

        // load the Mindmap from the InputStream
        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
            document = docBuilder.parse(inputStream);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return;
        } catch (SAXException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
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

        long loadDocumentEndTime = System.currentTimeMillis();
        Tracker tracker = MainApplication.getTracker();
        tracker.send(new HitBuilders.TimingBuilder()
                .setCategory("document")
                .setValue(loadDocumentEndTime - loadDocumentStartTime)
                .setVariable("loadDocument")
                .setLabel("loadTime")
                .build());
        Log.d(MainApplication.TAG, "Document loaded");

        long numNodes = document.getElementsByTagName("node").getLength();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("document")
                .setAction("loadDocument")
                .setLabel("numNodes")
                .setValue(numNodes)
                .build()
        );

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
     * @param node
     */
    private void indexNodesByIds(MindmapNode node) {

        this.nodesById.put(node.getId(), node);
        Log.v(MainApplication.TAG, "Added " + node.getId() + " to hashmap");

        for (MindmapNode mindmapNode : node.getChildNodes()) {
            indexNodesByIds(mindmapNode);
        }

    }

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    public MindmapNode getNodeByID(String id) {

        // lazy index mindmap nodes
        if (this.nodesById == null) {
            this.nodesById = new HashMap<>();
            indexNodesByIds(rootNode);
        }

        MindmapNode node = this.nodesById.get(id);

        Log.d(MainApplication.TAG, "Returning node with ID " + id);

        return node;
    }

    public MindmapNode getDeepestSelectedMindmapNode() {

        return deepestSelectedMindmapNode;
    }

    public void setDeepestSelectedMindmapNode(MindmapNode deepestSelectedMindmapNode) {

        this.deepestSelectedMindmapNode = deepestSelectedMindmapNode;
    }
}
