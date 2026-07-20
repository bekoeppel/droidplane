package ch.benediktkoeppel.code.droidplane.model;

import android.net.Uri;
import android.os.AsyncTask;

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
     * The deepest node that the user has expanded. This lives here (and not in the view) so that we can restore the
     * user's position when the activity is re-created, e.g. after a screen rotation.
     */
    private MindmapNode deepestSelectedMindmapNode;

    /**
     * The task that is currently loading this mindmap, if any. This lives here (and not in the activity) because it
     * outlives the activity: when the screen is rotated while a document is loading, the new activity has to be able
     * to stop the load that the previous activity had started.
     */
    private AsyncTask<?, ?, ?> loadingTask;

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

    public MindmapNode getDeepestSelectedMindmapNode() {
        return deepestSelectedMindmapNode;
    }

    public void setDeepestSelectedMindmapNode(MindmapNode deepestSelectedMindmapNode) {
        this.deepestSelectedMindmapNode = deepestSelectedMindmapNode;
    }

    public AsyncTask<?, ?, ?> getLoadingTask() {
        return loadingTask;
    }

    public void setLoadingTask(AsyncTask<?, ?, ?> loadingTask) {
        this.loadingTask = loadingTask;
    }

    /**
     * Forgets the currently loaded document, so that a new document can be loaded into this Mindmap
     */
    public void reset() {

        uri = null;
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
