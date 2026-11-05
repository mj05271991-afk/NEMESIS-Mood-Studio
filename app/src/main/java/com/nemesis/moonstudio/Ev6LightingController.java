package com.nemesis.moonstudio;
import android.os.IBinder;
import java.lang.reflect.Method;

/** Direct, firmware-gated EV6 SML control. Writes are Park-only and restore the captured owner state. */
public final class Ev6LightingController {
 private static final int[] PALETTE={0x1043f4,0x102ef2,0x1020ef,0x211cef,0x2f1cef,0x401cef,0x521cef,0x641cef,0x781cef,0x881def,0x9b21ef,0xa923ef,0xb723ea,0xc121e0,0xcc1fd2,0xd51bc0,0xdd17ae,0xe41399,0xeb0e99,0xef0a74,0xf60660,0xfa0451,0xfe0145,0xff321d,0xff4423,0xff5027,0xff6930,0xff7835,0xff8f3e,0xffa646,0xffc04f,0xffd356,0xffe55c,0xfff362,0xfefd66,0xffffff,0xf4fd66,0xe8fd66,0xd9fd66,0xcbfd63,0xb7f962,0xa8f362,0x95eb62,0x84e562,0x75df62,0x68dd62,0x5dde69,0x51e176,0x47e785,0x3eee98,0x35f5a9,0x2cfabd,0x25facf,0x1efae1,0x19faed,0x14faf9,0x10f4fc,0x10e6fc,0x10d2fc,0x10bbfc,0x10a3fc,0x108afc,0x106ffc,0x1059f8};
 private static final boolean VEHICLE_OUTPUT_ENABLED=false; // Keep fail-closed until status provenance and crash-safe recovery are proven.
 private static Object svc; private static int[] saved; private static long last;
 private Ev6LightingController(){}
 private static Object system() throws Exception {
  if(svc!=null)return svc;
  Class<?> sm=Class.forName("android.os.ServiceManager");
  IBinder b=(IBinder)sm.getMethod("getService",String.class).invoke(null,Ev6LightingProbe.SERVICE_NAME);
  if(b==null)throw new Exception("EV6 system service unavailable");
  Class<?> stub=Class.forName("com.hkmc.micom.ISystemService$Stub");
  svc=stub.getMethod("asInterface",IBinder.class).invoke(null,b);
  if(svc==null)throw new Exception("ISystemService interface unavailable");
  return svc;
 }
 private static void safePark() throws Exception {
  String mismatch=Ev6LightingProbe.firmwareMismatch();
  if(mismatch!=null)throw new Exception("Safety gate: "+mismatch);
  Object s=system(); Object p=s.getClass().getMethod("getGearPStatus").invoke(s);
  if(!(p instanceof Number)||((Number)p).intValue()!=1)throw new Exception("Vehicle must be in Park for lighting changes");
 }
 private static void pace() throws Exception {long w=100-(android.os.SystemClock.elapsedRealtime()-last);if(last!=0&&w>0)Thread.sleep(w);last=android.os.SystemClock.elapsedRealtime();}
 private static byte[] two(int v){return new byte[]{(byte)(v>>8),(byte)v};}
 private static int nearest(int rgb){int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255,best=PALETTE[0];long d0=Long.MAX_VALUE;for(int c:PALETTE){int x=r-(c>>16&255),y=g-(c>>8&255),z=b-(c&255);long d=(long)x*x+(long)y*y+(long)z*z;if(d<d0){d0=d;best=c;}}return best;}
 public static synchronized void capture(byte[] s)throws Exception{
  if(saved!=null)throw new Exception("An EV6 lighting session is already active. Restore the original lighting before reconnecting.");
  if(s==null||s.length!=9)throw new Exception("Invalid EV6 lighting status");
  int r=(s[0]&255)<<8|s[1]&255,g=(s[2]&255)<<8|s[3]&255,b=(s[4]&255)<<8|s[5]&255,br=s[6]&255;
  if(r>511||g>511||b>511||br<1||br>11)throw new Exception("EV6 lighting status out of range");
  safePark();saved=new int[]{r,g,b,br};
 }
 public static synchronized boolean enabled(){return saved!=null;}
 public static synchronized void color(String hex)throws Exception{
  if(!VEHICLE_OUTPUT_ENABLED)throw new Exception("Vehicle output is disabled in this audit build until fresh status verification and crash-safe recovery are validated");
  if(saved==null)throw new Exception("Connect to the EV6 first");
  if(hex==null||!hex.matches("[0-9a-fA-F]{6}"))throw new Exception("Invalid RGB color");
  safePark();pace();int c=nearest(Integer.parseInt(hex,16));Object s=system();
  s.getClass().getMethod("setSMLRGB",byte[].class,byte[].class,byte[].class).invoke(s,two(c>>16&255),two(c>>8&255),two(c&255));
 }
 public static synchronized void brightness(int percent)throws Exception{
  if(!VEHICLE_OUTPUT_ENABLED)throw new Exception("Vehicle output is disabled in this audit build until fresh status verification and crash-safe recovery are validated");
  if(saved==null)throw new Exception("Connect to the EV6 first");
  if(percent<0||percent>100)throw new Exception("Brightness must be 0-100");
  safePark();pace();int raw=Math.round(percent/10f)+1;Object s=system();
  s.getClass().getMethod("setSMLConfiguration",int.class,int.class).invoke(s,5,raw);
 }
 public static synchronized void restore()throws Exception{
  if(saved==null)return;
  if(!VEHICLE_OUTPUT_ENABLED){saved=null;return;}safePark();pace();Object s=system();
  s.getClass().getMethod("setSMLRGB",byte[].class,byte[].class,byte[].class).invoke(s,two(saved[0]),two(saved[1]),two(saved[2]));
  s.getClass().getMethod("setSMLConfiguration",int.class,int.class).invoke(s,5,saved[3]);saved=null;
 }
}