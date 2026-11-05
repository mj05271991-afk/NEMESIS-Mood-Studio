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
 private WebView web; private Handler mainHandler;
 private boolean probePending; private BroadcastReceiver smlReceiver;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);mainHandler=new Handler();
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);web.setWebChromeClient(new WebChromeClient());
  smlReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
   if(!probePending||intent==null)return;
   try{byte[] status=intent.getByteArrayExtra(Ev6LightingProbe.STATUS_EXTRA);Ev6LightingProbe.decodeStatus(status);Ev6LightingController.capture(status);
    finishProbe("STATUS CAPTURED - OUTPUT LOCKED","Received and validated the EV6 lighting status. This audit build does not send color or brightness writes.");
   }catch(Exception e){finishProbe("LIGHT CONTROL BLOCKED",e.getMessage());}
  }};
  registerReceiver(smlReceiver,new IntentFilter(Ev6LightingProbe.STATUS_ACTION));
  web.addJavascriptInterface(new VehicleBridge(),"NemesisVehicle");
  setContentView(web);web.loadUrl("file:///android_asset/index.html");
 }
 private void beginConnect(){
  if(probePending)return;
  if(Ev6LightingController.enabled()){finishProbe("SESSION ALREADY ACTIVE","The original EV6 lighting state is already saved. Restore the lighting before starting another session.");return;}
  String mismatch=Ev6LightingProbe.firmwareMismatch();
  if(mismatch!=null){finishProbe("SAFETY GATE BLOCKED",mismatch);return;}
  probePending=true;callJavascript("window.onVehicleProbeResult('REQUESTING','Reading the current EV6 lighting state before enabling control…')");
  mainHandler.postDelayed(new Runnable(){@Override public void run(){if(probePending)finishProbe("NO RESPONSE","No status response within 4 seconds. No lighting changes were sent.");}},4000);
  new Thread(new Runnable(){@Override public void run(){try{Ev6LightingProbe.requestStatus();}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){finishProbe("CONNECT FAILED",e.getMessage());}});}}},"NemesisVehicleConnect").start();
 }
 private void finishProbe(String state,String detail){probePending=false;callJavascript("window.onVehicleProbeResult('"+jsQuote(state)+"','"+jsQuote(detail==null?"No details":detail)+"')");}
 private String jsQuote(String s){if(s==null)return "Unknown error";return s.replace("\\","\\\\").replace("'","\\'").replace("\r"," ").replace("\n"," ");}
 private void callJavascript(String s){if(web!=null)web.loadUrl("javascript:"+s);}
 private void vehicleError(Exception e){callJavascript("window.onVehicleWriteError('"+jsQuote(e.getMessage())+"')");}
 private class VehicleBridge{
  @JavascriptInterface public void connect(){runOnUiThread(new Runnable(){@Override public void run(){beginConnect();}});}
  @JavascriptInterface public void setColor(final String hex){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.color(hex);}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLColor").start();}
  @JavascriptInterface public void setBrightness(final int percent){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.brightness(percent);}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLBrightness").start();}
  @JavascriptInterface public void release(){new Thread(new Runnable(){@Override public void run(){try{Ev6LightingController.restore();runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onVehicleReleased('Original EV6 color and brightness restored.')");}});}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}}},"NemesisSMLRestore").start();}
 }
 @Override protected void onDestroy(){
  probePending=false;mainHandler.removeCallbacksAndMessages(null);
  try{Ev6LightingController.restore();}catch(Exception ignored){}
  if(smlReceiver!=null){try{unregisterReceiver(smlReceiver);}catch(Exception ignored){}}
  if(web!=null)web.destroy();super.onDestroy();
 }
}