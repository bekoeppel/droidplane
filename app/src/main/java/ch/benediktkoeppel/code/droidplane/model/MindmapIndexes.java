package ch.benediktkoeppel.code.droidplane.model;

import java.util.Collections;
import java.util.Map;


/**
 * Resolves node IDs to nodes. Built once, after the whole document was parsed, and only read from then on.
 */
public class MindmapIndexes {

    private final Map<String, MindmapNode> nodesById;
    private final Map<Integer, MindmapNode> nodesByNumericId;

    public MindmapIndexes(Map<String, MindmapNode> nodesById, Map<Integer, MindmapNode> nodesByNumericId) {
        this.nodesById = nodesById;
        this.nodesByNumericId = nodesByNumericId;
    }

    // The indexes are handed to the whole app, but they describe the document that was loaded, so a caller must not
    // be able to quietly add or drop nodes. These wrap, they do not copy - the maps hold one entry per node.

    public Map<String, MindmapNode> getNodesByIdIndex() {
        return Collections.unmodifiableMap(this.nodesById);
    }

    public Map<Integer, MindmapNode> getNodesByNumericIndex() {
        return Collections.unmodifiableMap(this.nodesByNumericId);
    }
}
