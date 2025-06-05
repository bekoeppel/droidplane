package ch.benediktkoeppel.code.droidplane.controller;

import android.text.Html;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
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

    HtmlEntitySanitizingInputStream(InputStream in, MainActivity activity) {
        // The pushback buffer should be able to hold the longest replacement we
        // ever push back. Numeric representations of multi-byte characters can
        // be quite long, so allocate a generous buffer.
        super(new PushbackInputStream(in, 32));
        this.pushback = (PushbackInputStream) super.in;
        this.mainActivity = activity;
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

    private byte[] toNumericBytes(String text) {
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

