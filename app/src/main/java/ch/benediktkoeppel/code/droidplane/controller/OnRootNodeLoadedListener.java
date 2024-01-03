package ch.benediktkoeppel.code.droidplane.controller;

import ch.benediktkoeppel.code.droidplane.model.Mindmap;
import ch.benediktkoeppel.code.droidplane.model.MindmapNode;

public interface OnRootNodeLoadedListener {

    void rootNodeLoaded(Mindmap mindmap, MindmapNode rootNode);
}
