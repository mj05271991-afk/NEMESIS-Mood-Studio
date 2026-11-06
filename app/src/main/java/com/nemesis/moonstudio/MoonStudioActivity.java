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
 private boolean probePending; private BroadcastReceiver smlReceiver;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);mainHandler=new Handler();
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);web.setWebChromeClient(new WebChromeClient());
  capture=new AudioBandCapture(new AudioBandCapture.Listener(){@Override public void onBands(final float bass,final float mid,final float treble){runOnUiThread(new Runnable(){@Override public void run(){if(web!=null)web.loadUrl("javascript:window.onNativeBands("+bass+","+mid+","+treble+")");}});}});
  smlReceiver=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){
   if(!probePending||intent==null)return;
   try{byte[] status=intent.getByteArrayExtra(Ev6LightingProbe.STATUS_EXTRA);Ev6LightingProbe.decodeStatus(status);Ev6LightingController.capture(status);
    finishProbe("LIGHT CONTROL READY","Connected directly to the EV6. Original color and brightness saved for restore. Changes are enabled only in Park.");
   }catch(Exception e){finishProbe("LIGHT CONTROL BLOCKED",e.getMessage());}
  }};
  registerReceiver(smlReceiver,new IntentFilter(Ev6LightingProbe.STATUS_ACTION));
  web.addJavascriptInterface(new MoonServiceBridge(),"MoonService");web.addJavascriptInterface(new AudioBridge(),"NemesisAudio");web.addJavascriptInterface(new VehicleBridge(),"NemesisVehicle");
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
 private void writeMoonSetting(String name,String value) throws Exception {
  if(!name.matches("[a-z_]+")||!value.matches("[a-zA-Z0-9#.,: _-]{1,256}"))throw new IllegalArgumentException("Invalid setting");
  java.io.File dir=new java.io.File(getFilesDir(),"settings");
  if(!dir.exists()&&!dir.mkdirs())throw new java.io.IOException("Settings directory unavailable");
  java.io.File temp=new java.io.File(dir,name+".tmp");
  java.io.File target=new java.io.File(dir,name);
  java.io.FileOutputStream out=new java.io.FileOutputStream(temp);
  try{out.write((value+"\\n").getBytes("UTF-8"));out.getFD().sync();}finally{out.close();}
  if(!temp.renameTo(target))throw new java.io.IOException("Settings update failed");
 }
 private class MoonServiceBridge{
  @JavascriptInterface public void stopBackground(){
   new Thread(new Runnable(){@Override public void run(){
    try{
     writeMoonSetting("enabled","0");
     java.io.File marker=new java.io.File(getFilesDir(),"enable_moon_lighting");
     if(marker.exists()&&!marker.delete())throw new java.io.IOException("Could not clear service start marker");
     runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onMoonServiceConfigured('Background lighting disabled in settings. An already-running HMG service must be stopped by its manager.')");}});
    }catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}
   }},"MoonStopBackground").start();
  }
  @JavascriptInterface public void configure(final String mode,final int brightness){
   new Thread(new Runnable(){@Override public void run(){
    try{
     String m=mode.toLowerCase(java.util.Locale.US);
     if(!m.equals("beat")&&!m.equals("spectrum")&&!m.equals("pulse")&&!m.equals("speed"))throw new IllegalArgumentException("Unsupported music mode");
     writeMoonSetting("mode",m);
     writeMoonSetting("brightness_max",String.valueOf(Math.max(0,Math.min(10,brightness/10))));
     writeMoonSetting("enabled","0");
     runOnUiThread(new Runnable(){@Override public void run(){callJavascript("window.onMoonServiceConfigured('Settings saved. Background lighting remains disabled pending vehicle validation.')");}});
    }catch(final Exception e){runOnUiThread(new Runnable(){@Override public void run(){vehicleError(e);}});}
   }},"MoonServiceSettings").start();
  }
 }
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
  if(capture!=null)capture.stop();if(web!=null)web.destroy();super.onDestroy();
 }
}