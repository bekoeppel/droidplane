package ch.benediktkoeppel.code.droidplane.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The sanitizer rewrites the HTML entities that mindmap files contain but XML does not know, so that the pull parser
 * does not abort on them. Its job is to never lose text and to never hand the parser something it can not read.
 */
@RunWith(RobolectricTestRunner.class)
public class HtmlEntitySanitizingInputStreamTest {

    /**
     * Runs the given text through the sanitizer, reading it one byte at a time
     */
    private String sanitize(String input) throws IOException {

        InputStream in = new HtmlEntitySanitizingInputStream(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            out.write(b);
        }
        return out.toString("UTF-8");
    }

    /**
     * Runs the given text through the sanitizer using bulk reads
     */
    private String sanitizeBulk(String input) throws IOException {

        InputStream in = new HtmlEntitySanitizingInputStream(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[7];
        int read;
        while ((read = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toString("UTF-8");
    }

    /**
     * Returns the concatenated text content of an XML document, and fails if it can not be parsed
     */
    private String textContentOf(String xml) throws Exception {

        XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
        xpp.setInput(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "UTF-8");

        StringBuilder text = new StringBuilder();
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.TEXT) {
                text.append(xpp.getText());
            }
            eventType = xpp.next();
        }
        return text.toString();
    }

    @Test
    public void passesPlainTextThrough() throws IOException {

        assertEquals("<node TEXT=\"hello world\"/>", sanitize("<node TEXT=\"hello world\"/>"));
    }

    @Test
    public void keepsTheEntitiesThatXmlItselfDefines() throws IOException {

        // rewriting these would be pointless, and getting it wrong would corrupt the document
        assertEquals("&amp;&lt;&gt;&quot;&apos;", sanitize("&amp;&lt;&gt;&quot;&apos;"));
    }

    @Test
    public void keepsNumericCharacterReferences() throws IOException {

        assertEquals("&#160;&#xA0;", sanitize("&#160;&#xA0;"));
    }

    @Test
    public void rewritesHtmlEntitiesAsNumericReferences() throws IOException {

        // &nbsp; is the reason this class exists: it is valid HTML, but an XML parser aborts on it
        assertEquals("a&#160;b", sanitize("a&nbsp;b"));
    }

    @Test
    public void rewrittenDocumentIsReadableByTheXmlParser() throws Exception {

        String sanitized = sanitize("<n>a&nbsp;b&mdash;c</n>");

        assertEquals("a b—c", textContentOf(sanitized));
    }

    @Test
    public void doesNotDropEntitiesItCanNotDecode() throws Exception {

        // regression: an entity that Html can not decode used to be replaced by its empty decoding, i.e. the text
        // silently disappeared from the document
        String sanitized = sanitize("<n>before&notarealentity;after</n>");

        String text = textContentOf(sanitized);
        assertTrue("expected the text around the entity to survive, got: " + text,
                text.startsWith("before") && text.endsWith("after"));
        assertTrue("expected the entity itself not to be dropped, got: " + text, text.length() > "beforeafter".length());
    }

    @Test
    public void keepsALiteralAmpersandParseable() throws Exception {

        // a bare "&" is not valid XML. It has to survive as text, and the document has to stay readable.
        String sanitized = sanitize("<n>Tom & Jerry</n>");

        assertEquals("Tom & Jerry", textContentOf(sanitized));
    }

    @Test
    public void keepsALiteralAmpersandThatIsFollowedByASemicolon() throws Exception {

        // this one looks like an entity to the scanner, which is what makes it interesting
        String sanitized = sanitize("<n>a & b; c</n>");

        assertEquals("a & b; c", textContentOf(sanitized));
    }

    @Test
    public void doesNotLoseTextAfterAnAmpersandWithoutASemicolon() throws Exception {

        // the scanner reads ahead looking for the semicolon and has to put back everything it consumed
        assertEquals("AT&amp;T and some more text", sanitize("AT&T and some more text"));
        assertEquals("AT&T and some more text", textContentOf("<n>" + sanitize("AT&T and some more text") + "</n>"));
    }

    @Test
    public void handlesAnAmpersandAtTheVeryEndOfTheDocument() throws IOException {

        assertEquals("trailing&amp;", sanitize("trailing&"));
    }

    @Test
    public void handlesEntityNamesLongerThanTheScanWindow() throws Exception {

        // the scanner only reads a limited number of bytes ahead. Whatever it consumed has to come back out, and
        // the ampersand has to be escaped so that the parser can still read the document.
        String tooLong = "&" + "a".repeat(60) + ";";

        assertEquals("&amp;" + "a".repeat(60) + ";", sanitize(tooLong));
        assertEquals(tooLong, textContentOf("<n>" + sanitize(tooLong) + "</n>"));
    }

    @Test
    public void keepsMultiByteCharacters() throws IOException {

        assertEquals("Zürich – Grüße 日本語", sanitize("Zürich – Grüße 日本語"));
    }

    @Test
    public void bulkReadsGiveTheSameResultAsSingleByteReads() throws IOException {

        String input = "<n>a&nbsp;b</n><n>Tom & Jerry &mdash; ok</n>";

        assertEquals(sanitize(input), sanitizeBulk(input));
    }

    @Test
    public void handlesAnEntityThatDecodesToSeveralCharacters() throws Exception {

        // the replacement is pushed back into the stream, and used to overflow the pushback buffer
        String sanitized = sanitize("<n>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</n>");

        assertEquals(" ".repeat(10), textContentOf(sanitized));
    }
}
