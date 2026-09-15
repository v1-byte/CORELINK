package com.v1byte.corelink;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.content.Context;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getWindow().setStatusBarColor(Color.rgb(7, 10, 18));
            getWindow().setNavigationBarColor(Color.rgb(7, 10, 18));
            webView = new WebView(getApplicationContext());
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            webView.setBackgroundColor(Color.rgb(7, 10, 18));
            webView.setWebViewClient(new WebViewClient() {
                @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request.isForMainFrame()) showFallback(MainActivity.this, "CORELINK gagal memuat tampilan. Silakan buka ulang aplikasi.");
                }
            });
            CookieManager.getInstance().setAcceptCookie(true);
            setContentView(webView, new ViewGroup.LayoutParams(-1, -1));
            webView.loadUrl("file:///android_asset/index.html");
        } catch (Throwable error) {
            showFallback(this, "CORELINK tidak dapat dimulai pada perangkat ini.");
        }
    }

    private void showFallback(Context context, String message) {
        TextView screen = new TextView(context);
        screen.setText("CORELINK\n\n" + message + "\n\nPastikan Android System WebView aktif dan coba buka kembali.");
        screen.setTextColor(Color.rgb(220, 235, 250));
        screen.setTextSize(16);
        screen.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(42, 42, 42, 42);
        screen.setBackgroundColor(Color.rgb(7, 10, 18));
        setContentView(screen, new ViewGroup.LayoutParams(-1, -1));
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
