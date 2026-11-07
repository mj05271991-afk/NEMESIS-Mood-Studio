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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
  web.addJavascriptInterface(new NativeDaemonBridge(),"MoonDaemon");web.addJavascriptInterface(new MoodLightingBridge(),"MoodLighting");web.addJavascriptInterface(new AudioBridge(),"NemesisAudio");web.addJavascriptInterface(new VehicleBridge(),"NemesisVehicle");
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
 private class NativeDaemonBridge{
  private File settingsDir(){return new File(getFilesDir(),"settings");}
  private void write(String key,String value) throws IOException {
   File dir=settingsDir();
   if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Cannot create native settings directory");
   File temp=new File(dir,key+".tmp");
   FileOutputStream out=new FileOutputStream(temp);
   try{out.write((value+"\n").getBytes("UTF-8"));out.getFD().sync();}finally{out.close();}
   File target=new File(dir,key);
   if(!temp.renameTo(target))throw new IOException("Cannot save "+key);
  }
  @JavascriptInterface public boolean isEnabled(){
   File f=new File(settingsDir(),"enabled");
   if(!f.isFile())return false;
   java.io.BufferedReader reader=null;
   try{reader=new java.io.BufferedReader(new java.io.FileReader(f));return "1".equals(reader.readLine());}
   catch(IOException ignored){return false;}
   finally{if(reader!=null)try{reader.close();}catch(IOException ignored){}}
  }
  @JavascriptInterface public void configure(String requestedMode,boolean enabled){
   String mode=requestedMode==null?"":requestedMode.toLowerCase(java.util.Locale.US);
   if(!mode.equals("beat")&&!mode.equals("spectrum")&&!mode.equals("pulse")){
    mode="beat";
   }
   try{
    write("mode",mode);
    write("enabled",enabled?"1":"0");
   }catch(final IOException e){
    runOnUiThread(new Runnable(){@Override public void run(){
     callJavascript("window.onNativeAudioError('"+jsQuote(e.getMessage())+"')");
    }});
   }
  }
 }
 private class MoodLightingBridge{
  private File dir(){return new File(getFilesDir(),"settings");}
  private String read(String name,String fallback){
   java.io.BufferedReader in=null;
   try{in=new java.io.BufferedReader(new java.io.FileReader(new File(dir(),name)));String v=in.readLine();return v==null?fallback:v.trim();}
   catch(IOException e){return fallback;}
   finally{if(in!=null)try{in.close();}catch(IOException ignored){}}
  }
  private String write(String name,String value){
   File folder=dir();if(!folder.isDirectory()&&!folder.mkdirs())return "settings directory unavailable";
   File tmp=new File(folder,name+".tmp");
   try{
    FileOutputStream out=new FileOutputStream(tmp);
    try{out.write((value+"\n").getBytes("UTF-8"));out.getFD().sync();}finally{out.close();}
    if(!tmp.renameTo(new File(folder,name)))return "settings rename failed";
    return "ok";
   }catch(IOException e){return "settings write failed";}
  }
  @JavascriptInterface public String getState(){
   String m=read("mode","beat");if(!m.equals("beat")&&!m.equals("spectrum")&&!m.equals("pulse"))m="beat";
   int lo=1,hi=8;
   try{lo=Integer.parseInt(read("brightness_min","1"));}catch(NumberFormatException ignored){}
   try{hi=Integer.parseInt(read("brightness_max","8"));}catch(NumberFormatException ignored){}
   return "{\"enabled\":"+("1".equals(read("enabled","0"))?"true":"false")+",\"mode\":\""+m+"\",\"brightnessMin\":"+lo+",\"brightnessMax\":"+hi+"}";
  }
  @JavascriptInterface public String setEnabled(boolean enabled){return write("enabled",enabled?"1":"0");}
  @JavascriptInterface public String setMode(String mode){
   if(!"beat".equals(mode)&&!"spectrum".equals(mode)&&!"pulse".equals(mode))return "unsupported mode";
   return write("mode",mode);
  }
  @JavascriptInterface public String setBrightness(int min,int max){
   if(min<0||max>10||min>max)return "brightness out of range";
   String r=write("brightness_min",Integer.toString(min));if(!"ok".equals(r))return r;
   return write("brightness_max",Integer.toString(max));
  }
 }
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