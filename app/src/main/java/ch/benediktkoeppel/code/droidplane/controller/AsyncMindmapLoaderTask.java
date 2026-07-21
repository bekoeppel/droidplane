package ch.benediktkoeppel.code.droidplane.controller;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
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

public class AsyncMindmapLoaderTask extends AsyncTask<String, Void, Object> {

    /**
     * The load that is currently running, if any.
     * <p>
     * Loading a mindmap costs a lot of memory, so there must never be more than one load in flight - two parses of a
     * large document at the same time run the device out of memory. The activity can not keep track of this on its
     * own: it is destroyed and re-created on a screen rotation, and the launcher can start a second instance of it,
     * and in both cases the load that the previous instance started keeps running.
     */
    private static AsyncMindmapLoaderTask runningTask;

    // TODO: why is MainActivty needed here?
    // not final: when the activity is re-created (e.g. on a screen rotation) while we are loading, the new activity
    // attaches itself to this running load instead of starting the whole parse again
    private volatile MainActivity mainActivity;
    private volatile OnRootNodeLoadedListener onRootNodeLoadedListener;

    private final Intent intent;
    private final String action;

    private final Mindmap mindmap;

    public AsyncMindmapLoaderTask(MainActivity mainActivity,
                                  OnRootNodeLoadedListener onRootNodeLoadedListener,
                                  Mindmap mindmap,
                                  Intent intent) {

        this.mainActivity = mainActivity;
        this.onRootNodeLoadedListener = onRootNodeLoadedListener;
        this.intent = intent;
        this.action = intent.getAction();
        this.mindmap = mindmap;
    }

    /**
     * The load that is currently running, or null if no mindmap is being loaded
     */
    public static synchronized AsyncMindmapLoaderTask getRunningTask() {

        return runningTask;
    }

    /**
     * Stops the load that is currently running, if any
     */
    public static synchronized void cancelRunningTask() {

        if (runningTask != null) {
            runningTask.cancel(true);
            runningTask = null;
        }
    }

    private static synchronized void setRunningTask(AsyncMindmapLoaderTask task) {

        runningTask = task;
    }

    private static synchronized void clearRunningTask(AsyncMindmapLoaderTask task) {

        if (runningTask == task) {
            runningTask = null;
        }
    }

