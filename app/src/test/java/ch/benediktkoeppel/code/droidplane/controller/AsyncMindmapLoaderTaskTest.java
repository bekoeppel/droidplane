package ch.benediktkoeppel.code.droidplane.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.model.Mindmap;
import ch.benediktkoeppel.code.droidplane.model.MindmapNode;

/**
 * Tests the streaming XML parse that turns a .mm file into the mindmap tree.
 */
@RunWith(RobolectricTestRunner.class)
public class AsyncMindmapLoaderTaskTest {

    private Mindmap mindmap;
    private MainActivity mainActivity;

    @Before
    public void setUp() {

        mindmap = new Mindmap();

        // we only need the activity as a context and as something to post to - not a created, visible activity
        mainActivity = Robolectric.buildActivity(MainActivity.class).get();
    }

    /**
     * Parses a mindmap document and returns its root node
     */
    private MindmapNode load(String document) {

        AsyncMindmapLoaderTask task = new AsyncMindmapLoaderTask(
                mainActivity,
                (loadedMindmap, rootNode) -> { },
                mindmap,
                new Intent()
        );

        task.loadDocument(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));

        return mindmap.getRootNode();
    }

    private MindmapNode child(MindmapNode parent, int index) {
        return parent.getChildMindmapNodes().get(index);
    }

    @Test
    public void buildsTheNodeTree() {

        MindmapNode root = load(
                "<map version='freeplane 1.9.13'>"
                        + "<node TEXT='root' ID='ID_1'>"
                        + "  <node TEXT='first' ID='ID_2'>"
                        + "    <node TEXT='grandchild' ID='ID_4'/>"
                        + "  </node>"
                        + "  <node TEXT='second' ID='ID_3'/>"
                        + "</node>"
                        + "</map>");

        assertEquals("root", root.getText());
        assertEquals(2, root.getNumChildMindmapNodes());
        assertEquals("first", child(root, 0).getText());
        assertEquals("second", child(root, 1).getText());
        assertEquals("grandchild", child(child(root, 0), 0).getText());
        assertEquals(root, child(root, 0).getParentNode());
    }

    @Test
    public void indexesNodesByTheirId() {

        load("<map><node TEXT='root' ID='ID_1'><node TEXT='child' ID='ID_77'/></node></map>");

        assertEquals("child", mindmap.getNodeByID("ID_77").getText());
        assertEquals("child", mindmap.getNodeByNumericID(77).getText());
        assertTrue(mindmap.isLoaded());
    }

    @Test
    public void readsIconsAndStyles() {

        MindmapNode root = load(
                "<map><node TEXT='root' ID='ID_1'>"
                        + "<font BOLD='true' ITALIC='true'/>"
                        + "<icon BUILTIN='button_ok'/>"
                        + "<icon BUILTIN='full-1'/>"
                        + "</node></map>");

        assertTrue(root.isBold());
        assertTrue(root.isItalic());
        assertEquals(List.of("button_ok", "full-1"), root.getIconNames());
    }

    @Test
    public void readsRichTextContent() {

        MindmapNode root = load(
                "<map><node ID='ID_1'>"
                        + "<richcontent TYPE='NODE'><html><body><p>Hello <b>world</b></p></body></html></richcontent>"
                        + "</node></map>");

        assertEquals(1, root.getRichTextContents().size());
        assertTrue(root.getRichTextContents().get(0).contains("<b>world</b>"));

        // a node without a TEXT attribute shows its rich text instead
        assertEquals("Hello world", root.getText().trim());
    }

    @Test
    public void escapesMarkupInRichTextContentSoThatItSurvives() {

        // regression: the parser hands us decoded text, and writing it back raw produced broken markup
        MindmapNode root = load(
                "<map><node ID='ID_1'>"
                        + "<richcontent TYPE='NODE'><html><body>"
                        + "<p title='say &quot;hi&quot;'>Tom &amp; Jerry &lt;not a tag&gt;</p>"
                        + "</body></html></richcontent>"
                        + "</node></map>");

        String richText = root.getRichTextContents().get(0);

        assertTrue("ampersand has to stay escaped: " + richText, richText.contains("Tom &amp; Jerry"));
        assertTrue("angle brackets have to stay escaped: " + richText, richText.contains("&lt;not a tag&gt;"));
        assertTrue("quotes in attributes have to stay escaped: " + richText, richText.contains("&quot;hi&quot;"));

        // and the text of the node is the readable version of all that
        assertEquals("Tom & Jerry <not a tag>", root.getText().trim());
    }

    @Test
    public void linksNodesThatPointAtEachOtherWithAnArrow() {

        MindmapNode root = load(
                "<map><node TEXT='root' ID='ID_1'>"
                        + "<node TEXT='from' ID='ID_2'><arrowlink DESTINATION='ID_3'/></node>"
                        + "<node TEXT='to' ID='ID_3'/>"
                        + "</node></map>");

        MindmapNode from = mindmap.getNodeByID("ID_2");
        MindmapNode to = mindmap.getNodeByID("ID_3");

        assertEquals(List.of(to), from.getArrowLinkDestinationNodes());

        // the link is known at both ends, so the context menu of the target offers it too
        assertEquals(List.of(from), to.getArrowLinkIncomingNodes());
    }

    @Test
    public void readsAMindmapThatUsesHtmlEntities() {

        // &nbsp; is not valid XML - the sanitizer in front of the parser deals with it
        MindmapNode root = load("<map><node TEXT='a&nbsp;b' ID='ID_1'/></map>");

        assertEquals("a b", root.getText());
    }

    @Test
    public void acceptsARichContentTagWithoutATypeAttribute() {

        // regression: this used to throw a NullPointerException and abort the rest of the document
        MindmapNode root = load(
                "<map><node TEXT='root' ID='ID_1'>"
                        + "<richcontent><html><body>whatever</body></html></richcontent>"
                        + "<node TEXT='after' ID='ID_2'/>"
                        + "</node></map>");

        assertEquals("root", root.getText());
        assertEquals("after", child(root, 0).getText());
    }

    @Test
    public void acceptsANodeWithoutAnIdAttribute() {

        // regression: this used to throw a NullPointerException and abort the rest of the document
        MindmapNode root = load(
                "<map><node TEXT='root' ID='ID_1'>"
                        + "<node TEXT='no id here'/>"
                        + "<node TEXT='after' ID='ID_2'/>"
                        + "</node></map>");

        assertEquals(2, root.getNumChildMindmapNodes());
        assertEquals("after", child(root, 1).getText());
    }

    @Test
    public void showsWhatItCouldReadOfATruncatedDocument() {

        // this is what a mindmap looks like that Dropbox has not finished syncing
        MindmapNode root = load(
                "<map><node TEXT='root' ID='ID_1'>"
                        + "<node TEXT='first' ID='ID_2'/>"
                        + "<node TEXT='second' ID_");

        assertNotNull("we should still show the part of the document that we could read", root);
        assertEquals("root", root.getText());
        assertEquals("first", child(root, 0).getText());
    }

    @Test
    public void hasNoRootNodeForADocumentWithoutOne() {

        assertNull(load("<map version='freeplane 1.9.13'></map>"));
    }
}
