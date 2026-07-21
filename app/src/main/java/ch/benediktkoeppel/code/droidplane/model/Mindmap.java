package ch.benediktkoeppel.code.droidplane.model;

import android.net.Uri;

import androidx.lifecycle.ViewModel;

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
    MindmapIndexes mindmapIndexes;

    // whether the mindmap has finished loading
    private boolean isLoaded = false;

    /**
     * Size and modification time of the document as it was when we loaded it. We use this to tell whether a document
     * that is opened again is still the one we have in memory, or whether it has changed in the meantime (e.g.
     * because Dropbox has synced new content) and has to be parsed again.
     */
    private String documentVersion;

    /**
     * The deepest node that the user has expanded. This lives here (and not in the view) so that we can restore the
     * user's position when the activity is re-created, e.g. after a screen rotation.
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
     * Returns the root node of the currently loaded mind map
     *
     * @return the root node
     */
    public MindmapNode getRootNode() {

        return rootNode;
    }

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    public MindmapNode getNodeByID(String id) {
        // the indexes are only built once the document is fully parsed, so while loading we can't resolve anything yet
        if (mindmapIndexes == null) {
            return null;
        }
        return mindmapIndexes.getNodesByIdIndex().get(id);
    }

    public MindmapNode getNodeByNumericID(Integer numericId) {
        if (mindmapIndexes == null) {
            return null;
        }
        return mindmapIndexes.getNodesByNumericIndex().get(numericId);
    }

    public String getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(String documentVersion) {
        this.documentVersion = documentVersion;
    }

    public MindmapNode getDeepestSelectedMindmapNode() {
        return deepestSelectedMindmapNode;
    }

    public void setDeepestSelectedMindmapNode(MindmapNode deepestSelectedMindmapNode) {
        this.deepestSelectedMindmapNode = deepestSelectedMindmapNode;
    }

    /**
     * Forgets the currently loaded document, so that a new document can be loaded into this Mindmap
     */
    public void reset() {

        uri = null;
        documentVersion = null;
        rootNode = null;
        mindmapIndexes = null;
        deepestSelectedMindmapNode = null;
        isLoaded = false;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public void setRootNode(MindmapNode rootNode) {
        this.rootNode = rootNode;
    }

    public void setMindmapIndexes(MindmapIndexes mindmapIndexes) {
        this.mindmapIndexes = mindmapIndexes;
    }

    public MindmapIndexes getMindmapIndexes() {
        return mindmapIndexes;
    }
}
