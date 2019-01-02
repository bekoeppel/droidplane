package ch.benediktkoeppel.code.droidplane;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class RichTextViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_text_view);

        // get data from intent
        String richTextContent = getIntent().getStringExtra("richTextContent");

        // set data of web view
        WebView webView = findViewById(R.id.webview);
        webView.loadData(richTextContent, "text/html", "UTF-8");

    }

}
