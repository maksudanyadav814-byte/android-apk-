package com.faststore.app;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/** Loads a bundled local HTML page (Privacy Policy / Terms / Contact Us / About). */
public class StaticPageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Toolbar toolbar = findViewById(R.id.toolbarWeb);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.webProgress).setVisibility(android.view.View.GONE);

        String title = getIntent().getStringExtra("title");
        String asset = getIntent().getStringExtra("asset");
        if (title != null) toolbar.setTitle(title);

        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false);
        if (asset != null) {
            webView.loadUrl("file:///android_asset/" + asset);
        }
    }
}
