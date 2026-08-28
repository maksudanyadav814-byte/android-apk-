package com.faststore.app;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

/**
 * Loads product / buy-now links inside the app via WebView.
 * - Same-domain (store) links stay inside the WebView.
 * - Known external apps (WhatsApp, Facebook, YouTube, Instagram, Telegram,
 *   tel:, mailto:, intent:// UPI/payment links) open in their own external app.
 * - Three-dot menu lets the user force-open the current page in an external browser or share it.
 * - Downloads (files/media) are handed off to the system DownloadManager.
 * - File upload / camera / voice input requests from web pages (e.g. "upload photo",
 *   "scan document", checkout forms) are supported like a real shopping app.
 */
public class WebViewActivity extends AppCompatActivity {

    private WebView webView;
    private String currentUrl;
    private ValueCallback<Uri[]> filePathCallback;

    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (filePathCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String dataString = result.getData().getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = result.getData().getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            });

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            grantResults -> { /* WebView will re-check on next request */ });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // Ask for camera + mic up front so WebView permission grants work smoothly
        requestRuntimePermissionsIfNeeded();

        Toolbar toolbar = findViewById(R.id.toolbarWeb);
        toolbar.setNavigationOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack(); else finish();
        });
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_more_vert));

        ProgressBar progressBar = findViewById(R.id.webProgress);
        webView = findViewById(R.id.webView);

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        currentUrl = url;
        if (title != null) toolbar.setTitle(title);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? android.view.View.GONE : android.view.View.VISIBLE);
            }

            @Override
            public void onReceivedTitle(WebView view, String pageTitle) {
                if (getIntent().getStringExtra("title") == null && pageTitle != null) {
                    toolbar.setTitle(pageTitle);
                }
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                // Allow camera/mic/media access requested by web content (e.g. media players, voice search)
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(WebViewActivity.this, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, @NonNull android.webkit.WebResourceRequest request) {
                String loadUrl = request.getUrl().toString();
                if (isExternalAppLink(loadUrl)) {
                    openExternally(loadUrl);
                    return true;
                }
                currentUrl = loadUrl;
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String finishedUrl) {
                super.onPageFinished(view, finishedUrl);
                currentUrl = finishedUrl;
            }
        });

        webView.setDownloadListener((dUrl, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(dUrl));
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Downloading file...");
                String fileName = URLUtil.guessFileName(dUrl, contentDisposition, mimeType);
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) dm.enqueue(request);
                Toast.makeText(this, "Download started ⬇️", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not start download", Toast.LENGTH_SHORT).show();
            }
        });

        if (url != null) {
            webView.loadUrl(url);
        }
    }

    private boolean isExternalAppLink(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.startsWith("tel:")
                || lower.startsWith("mailto:")
                || lower.startsWith("sms:")
                || lower.startsWith("intent:")
                || lower.startsWith("upi:")
                || lower.contains("wa.me")
                || lower.contains("whatsapp.com")
                || lower.contains("facebook.com")
                || lower.contains("fb.com")
                || lower.contains("youtube.com")
                || lower.contains("youtu.be")
                || lower.contains("instagram.com")
                || lower.contains("twitter.com")
                || lower.contains("x.com")
                || lower.contains("t.me")
                || lower.contains("telegram.me");
    }

    private void openExternally(String url) {
        try {
            Intent intent;
            if (url.toLowerCase().startsWith("intent:")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException | java.net.URISyntaxException e) {
            Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestRuntimePermissionsIfNeeded() {
        java.util.List<String> toRequest = new java.util.ArrayList<>();
        String[] needed = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };
        for (String perm : needed) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(perm);
            }
        }
        if (!toRequest.isEmpty()) {
            permissionLauncher.launch(toRequest.toArray(new String[0]));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_browser) {
            openExternally(currentUrl != null ? currentUrl : webView.getUrl());
            return true;
        } else if (id == R.id.action_share) {
            String shareUrl = currentUrl != null ? currentUrl : webView.getUrl();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            return true;
        } else if (id == R.id.action_reload) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
