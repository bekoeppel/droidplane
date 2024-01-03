package ch.benediktkoeppel.code.droidplane.controller;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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

        MindmapNode rootNode = null;


        Stack<MindmapNode> nodeStack = new Stack<>();
        int numNodes = 0;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(inputStream, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {

                } else if (eventType == XmlPullParser.START_TAG) {

                    if (xpp.getName().equals("node")) {

                        MindmapNode parentNode = null;
                        if (!nodeStack.empty()) {
                            parentNode = nodeStack.peek();
                        }

                        String id = xpp.getAttributeValue(null, "ID");
                        int numericId;
                        try {
                            numericId = Integer.parseInt(id.replaceAll("\\D+", ""));
                        } catch (NumberFormatException e) {
                            numericId = id.hashCode();
                        }

                        String text = xpp.getAttributeValue(null, "TEXT");

                        MindmapNode newMindmapNode = new MindmapNode(mindmap, parentNode, id, numericId, text);
                        newMindmapNode.subscribeNodeRichContentChanged(mainActivity);
                        numNodes += 1;

                        if (parentNode == null) {
                            rootNode = newMindmapNode;

                            mindmap.setRootNode(rootNode);

                            // now set up the view
                            MindmapNode finalRootNode = rootNode;
                            mainActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    horizontalMindmapView.setMindmap(mindmap);

                                    // by default, the root node is the deepest node that is expanded
                                    horizontalMindmapView.setDeepestSelectedMindmapNode(finalRootNode);

                                    horizontalMindmapView.onRootNodeLoaded();

                                }
                            });

                        } else {
                            parentNode.addChildMindmapNode(newMindmapNode);
                            if (parentNode.hasAddedChildMindmapNodeSubscribers()) {
                                MindmapNode finalParentNode = parentNode;
                                mainActivity.runOnUiThread(() -> {
                                    finalParentNode.notifySubscribersAddedChildMindmapNode(newMindmapNode);
                                });
                            }
                        }

                        nodeStack.push(newMindmapNode);

                    }

                    else if (xpp.getName().equals("richcontent")
                            && (
                            xpp.getAttributeValue(null, "TYPE").equals("NODE")
                                    || xpp.getAttributeValue(null, "TYPE").equals("NOTE")
                                    || xpp.getAttributeValue(null, "TYPE").equals("DETAILS")
                    )
                    ) {

                        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
                        // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").
                        String richTextContent = "";

                        // if this is an empty tag, we won't need to bother trying to read its content
                        // we don't even need to read the <richcontent> node's attributes, as we would
                        // only be interested in it's children
                        if (xpp.isEmptyElementTag()) {

//                            String tagString = "";
//
//                            String tagName = xpp.getName();
//                            tagString += "<" + tagName;
//
//                            for (int i = 0; i < xpp.getAttributeCount(); i++) {
//                                String attributeName = xpp.getAttributeName(i);
//                                String attributeValue = xpp.getAttributeValue(i);
//
//                                String attributeString = " " + attributeName + "=" + '"' + attributeValue + '"';
//                                tagString += attributeString;
//                            }
//
//                            tagString += " />";
//
//                            richTextContent += tagString;

                            richTextContent = "";

                        } else {

                            // as we are stream processing the XML, we need to consume the full XML until the
                            // richcontent tag is closed (i.e. until we're back at the current parsing depth)
                            // eagerly parse until richcontent node is closed
                            int startingDepth = xpp.getDepth();

                            int richContentSubParserEventType = xpp.next();

                            do {

                                // EVENT TYPES as reported by next()
                                switch (richContentSubParserEventType) {
                                    /**
                                     * Signalize that parser is at the very beginning of the document
                                     * and nothing was read yet.
                                     * This event type can only be observed by calling getEvent()
                                     * before the first call to next(), nextToken, or nextTag()</a>).
                                     */
                                    case XmlPullParser.START_DOCUMENT:
                                        throw new IllegalStateException("Received START_DOCUMENT but were already within the document");

                                    /**
                                     * Logical end of the xml document. Returned from getEventType, next()
                                     * and nextToken()
                                     * when the end of the input document has been reached.
                                     * <p><strong>NOTE:</strong> subsequent calls to
                                     * <a href="#next()">next()</a> or <a href="#nextToken()">nextToken()</a>
                                     * may result in exception being thrown.
                                     */
                                    case XmlPullParser.END_DOCUMENT:
                                        throw new IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document");

                                    /**
                                     * Returned from getEventType(),
                                     * <a href="#next()">next()</a>, <a href="#nextToken()">nextToken()</a> when
                                     * a start tag was read.
                                     * The name of start tag is available from getName(), its namespace and prefix are
                                     * available from getNamespace() and getPrefix()
                                     * if <a href='#FEATURE_PROCESS_NAMESPACES'>namespaces are enabled</a>.
                                     * See getAttribute* methods to retrieve element attributes.
                                     * See getNamespace* methods to retrieve newly declared namespaces.
                                     */
                                    case XmlPullParser.START_TAG: {
                                        String tagString = "";

                                        String tagName = xpp.getName();
                                        tagString += "<" + tagName;

                                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                                            String attributeName = xpp.getAttributeName(i);
                                            String attributeValue = xpp.getAttributeValue(i);

                                            String attributeString = " " + attributeName + "=" + '"' + attributeValue + '"';
                                            tagString += attributeString;
                                        }

                                        tagString += ">";

                                        richTextContent += tagString;

                                        break;
                                    }

                                    /**
                                     * Returned from getEventType(), <a href="#next()">next()</a>, or
                                     * <a href="#nextToken()">nextToken()</a> when an end tag was read.
                                     * The name of start tag is available from getName(), its
                                     * namespace and prefix are
                                     * available from getNamespace() and getPrefix().
                                     */
                                    case XmlPullParser.END_TAG: {
                                        String tagName = xpp.getName();
                                        String tagString = "</" + tagName + ">";
                                        richTextContent += tagString;
                                        break;
                                    }

                                    /**
                                     * Character data was read and will is available by calling getText().
                                     * <p><strong>Please note:</strong> <a href="#next()">next()</a> will
                                     * accumulate multiple
                                     * events into one TEXT event, skipping IGNORABLE_WHITESPACE,
                                     * PROCESSING_INSTRUCTION and COMMENT events,
                                     * In contrast, <a href="#nextToken()">nextToken()</a> will stop reading
                                     * text when any other event is observed.
                                     * Also, when the state was reached by calling next(), the text value will
                                     * be normalized, whereas getText() will
                                     * return unnormalized content in the case of nextToken(). This allows
                                     * an exact roundtrip without changing line ends when examining low
                                     * level events, whereas for high level applications the text is
                                     * normalized appropriately.
                                     */
                                    case XmlPullParser.TEXT: {
                                        String text = xpp.getText();
                                        richTextContent += text;
                                        break;
                                    }

                                    default:
                                        throw new IllegalStateException("Received unexpected event type " + richContentSubParserEventType);

                                }

                                richContentSubParserEventType = xpp.next();

                            // stop parsing once we have come out far enough from the XML to be at the starting depth again
                            } while (xpp.getDepth() != startingDepth);

                            // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part of a mindmap node
                            if (nodeStack.empty()) {
                                throw new IllegalStateException("Received richtext without a parent node");
                            }

                            MindmapNode parentNode = nodeStack.peek();
                            parentNode.addRichTextContent(richTextContent);

                            // let view know that node content has changed
                            if (parentNode.hasNodeRichContentChangedSubscribers()) {
                                MindmapNode finalParentNode = parentNode;
                                mainActivity.runOnUiThread(() -> {
                                    finalParentNode.notifySubscribersNodeRichContentChanged();
                                });
                            }
                        }

                    }

                    else if (xpp.getName().equals("font")) {
                        String boldAttribute = xpp.getAttributeValue(null, "BOLD");

                        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part of a mindmap node
                        if (nodeStack.empty()) {
                            throw new IllegalStateException("Received richtext without a parent node");
                        }
                        MindmapNode parentNode = nodeStack.peek();

                        if (boldAttribute != null && boldAttribute.equals("true")) {
                            parentNode.setBold(true);
                        }

                        String italicsAttribute = xpp.getAttributeValue(null, "ITALIC");
                        if (italicsAttribute != null && italicsAttribute.equals("true")) {
                            parentNode.setItalic(true);
                        }

                        // let view know that node content has changed
                        if (parentNode.hasNodeRichContentChangedSubscribers()) {
                            MindmapNode finalParentNode = parentNode;
                            mainActivity.runOnUiThread(() -> {
                                finalParentNode.notifySubscribersNodeStyleChanged();
                            });
                        }

                    }

                    else {
                        // Log.d(MainApplication.TAG, "Received unknown node " + xpp.getName());
                    }


                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.getName().equals("node")) {
                        MindmapNode completedMindmapNode = nodeStack.pop();
                        completedMindmapNode.setLoaded(true);
                    }

                } else if (eventType == XmlPullParser.TEXT) {
                    // TODO: do we have TEXT nodes in the mindmap at all?

                } else {
                    throw new IllegalStateException("Received unknown event " + eventType);
                }
                eventType = xpp.next();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // stack should now be empty
        if (!nodeStack.empty()) {
            throw new RuntimeException("Stack should be empty");
            // TODO: we could try to be lenient here to allow opening partial documents (which sometimes happens when dropbox doesn't fully sync). Probably doesn't work anyways, as we already throw a runtime exception above if we receive garbage
        }


        // TODO: can we do this as we stream through the XML above?

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

        //long numNodes = document.getElementsByTagName("node").getLength();
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
