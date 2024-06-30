package ch.benediktkoeppel.code.droidplane.model;

import java.util.Map;

import ch.benediktkoeppel.code.droidplane.model.MindmapNode;

public class MindmapIndexes {

    private final Map<String, MindmapNode> nodesById;
    private final Map<Integer, MindmapNode> nodesByNumericId;

    public MindmapIndexes(Map<String, MindmapNode> nodesById, Map<Integer, MindmapNode> nodesByNumericId) {
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
