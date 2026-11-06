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
 private WebView web; private AudioBandCapture capture; private Handler mainHandler;
 private boolean probePending; private volatile boolean uiLightingChanged; private BroadcastReceiver smlReceiver;
 private long lastBandUiMs;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);mainHandler=new Handler();
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);web.setWebChromeClient(new WebChromeClient());
  capture=new AudioBandCapture(new AudioBandCapture.Listener(){@Override public void onBands(final float bass,final float mid,final float treble){if(android.os.SystemClock.uptimeMillis()-lastBandUiMs<100)return;
   lastBandUiMs=android.os.SystemClock.uptimeMillis();
   runOnUiThread(new Runnable(){@Override public void run(){if(web!=null)web.evaluateJavascript("window.onNativeBands("+bass+","+mid+","+treble+")",null);}});}
   @Override public void onError(final String message){runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onNativeAudioError(\'"+jsQuote(message)+"\')");}});}
  });
  smlReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
   if(!probePending||intent==null)return;
   try{byte[] status=intent.getByteArrayExtra(Ev6LightingProbe.STATUS_EXTRA);Ev6LightingProbe.decodeStatus(status);Ev6LightingController.capture(status);
    finishProbe("LIGHT CONTROL READY","Connected directly to the EV6. Original color and brightness saved for restore. Changes are enabled only in Park.");
   }catch(Exception e){finishProbe("LIGHT CONTROL BLOCKED",e.getMessage());}
  }};
  registerReceiver(smlReceiver,new IntentFilter(Ev6LightingProbe.STATUS_ACTION));
  web.addJavascriptInterface(new AudioBridge(),"NemesisAudio");web.addJavascriptInterface(new VehicleBridge(),"NemesisVehicle");
  setContentView(web);web.loadUrl("file:///android_asset/index.html");
 }
 private void beginConnect(){
  if(probePending)return;String mismatch=Ev6LightingProbe.firmwareMismatch();
  if(mismatch!=null){finishProbe("SAFETY GATE BLOCKED",mismatch);return;}
  probePending=true;callJavascript("window.onVehicleProbeResult('REQUESTING','Reading the current EV6 lighting state before enabling control…')");
  mainHandler.postDelayed(new Runnable(){@Override public void run(){if(probePending)finishProbe("NO RESPONSE","No status response within 4 seconds. No lighting changes were sent.");}},4000);
  new Thread(new Runnable(){@Override public void run(){try{Ev6LightingProbe.requestStatus();}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){finishProbe("CONNECT FAILED",e.getMessage());}});}}},"NemesisVehicleConnect").start();
 }
 private void finishProbe(String state,String detail){probePending=false;callJavascript("window.onVehicleProbeResult('"+jsQuote(state)+"','"+jsQuote(detail==null?"No details":detail)+"')");}
 private String jsQuote(String s){if(s==null)return "Unknown error";return s.replace("\\","\\\\").replace("'","\\'").replace("\r"," ").replace("\n"," ");}
 private void callJavascript(String s){if(web!=null)web.loadUrl("javascript:"+s);}
 private void vehicleError(Exception e){callJavascript("window.onVehicleWriteError('"+jsQuote(e.getMessage())+"')");}
 private class AudioBridge{
  @JavascriptInterface public void start(){if(capture!=null)capture.start();}
  @JavascriptInterface public void stop(){if(capture!=null)capture.stop();}
 }
 private class VehicleBridge{
  @JavascriptInterface public void connect(){runOnUiThread(new Runnable(){@Override public void run(){beginConnect();}});}
  @JavascriptInterface public void setColor(final String hex){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.color(hex);uiLightingChanged=true;}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLColor").start();}
  @JavascriptInterface public void setBrightness(final int percent){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.brightness(percent);uiLightingChanged=true;}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLBrightness").start();}
  @JavascriptInterface public void release(){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.restore();uiLightingChanged=false;runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onVehicleReleased('Original EV6 color and brightness restored.')");}});}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLRestore").start();}
 }
 @Override protected void onDestroy(){
  probePending=false;mainHandler.removeCallbacksAndMessages(null);
  if(uiLightingChanged){try{Ev6LightingController.restore();}catch(Exception ignored){}}
  if(smlReceiver!=null){try{unregisterReceiver(smlReceiver);}catch(Exception ignored){}}
  if(capture!=null)capture.stop();if(web!=null)web.destroy();super.onDestroy();
 }
}