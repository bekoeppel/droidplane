package ch.benediktkoeppel.code.droidplane.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class MindmapTest {

    private Mindmap mindmap;

    @Before
    public void setUp() {
        mindmap = new Mindmap();
    }

    @Test
    public void resolvesNodesByIdOnceTheIndexesAreBuilt() {

        MindmapNode node = new MindmapNode(mindmap, null, "ID_42", 42, "root", null, null);
        mindmap.setMindmapIndexes(new MindmapIndexes(Map.of("ID_42", node), Map.of(42, node)));

        assertEquals(node, mindmap.getNodeByID("ID_42"));
        assertEquals(node, mindmap.getNodeByNumericID(42));
        assertNull(mindmap.getNodeByID("ID_unknown"));
    }

    @Test
    public void resolvingANodeWhileTheDocumentIsStillLoadingDoesNotThrow() {

        // the indexes are only built once the whole document is parsed, but nodes ask for their clone target and
        // their arrow link targets while it is still streaming in
        assertNull(mindmap.getNodeByID("ID_42"));
        assertNull(mindmap.getNodeByNumericID(42));
    }

    @Test
    public void resetForgetsTheLoadedDocument() {

        MindmapNode root = new MindmapNode(mindmap, null, "ID_1", 1, "root", null, null);
        mindmap.setUri(Uri.parse("content://provider/doc.mm"));
        mindmap.setDocumentVersion("123@456");
        mindmap.setRootNode(root);
        mindmap.setDeepestSelectedMindmapNode(root);
        mindmap.setMindmapIndexes(new MindmapIndexes(Map.of("ID_1", root), Map.of(1, root)));
        mindmap.setLoaded(true);

        mindmap.reset();

        // everything that identifies the old document has to go, otherwise the next one is mistaken for it
        assertNull(mindmap.getUri());
        assertNull(mindmap.getDocumentVersion());
        assertNull(mindmap.getRootNode());
        assertNull(mindmap.getMindmapIndexes());
        assertNull(mindmap.getDeepestSelectedMindmapNode());
        assertFalse(mindmap.isLoaded());
    }

    @Test
    public void remembersWhereTheUserHadNavigatedTo() {

        // this lives in the view model so that it survives a screen rotation
        MindmapNode root = new MindmapNode(mindmap, null, "ID_1", 1, "root", null, null);
        MindmapNode deep = new MindmapNode(mindmap, root, "ID_2", 2, "deep", null, null);

        mindmap.setRootNode(root);
        mindmap.setDeepestSelectedMindmapNode(deep);

        assertEquals(deep, mindmap.getDeepestSelectedMindmapNode());
    }
}
