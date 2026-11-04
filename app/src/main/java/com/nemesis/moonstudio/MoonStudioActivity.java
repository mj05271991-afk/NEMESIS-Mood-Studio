package com.nemesis.moonstudio;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.view.View;

public class MoonStudioActivity extends Activity {
    private WebView web;
    private AudioBandCapture capture;
    private Handler mainHandler;
    private boolean probePending;
    private BroadcastReceiver smlReceiver;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        mainHandler = new Handler();
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
        smlReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!probePending || intent == null) return;
                try {
                    byte[] status = intent.getByteArrayExtra(Ev6LightingProbe.STATUS_EXTRA);
                    finishProbe("RESPONSE RECEIVED", Ev6LightingProbe.decodeStatus(status));
                } catch (Exception e) {
                    finishProbe("INVALID RESPONSE", e.getMessage());
                }
            }
        };
        registerReceiver(smlReceiver, new IntentFilter(Ev6LightingProbe.STATUS_ACTION));
        web.addJavascriptInterface(new AudioBridge(), "NemesisAudio");
        web.addJavascriptInterface(new VehicleBridge(), "NemesisVehicle");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private void beginProbe() {
        if (probePending) return;
        String mismatch = Ev6LightingProbe.firmwareMismatch();
        if (mismatch != null) {
            finishProbe("SAFETY GATE BLOCKED", mismatch);
            return;
        }
        probePending = true;
        callJavascript("window.onVehicleProbeResult('REQUESTING','Asking the EV6 for read-only lighting status…')");
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (probePending) finishProbe("NO RESPONSE", "No lighting status broadcast arrived within 4 seconds.");
            }
        }, 4000);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    Ev6LightingProbe.requestStatus();
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            finishProbe("REQUEST FAILED", e.getMessage());
                        }
                    });
                }
            }
        }, "NemesisVehicleProbe").start();
    }

    private void finishProbe(String state, String detail) {
        if (probePending) probePending = false;
        callJavascript("window.onVehicleProbeResult('" + jsQuote(state) + "','" + jsQuote(detail == null ? "No details" : detail) + "')");
    }

    private String jsQuote(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", " ").replace("\n", " ");
    }

    private void callJavascript(String script) {
        if (web != null) web.loadUrl("javascript:" + script);
    }

    private class AudioBridge {
        @JavascriptInterface public void start() { if (capture != null) capture.start(); }
        @JavascriptInterface public void stop() { if (capture != null) capture.stop(); }
    }

    private class VehicleBridge {
        @JavascriptInterface public void checkVehicleLights() {
            runOnUiThread(new Runnable() {
                @Override public void run() { beginProbe(); }
            });
        }
    }

    @Override protected void onDestroy() {
        probePending = false;
        mainHandler.removeCallbacksAndMessages(null);
        if (smlReceiver != null) {
            try { unregisterReceiver(smlReceiver); } catch (Exception ignored) { }
        }
        if (capture != null) capture.stop();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
