package ch.benediktkoeppel.code.droidplane.controller;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
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

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.MainApplication;
import ch.benediktkoeppel.code.droidplane.R;
import ch.benediktkoeppel.code.droidplane.model.Mindmap;
import ch.benediktkoeppel.code.droidplane.model.MindmapIndexes;
import ch.benediktkoeppel.code.droidplane.model.MindmapNode;
import ch.benediktkoeppel.code.droidplane.view.HorizontalMindmapView;

public class AsyncMindmapLoaderTask extends AsyncTask<String, Void, Object> {

    private final MainActivity mainActivity;
    private final Intent intent;
    private final String action;
    private final HorizontalMindmapView horizontalMindmapView;

    private final Mindmap mindmap;

    public AsyncMindmapLoaderTask(MainActivity mainActivity,
                                  Mindmap mindmap,
                                  HorizontalMindmapView horizontalMindmapView,
                                  Intent intent) {

        this.mainActivity = mainActivity;
        this.intent = intent;
        this.action = intent.getAction();
        this.horizontalMindmapView = horizontalMindmapView;
        this.mindmap = mindmap;
    }

    @Override
    protected Object doInBackground(String... strings) {

        // prepare loading of the Mindmap file
        InputStream mm = null;

        // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the
        // launcher started from ACTION_EDIT/VIEW intent
        if ((Intent.ACTION_EDIT.equals(action) || Intent.ACTION_VIEW.equals(action)) ||
                Intent.ACTION_OPEN_DOCUMENT.equals(action)
        ) {

            Log.d(MainApplication.TAG, "started from ACTION_EDIT/VIEW intent");

            // get the URI to the target document (the Mindmap we are opening) and open the InputStream
            Uri uri = intent.getData();
            if (uri != null) {
                ContentResolver cr = mainActivity.getContentResolver();
                try {
                    mm = cr.openInputStream(uri);
                } catch (FileNotFoundException e) {

                    mainActivity.abortWithPopup(R.string.filenotfound);
                    e.printStackTrace();
                }
            } else {
                mainActivity.abortWithPopup(R.string.novalidfile);
            }

            // store the Uri. Next time the MainActivity is started, we'll
            // check whether the Uri has changed (-> load new document) or
            // remained the same (-> reuse previous document)
            this.mindmap.setUri(uri);
        }

        // started from the launcher
        else {
            Log.d(MainApplication.TAG, "started from app launcher intent");

            // display the default Mindmap "example.mm", from the resources
            mm = mainActivity.getApplicationContext().getResources().openRawResource(R.raw.example);
        }

        // load the mindmap
        Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");

        loadDocument(mm);

        return null;
    }


    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    public void loadDocument(InputStream inputStream) {

        // show loading indicator
        mainActivity.setMindmapIsLoading(true);

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
        MindmapNode rootNode = MindmapNodeFromXmlBuilder.parse(
                document.getDocumentElement().getElementsByTagName("node").item(0),
                null,
                mindmap
        );

        mindmap.setRootNode(rootNode);

        // now set up the view
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                horizontalMindmapView.setMindmap(mindmap);

                // by default, the root node is the deepest node that is expanded
                horizontalMindmapView.setDeepestSelectedMindmapNode(rootNode);

                horizontalMindmapView.onRootNodeLoaded();

            }
        });

        // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
        MindmapIndexes mindmapIndexes = loadAndIndexNodesByIds(rootNode);
        mindmap.setMindmapIndexes(mindmapIndexes);

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

        long numNodes = document.getElementsByTagName("node").getLength();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("document")
                .setAction("loadDocument")
                .setLabel("numNodes")
                .setValue(numNodes)
                .build()
        );

        // now the full mindmap is loaded
        mindmap.setLoaded(true);
        mainActivity.setMindmapIsLoading(false);

    }


    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    private MindmapIndexes loadAndIndexNodesByIds(MindmapNode root) {

        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all mindmap nodes, defeating the lazy loading in ch.benediktkoeppel.code.droidplane.model.MindmapNode.getChildNodes

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

            for (MindmapNode mindmapNode : node.getChildMindmapNodes()) {
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

    private void fillArrowLinks() {

        Map<String, MindmapNode> nodesById = mindmap.getMindmapIndexes().getNodesByIdIndex();

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

}
