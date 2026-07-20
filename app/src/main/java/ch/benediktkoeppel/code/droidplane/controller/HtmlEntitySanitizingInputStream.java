package ch.benediktkoeppel.code.droidplane.controller;

import android.text.Html;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.R;

/**
 * InputStream wrapper that replaces unsupported HTML entities in a streaming
 * fashion. It attempts to decode HTML named entities and emits their numeric
 * representation so that the XML parser can consume the document without
 * failing. This is done without loading the whole file into memory.
 */
class HtmlEntitySanitizingInputStream extends FilterInputStream {

    /**
     * Maximum length of an HTML entity name we try to read. The longest HTML5 entity name
     * ("CounterClockwiseContourIntegral") is 31 characters, plus the closing semicolon.
     */
    private static final int MAX_ENTITY_LENGTH = 32;

    /**
     * Size of the pushback buffer. It has to hold the longest replacement we ever push back: an entity that decodes
     * to several code points becomes one numeric reference of up to 10 bytes per code point.
     */
    private static final int PUSHBACK_BUFFER_SIZE = 512;

    private final PushbackInputStream pushback;
    private final MainActivity mainActivity;
    private boolean warned = false;

    /**
     * Decoding an entity means running a full HTML parse, and mindmaps tend to contain the same few entities
     * thousands of times, so we remember what we replaced an entity name with.
     */
    private final Map<String, byte[]> replacementCache = new HashMap<>();

    private static final String[] XML_BUILTINS = {"lt", "gt", "amp", "apos", "quot"};


    HtmlEntitySanitizingInputStream(InputStream in, MainActivity activity) {
        // Wrap the stream in a buffer so that the byte-by-byte processing below
        // does not trigger actual disk reads for every single byte. The
        // pushback stream allows us to "unread" the transformed entity bytes.
        super(new PushbackInputStream(new BufferedInputStream(in), PUSHBACK_BUFFER_SIZE));
        this.pushback = (PushbackInputStream) super.in;
        this.mainActivity = activity;
    }

    private boolean isXmlBuiltin(String name) {
        for (String b : XML_BUILTINS) {
            if (b.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumericEntity(String name) {
        if (!name.startsWith("#")) {
            return false;
        }
        String digits = name.substring(1);
        if (digits.startsWith("x") || digits.startsWith("X")) {
            digits = digits.substring(1);
            return digits.matches("[0-9A-Fa-f]+");
        }
        return digits.matches("[0-9]+");
    }

    private void notifyUser() {
        if (!warned && mainActivity != null) {
            warned = true;
            mainActivity.runOnUiThread(() ->
                    Toast.makeText(mainActivity,
                            R.string.invalid_xml_entities,
                            Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Convert the given text into UTF-8 bytes containing numeric character
     * references for each Unicode code point. This avoids issues with
     * surrogate pairs by working on full code points rather than 16‑bit
     * code units.
     */
    private byte[] toNumericBytes(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            String entity = "&#" + codePoint + ";";
            out.write(entity.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    @Override
    public int read() throws IOException {
        int b = pushback.read();
        if (b != '&') {
            return b;
        }

        byte[] nameBuf = new byte[MAX_ENTITY_LENGTH];
        int n = 0;
        int ch;
        while (n < nameBuf.length && (ch = pushback.read()) != -1) {
            nameBuf[n++] = (byte) ch;
            if (ch == ';') {
                break;
            }
        }

        String entity = new String(nameBuf, 0, n, StandardCharsets.UTF_8);
        if (n > 0 && entity.endsWith(";")) {
            String name = entity.substring(0, entity.length() - 1);

            byte[] replacement = getReplacement(name);
            if (replacement != null) {
                notifyUser();
                pushback.unread(replacement);
                return pushback.read();
            }
        }

        // nothing to replace: put the bytes we consumed back and hand out the ampersand
        if (n > 0) {
            pushback.unread(nameBuf, 0, n);
        }
        return '&';
    }

    /**
     * Returns the bytes that should replace the entity "&amp;name;", or null if it can stay as it is.
     *
     * @param name the entity name, without the leading ampersand and the trailing semicolon
     */
    private byte[] getReplacement(String name) throws IOException {

        // built-in XML entities and numeric references are understood by the XML parser
        if (isXmlBuiltin(name) || isNumericEntity(name)) {
            return null;
        }

        if (replacementCache.containsKey(name)) {
            return replacementCache.get(name);
        }

        String encoded = "&" + name + ";";
        String decoded = Html.fromHtml(encoded).toString();

        byte[] replacement;
        if (decoded.isEmpty() || encoded.equals(decoded)) {

            // We could not decode this - either it is an entity that Html does not know, or it is not an entity at
            // all but a literal ampersand (e.g. "AT&T; more text"). Escaping the ampersand keeps the document
            // well-formed for the XML parser and keeps the text visible. Dropping it, which is what emitting the
            // empty decoding would do, would silently delete content.
            replacement = ("&amp;" + name + ";").getBytes(StandardCharsets.UTF_8);

        } else {
            replacement = toNumericBytes(decoded);
        }

        replacementCache.put(name, replacement);
        return replacement;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = 0;
        for (; i < len; i++) {
            int c = read();
            if (c == -1) {
                return i == 0 ? -1 : i;
            }
            b[off + i] = (byte) c;
        }
        return i;
    }
}