    /**
     * Stops whatever else was loading and starts this load.
     */
    public void start() {

        cancelRunningTask();
        setRunningTask(this);

        // Run on the thread pool rather than on AsyncTask's default serial executor: a load that we just cancelled
        // may still be stuck in a blocking read on a slow content provider, and it would keep this load from ever
        // starting.
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Hands this running load over to a re-created activity, so that it can show the document that is already being
     * parsed instead of starting the whole parse from the beginning.
     *
     * @param mainActivity             the new activity
     * @param onRootNodeLoadedListener the new activity's listener
     */
    public void attachTo(MainActivity mainActivity, OnRootNodeLoadedListener onRootNodeLoadedListener) {

        this.mainActivity = mainActivity;
        this.onRootNodeLoadedListener = onRootNodeLoadedListener;

        // the new activity does not know yet that it is waiting for a document
        mainActivity.setMindmapIsLoading(true);

        // let it display what we have parsed so far. Everything that we parse from now on reaches it through the
        // subscriptions that its node columns set up.
        MindmapNode rootNode = mindmap.getRootNode();
        if (rootNode != null) {
            onRootNodeLoadedListener.rootNodeLoaded(mindmap, rootNode);
        }
    }

    public Mindmap getMindmap() {

        return mindmap;
    }

    @Override
    protected void onPostExecute(Object result) {

        clearRunningTask(this);
    }

    @Override
    protected void onCancelled(Object result) {

        clearRunningTask(this);
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

            // Log which document we are asked to open, and how it was announced to us. Which app can open a .mm
            // file, and whether the user's "always open with" sticks, depends entirely on the URI and the MIME type
            // that the sending app puts into the intent - and the system log redacts the URI of a start request, so
            // this is the only place where we can see it.
            Log.d(MainApplication.TAG,
                    "started from " + action + " intent, data " + intent.getData() + ", type " + intent.getType()
            );

            // get the URI to the target document (the Mindmap we are opening) and open the InputStream
            Uri uri = intent.getData();
            if (uri != null) {
                ContentResolver cr = mainActivity.getContentResolver();
                try {
                    mm = cr.openInputStream(uri);
                } catch (FileNotFoundException e) {

                    abortWithPopupOnUiThread(R.string.filenotfound);
                    e.printStackTrace();
                }
            } else {
                abortWithPopupOnUiThread(R.string.novalidfile);
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

        // we could not open the document, and have already told the user about it
        if (mm == null) {
            return null;
        }

        // load the mindmap
        Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document");

        loadDocument(mm);

        return null;
    }

    /**
     * Shows the error popup (which has to happen on the UI thread) for the given message
     */
    private void abortWithPopupOnUiThread(int stringResourceId) {

        mainActivity.runOnUiThread(() -> mainActivity.abortWithPopup(stringResourceId));
    }


    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    public void loadDocument(InputStream inputStream) {

        // show loading indicator
        mainActivity.setMindmapIsLoading(true);

        // Sanitize the input stream for invalid XML entities. Some mindmap
        // files created by older Freeplane/Freemind versions contain HTML
        // entities such as "&nbsp;" which are not valid in XML. The sanitizer
        // replaces such entities on the fly so the parser does not abort.
        if (inputStream != null) {
            inputStream = sanitizeInputStream(inputStream);
        }

        // start measuring the document load time
        long loadDocumentStartTime = System.currentTimeMillis();

        MindmapNode rootNode = null;
        Stack<MindmapNode> nodeStack = new Stack<>();
        int numNodes = 0;

        // whether we managed to parse the document until its end. Mindmap files are sometimes incomplete (e.g. when
        // Dropbox has not fully synced them yet), in which case we still show as much as we could parse.
        boolean isComplete = true;

        try {
            // set up XML pull parsing
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(inputStream, "UTF-8");

            // stream parse the XML
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                // we are cancelled when another document is being loaded. Stop, so that we don't write into that
                // document's mindmap.
                if (isCancelled()) {
                    Log.d(MainApplication.TAG, "Mindmap loading was cancelled");
                    return;
                }

                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(MainApplication.TAG, "Received XML Start Document");

                } else if (eventType == XmlPullParser.START_TAG) {

                    if (xpp.getName().equals("node")) {

                        MindmapNode parentNode = null;
                        if (!nodeStack.empty()) {
                            parentNode = nodeStack.peek();
                        }

                        MindmapNode newMindmapNode = parseNodeTag(xpp, parentNode);
                        nodeStack.push(newMindmapNode);
                        numNodes += 1;

                        newMindmapNode.subscribeNodeRichContentChanged(mainActivity);

                        // if we don't have a parent node, then this is the root node
                        if (parentNode == null) {
                            rootNode = newMindmapNode;
                            mindmap.setRootNode(rootNode);
                            onRootNodeLoadedListener.rootNodeLoaded(mindmap, rootNode);

                        } else {
                            parentNode.addChildMindmapNode(newMindmapNode);
                            if (parentNode.hasAddedChildMindmapNodeSubscribers()) {
                                MindmapNode finalParentNode = parentNode;
                                mainActivity.runOnUiThread(() -> {
                                    finalParentNode.notifySubscribersAddedChildMindmapNode(newMindmapNode);
                                });
                            }

                            // with its first child, the parent node becomes expandable, so the node itself (which is
                            // drawn in the column to the left) has to redraw to show the expandable indicator
                            if (parentNode.getNumChildMindmapNodes() == 1 && parentNode.hasNodeStyleChangedSubscribers()) {
                                MindmapNode finalParentNode = parentNode;
                                mainActivity.runOnUiThread(finalParentNode::notifySubscribersNodeStyleChanged);
                            }
                        }

                    }

                    else if (xpp.getName().equals("richcontent")
                            && isSupportedRichContentType(xpp.getAttributeValue(null, "TYPE"))
                    ) {

                        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
                        // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

                        // if this is an empty tag, we won't need to bother trying to read its content
                        // we don't even need to read the <richcontent> node's attributes, as we would
                        // only be interested in it's children
                        if (xpp.isEmptyElementTag()) {
                            Log.d(MainApplication.TAG, "Received empty richcontent node - skipping");

                        } else {
                            String richTextContent = loadRichContentNodes(xpp);

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

                            // the node itself has to redraw as well: its text (for nodes which only have rich text
                            // content) and its rich text icon only become known now
                            if (parentNode.hasNodeStyleChangedSubscribers()) {
                                MindmapNode finalParentNode = parentNode;
                                mainActivity.runOnUiThread(finalParentNode::notifySubscribersNodeStyleChanged);
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
                        if (parentNode.hasNodeStyleChangedSubscribers()) {
                            MindmapNode finalParentNode = parentNode;
                            mainActivity.runOnUiThread(() -> {
                                finalParentNode.notifySubscribersNodeStyleChanged();
                            });
                        }

                    }

                    else if (xpp.getName().equals("icon") && xpp.getAttributeValue(null, "BUILTIN") != null) {
                        String iconName = xpp.getAttributeValue(null, "BUILTIN");

                        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a mindmap node
                        if (nodeStack.empty()) {
                            throw new IllegalStateException("Received icon without a parent node");
                        }

                        MindmapNode parentNode = nodeStack.peek();
                        parentNode.addIconName(iconName);

                        // let view know that node content has changed
                        if (parentNode.hasNodeStyleChangedSubscribers()) {
                            MindmapNode finalParentNode = parentNode;
                            mainActivity.runOnUiThread(() -> {
                                finalParentNode.notifySubscribersNodeStyleChanged();
                            });
                        }

                    }

                    else if (xpp.getName().equals("arrowlink")) {
                        String destinationId = xpp.getAttributeValue(null, "DESTINATION");

                        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a mindmap node
                        if (nodeStack.empty()) {
                            throw new IllegalStateException("Received arrowlink without a parent node");
                        }

                        MindmapNode parentNode = nodeStack.peek();
                        parentNode.addArrowLinkDestinationId(destinationId);

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

        } catch (OutOfMemoryError e) {

            // The document does not fit into memory on this device. Everything we parsed so far is useless: building
            // the indexes and displaying it would only run out of memory again, so we drop the whole tree and tell
            // the user. An OutOfMemoryError is an Error, not an Exception, so the catch below never saw it and the
            // app died instead.
            Log.e(MainApplication.TAG, "Ran out of memory while loading the mindmap", e);

            rootNode = null;
            nodeStack.clear();
            mindmap.setRootNode(null);

            mainActivity.setMindmapIsLoading(false);
            abortWithPopupOnUiThread(R.string.mindmaptoobig);
            return;

        } catch (Exception e) {

            // don't crash on a broken (or incompletely synced) document - we show whatever we have parsed so far
            Log.e(MainApplication.TAG, "Could not parse the mindmap document", e);
            isComplete = false;
        }

        if (isCancelled()) {
            return;
        }

        // the stack should now be empty. If it isn't, the document ended in the middle of a node.
        if (!nodeStack.empty()) {
            Log.w(MainApplication.TAG, "Mindmap document ended with " + nodeStack.size() + " unclosed nodes");
            isComplete = false;
            while (!nodeStack.empty()) {
                nodeStack.pop().setLoaded(true);
            }
        }

        // if we could not even parse the root node, we have nothing to show at all
        if (rootNode == null) {
            mainActivity.setMindmapIsLoading(false);
            abortWithPopupOnUiThread(R.string.cantloadfile);
            return;
        }

        if (!isComplete) {
            mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity,
                    R.string.incompletefile,
                    Toast.LENGTH_LONG
            ).show());
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

        if (isCancelled()) {
            return;
        }

        // now the full mindmap is loaded
        mindmap.setLoaded(true);
        mainActivity.setMindmapIsLoading(false);

    }

    /**
     * Whether we display the given richcontent TYPE. The attribute is optional, so it can be null.
     *
     * @param type the TYPE attribute of a richcontent tag, or null if it has none
     */
    private boolean isSupportedRichContentType(String type) {

        return "NODE".equals(type) || "NOTE".equals(type) || "DETAILS".equals(type);
    }

    private String loadRichContentNodes(XmlPullParser xpp) throws IOException, XmlPullParserException {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        int startingDepth = xpp.getDepth();
        StringBuilder richTextContent = new StringBuilder();

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

                    richTextContent.append("<").append(xpp.getName());

                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                        String attributeName = xpp.getAttributeName(i);
                        String attributeValue = xpp.getAttributeValue(i);

                        // the parser hands us the decoded attribute value, so we have to escape it again - otherwise
                        // a value containing a quote or an ampersand would produce broken markup
                        richTextContent.append(" ").append(attributeName).append("=\"");
                        appendEscaped(richTextContent, attributeValue, true);
                        richTextContent.append("\"");
                    }

                    richTextContent.append(">");

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
                    richTextContent.append("</").append(xpp.getName()).append(">");
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

                    // the parser hands us the decoded text, so "&amp;" arrives as "&" here. We have to escape it
                    // again, otherwise it would be re-interpreted as markup when we render the rich text.
                    appendEscaped(richTextContent, xpp.getText(), false);
                    break;
                }

                default:
                    throw new IllegalStateException("Received unexpected event type " + richContentSubParserEventType);

            }

            richContentSubParserEventType = xpp.next();

        // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.getDepth() != startingDepth);
        return richTextContent.toString();
    }

    /**
     * Appends text to the rich text we are re-assembling, escaping the characters that would otherwise be read back
     * as markup.
     *
     * @param target       where to append to
     * @param text         the (already decoded) text to append
     * @param isAttribute  whether we are writing an attribute value, which additionally needs its quotes escaped
     */
    private void appendEscaped(StringBuilder target, String text, boolean isAttribute) {

        if (text == null) {
            return;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    target.append("&amp;");
                    break;
                case '<':
                    target.append("&lt;");
                    break;
                case '>':
                    target.append("&gt;");
                    break;
                case '"':
                    if (isAttribute) {
                        target.append("&quot;");
                    } else {
                        target.append(c);
                    }
                    break;
                case '\'':
                    if (isAttribute) {
                        target.append("&#39;");
                    } else {
                        target.append(c);
                    }
                    break;
                default:
                    target.append(c);
            }
        }
    }

