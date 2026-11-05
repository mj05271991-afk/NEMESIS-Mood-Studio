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
 private boolean probePending; private BroadcastReceiver smlReceiver; private Runnable probeTimeout;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);mainHandler=new Handler();
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);web.setWebChromeClient(new WebChromeClient());
  smlReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
   if(!probePending||intent==null)return;
   try{byte[] status=intent.getByteArrayExtra(Ev6LightingProbe.STATUS_EXTRA);Ev6LightingProbe.decodeStatus(status);
    finishProbe("STATUS RECEIVED - OUTPUT LOCKED","Received and validated an SML status broadcast. Request correlation is not yet proven; this audit build sends no lighting writes.");
   }catch(Exception e){finishProbe("LIGHT CONTROL BLOCKED",e.getMessage());}
  }};
  registerReceiver(smlReceiver,new IntentFilter(Ev6LightingProbe.STATUS_ACTION));
  web.addJavascriptInterface(new VehicleBridge(),"NemesisVehicle");
  setContentView(web);web.loadUrl("file:///android_asset/index.html");
 }
 private void beginConnect(){
  if(probePending)return;
  String mismatch=Ev6LightingProbe.firmwareMismatch();
  if(mismatch!=null){finishProbe("SAFETY GATE BLOCKED",mismatch);return;}
  probePending=true;callJavascript("window.onVehicleProbeResult('REQUESTING','Requesting EV6 lighting status for read-only diagnostics…')");
  if(probeTimeout!=null)mainHandler.removeCallbacks(probeTimeout);
  probeTimeout=new Runnable(){@Override public void run(){if(probePending)finishProbe("NO RESPONSE","No status response within 4 seconds. No lighting changes were sent.");}};
  mainHandler.postDelayed(probeTimeout,4000);
  new Thread(new Runnable(){@Override public void run(){try{Ev6LightingProbe.requestStatus();}catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){if(probePending)finishProbe("CONNECT FAILED",e.getMessage());}});}}},"NemesisVehicleConnect").start();
 }
 private void finishProbe(String state,String detail){probePending=false;if(probeTimeout!=null){mainHandler.removeCallbacks(probeTimeout);probeTimeout=null;}callJavascript("window.onVehicleProbeResult('"+jsQuote(state)+"','"+jsQuote(detail==null?"No details":detail)+"')");}
 private String jsQuote(String s){if(s==null)return "Unknown error";return s.replace("\\","\\\\").replace("'","\\'").replace("\r"," ").replace("\n"," ");}
 private void callJavascript(String s){if(web!=null)web.loadUrl("javascript:"+s);}
 private void vehicleError(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onVehicleWriteError('"+jsQuote(e.getMessage())+"')");}});}
 private class VehicleBridge{
  // Defense in depth: this audit Activity never dispatches vehicle writes or release calls.
  private void readOnly(){vehicleError(new Exception("This diagnostic build is read-only. Vehicle lighting commands are disabled."));}
  @JavascriptInterface public void connect(){runOnUiThread(new Runnable(){@Override public void run(){beginConnect();}});}
  @JavascriptInterface public void setColor(String hex){readOnly();}
  @JavascriptInterface public void setBrightness(int percent){readOnly();}
  @JavascriptInterface public void release(){readOnly();}
 }
 @Override protected void onDestroy(){
  probePending=false;mainHandler.removeCallbacksAndMessages(null);
  // No session snapshot is acquired in this read-only diagnostic build; do not issue restore calls.
  if(smlReceiver!=null){try{unregisterReceiver(smlReceiver);}catch(Exception ignored){}}
  if(web!=null)web.destroy();super.onDestroy();
 }
}