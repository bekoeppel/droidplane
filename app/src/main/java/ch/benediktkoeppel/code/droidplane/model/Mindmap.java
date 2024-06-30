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
        return mindmapIndexes.getNodesByIdIndex().get(id);
    }

    public MindmapNode getNodeByNumericID(Integer numericId) {
        return mindmapIndexes.getNodesByNumericIndex().get(numericId);
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
