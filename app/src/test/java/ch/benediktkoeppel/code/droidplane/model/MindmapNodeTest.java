package ch.benediktkoeppel.code.droidplane.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MindmapNodeTest {

    private Mindmap mindmap;

    @Before
    public void setUp() {
        mindmap = new Mindmap();
    }

    private MindmapNode node(MindmapNode parent, String id, String text) {
        MindmapNode node = new MindmapNode(mindmap, parent, id, id.hashCode(), text, null, null);
        if (parent != null) {
            parent.addChildMindmapNode(node);
        }
        return node;
    }

    @Test
    public void aNodeWithoutChildrenIsNotExpandable() {

        MindmapNode root = node(null, "ID_1", "root");

        assertFalse(root.isExpandable());
        assertEquals(0, root.getNumChildMindmapNodes());
    }

    @Test
    public void aNodeBecomesExpandableWhenItGetsAChild() {

        // this is what happens while the document streams in: a node is created before its children are parsed
        MindmapNode root = node(null, "ID_1", "root");
        node(root, "ID_2", "child");

        assertTrue(root.isExpandable());
        assertEquals(1, root.getNumChildMindmapNodes());
    }

    @Test
    public void takesItsTextFromTheRichTextContentWhenItHasNoTextAttribute() {

        MindmapNode node = node(null, "ID_1", null);
        node.addRichTextContent("<html><body><p>Hello <b>world</b></p></body></html>");

        assertEquals("Hello world", node.getText().trim());
    }

    @Test
    public void hasNoTextWhileItHasNeitherATextAttributeNorRichTextContent() {

        // a node that only holds an icon, or a node whose rich text has not been parsed yet
        MindmapNode node = node(null, "ID_1", null);

        assertNull(node.getText());
    }

    @Test
    public void searchFindsNodesAnywhereInTheSubtree() {

        MindmapNode root = node(null, "ID_1", "root");
        MindmapNode branch = node(root, "ID_2", "branch");
        MindmapNode leaf = node(branch, "ID_3", "the needle is here");
        node(branch, "ID_4", "something else");

        List<MindmapNode> found = root.search("needle");

        assertEquals(1, found.size());
        assertEquals(leaf, found.get(0));
    }

    @Test
    public void searchIgnoresCase() {

        MindmapNode root = node(null, "ID_1", "Root Node");

        assertEquals(1, root.search("root node").size());
        assertEquals(1, root.search("ROOT").size());
    }

    @Test
    public void searchSkipsNodesThatHaveNoText() {

        // regression: this used to throw a NullPointerException and take the whole app down
        MindmapNode root = node(null, "ID_1", "root");
        node(root, "ID_2", null);
        MindmapNode leaf = node(root, "ID_3", "findme");

        List<MindmapNode> found = root.search("findme");

        assertEquals(1, found.size());
        assertEquals(leaf, found.get(0));
    }

    @Test
    public void collectsIconsAndRichTextContentsThatArriveAfterTheNodeWasCreated() {

        // the parser sees <node> first and its <icon> and <richcontent> children only afterwards
        MindmapNode node = node(null, "ID_1", "text");

        assertTrue(node.getIconNames().isEmpty());
        assertTrue(node.getRichTextContents().isEmpty());

        node.addIconName("button_ok");
        node.addIconName("full-1");
        node.addRichTextContent("<html><body>note</body></html>");

        assertEquals(List.of("button_ok", "full-1"), node.getIconNames());
        assertEquals(1, node.getRichTextContents().size());
    }

    @Test
    public void keepsTheLinkItWasCreatedWith() {

        MindmapNode node = new MindmapNode(
                mindmap, null, "ID_1", 1, "linked", Uri.parse("http://example.com/x"), null);

        assertEquals("http://example.com/x", node.getLink().toString());
    }
}
