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

import ch.benediktkoeppel.code.droidplane.MainActivity;
import ch.benediktkoeppel.code.droidplane.R;

/**
 * InputStream wrapper that replaces unsupported HTML entities in a streaming
 * fashion. It attempts to decode HTML named entities and emits their numeric
 * representation so that the XML parser can consume the document without
 * failing. This is done without loading the whole file into memory.
 */
class HtmlEntitySanitizingInputStream extends FilterInputStream {

    /** Maximum length of an HTML entity name we try to read. */
    private static final int MAX_ENTITY_LENGTH = 10;

    private final PushbackInputStream pushback;
    private final MainActivity mainActivity;
    private boolean warned = false;

    private static final String[] XML_BUILTINS = {"lt", "gt", "amp", "apos", "quot"};

    /** Simple fast-path for the non-breaking space entity. */
    private static final String NBSP = "nbsp";

    HtmlEntitySanitizingInputStream(InputStream in, MainActivity activity) {
        // The pushback buffer should be able to hold the longest replacement we
        // ever push back. Numeric representations of multi-byte characters can
        // be quite long, so allocate a generous buffer.
        // Wrap the stream in a buffer so that the byte-by-byte processing below
        // does not trigger actual disk reads for every single byte. The
        // pushback stream allows us to "unread" the transformed entity bytes.
        super(new PushbackInputStream(new BufferedInputStream(in), 32));
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

    private byte[] toNumericBytes(String text) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < text.length(); i++) {
            String entity = "&#" + (int) text.charAt(i) + ";";
            out.write(entity.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    @Override
    public int read() throws IOException {
        int b = pushback.read();
        if (b == '&') {
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

                if (isXmlBuiltin(name) || isNumericEntity(name)) {
                    // Built-in XML entities and numeric references are left as is.
                    pushback.unread(entity.getBytes(StandardCharsets.UTF_8));
                    return '&';
                }

                if (NBSP.equals(name)) {
                    notifyUser();
                    pushback.unread(toNumericBytes("\u00A0"));
                    return pushback.read();
                }

                String encoded = "&" + entity;
                String decoded = Html.fromHtml(encoded).toString();
                if (!encoded.equals(decoded)) {
                    notifyUser();
                    pushback.unread(toNumericBytes(decoded));
                    return pushback.read();
                }
            }

            if (n > 0) {
                pushback.unread(nameBuf, 0, n);
            }
            return '&';
        }
        return b;
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