    private MindmapNode parseNodeTag(XmlPullParser xpp, MindmapNode parentNode) {
        // the ID attribute is optional - Freeplane always writes one, but hand-edited files sometimes don't have it
        String id = xpp.getAttributeValue(null, "ID");
        if (id == null) {
            id = "";
        }

        int numericId;
        try {
            numericId = Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            numericId = id.hashCode();
        }

        String text = xpp.getAttributeValue(null, "TEXT");

        // get link
        String linkAttribute = xpp.getAttributeValue(null, "LINK");
        Uri link;
        if (linkAttribute != null && !linkAttribute.equals("")) {
            link = Uri.parse(linkAttribute);
        } else {
            link = null;
        }

        // get tree ID (of cloned node)
        String treeIdAttribute = xpp.getAttributeValue(null, "TREE_ID");

        MindmapNode newMindmapNode = new MindmapNode(mindmap, parentNode, id, numericId, text, link, treeIdAttribute);
        return newMindmapNode;
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

    /**
     * Wrap the given stream with a filter that replaces invalid or unsupported
     * HTML entities on the fly. The sanitizer decodes any HTML entities it
     * recognizes and writes them back using numeric references so that the XML
     * parser does not fail. This avoids loading the entire document into memory.
     */
    private InputStream sanitizeInputStream(InputStream inputStream) {
        return new HtmlEntitySanitizingInputStream(inputStream, mainActivity);
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
