package com.nemesis.moonstudio;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;

public class MoonStudioActivity extends Activity {
    private WebView web;
    private AudioBandCapture capture;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        web.setWebChromeClient(new WebChromeClient());
        capture = new AudioBandCapture(new AudioBandCapture.Listener() {
            @Override public void onBands(final float bass, final float mid, final float treble) {
                runOnUiThread(new Runnable() { @Override public void run() {
                    if (web != null) web.loadUrl("javascript:window.onNativeBands(" + bass + "," + mid + "," + treble + ")");
                }});
            }
        });
        web.addJavascriptInterface(new AudioBridge(), "NemesisAudio");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    private class AudioBridge {
        @JavascriptInterface public void start() { if (capture != null) capture.start(); }
        @JavascriptInterface public void stop() { if (capture != null) capture.stop(); }
    }
    @Override protected void onDestroy() { if (capture != null) capture.stop(); if (web != null) web.destroy(); super.onDestroy(); }
}
