/*
 * Moon Studio EV6 ALSA capture prototype.
 * Standalone diagnostic; NOT installed or launched by the APK yet.
 * Never enable or change the vehicle mixer switch from this tool.
 * Capture only if an existing authorized loopback is already enabled.
 * Kyle's Light Vibes README identifies card0/device2, pcmC0D2c,
 * mixer switch "Device2 Tx to Rx Internal Loopback Mode", and 44.1/48 kHz.
 * This prototype does not enable that switch or manage call interruptions.
 * The 44.1/48 kHz rate is supplied by the caller, not auto-detected.
 * Android Auto support is listed as NOT MEASURED in Kyle's README.
 *
 * Usage: moon_audio_probe <verified-card> <verified-device> <verified-rate:44100|48000>
 * Emits BANDS bass mid treble (0..1) every ~100 ms.
 */
#include <tinyalsa/asoundlib.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <stdint.h>

#define MAX_RATE 48000
#define CHANNELS 2
#define MAX_FRAMES 4800
static volatile sig_atomic_t running=1;
static volatile sig_atomic_t stop_signal=0;
static void quit(int signum){stop_signal=signum;running=0;}
static float clamp01(double x){return x<0?0:x>1?1:(float)x;}
int main(int argc,char **argv){
 if(argc!=4){fprintf(stderr,"MOON_AUDIO_ERROR usage: moon_audio_probe <card> <device> <44100|48000>\n");return 64;}
 char *endCard=NULL,*endDevice=NULL;
 unsigned long cardValue=strtoul(argv[1],&endCard,10),deviceValue=strtoul(argv[2],&endDevice,10);
 if(!endCard||*endCard||!endDevice||*endDevice||cardValue>255||deviceValue>255){fprintf(stderr,"MOON_AUDIO_ERROR invalid card/device\n");return 64;}
 unsigned card=(unsigned)cardValue,device=(unsigned)deviceValue;
 char *endRate=NULL;unsigned long rateValue=strtoul(argv[3],&endRate,10);
 if(!endRate||*endRate||(rateValue!=44100&&rateValue!=48000)){fprintf(stderr,"MOON_AUDIO_ERROR unsupported rate; specify 44100 or 48000\n");return 64;}
 unsigned rate=(unsigned)rateValue;
 const int frames=(int)(rate/10);
 struct pcm_config cfg={0};
 cfg.channels=CHANNELS;cfg.rate=rate;cfg.period_size=1024;cfg.period_count=4;
 cfg.format=PCM_FORMAT_S16_LE;
 signal(SIGINT,quit);signal(SIGTERM,quit);
 struct pcm *pcm=pcm_open(card,device,PCM_IN,&cfg);
 if(!pcm||!pcm_is_ready(pcm)){
  fprintf(stderr,"MOON_AUDIO_ERROR open card%u device%u: %s\n",card,device,pcm?pcm_get_error(pcm):"null");
  if(pcm)pcm_close(pcm);return 2;
 }
 fprintf(stderr,"MOON_AUDIO_READY card%u device%u rate%u\n",card,device,rate);
 int16_t buf[MAX_FRAMES*CHANNELS];
 /* One-pole lowpass filters split the stream into low/mid/high components.
    This is a diagnostic energy estimator, not Kyle's production FFT. */
 const double alphaBass=1.0-exp(-2.0*3.141592653589793*250.0/rate);
 const double alphaMid=1.0-exp(-2.0*3.141592653589793*2000.0/rate);
 double low=0,lowmid=0;
 int exitCode=0;
 while(running){
  int got=0;
  while(running&&got<frames){
   int n=pcm_readi(pcm,buf+got*CHANNELS,(unsigned)(frames-got));
   if(n<=0){fprintf(stderr,"MOON_AUDIO_ERROR read: %s (returned %d)\n",pcm_get_error(pcm),n);exitCode=3;running=0;break;}
   got+=n;
  }
  if(got!=frames)break;
  double bass=0,mid=0,treble=0;
  for(int i=0;i<frames;i++){
   double sample=((double)buf[i*2]+buf[i*2+1])/(2.0*32768.0);
   low+=alphaBass*(sample-low);
   lowmid+=alphaMid*(sample-lowmid);
   double b=low,m=lowmid-low,t=sample-lowmid;
   bass+=b*b;mid+=m*m;treble+=t*t;
  }
  printf("BANDS %.4f %.4f %.4f\n",clamp01(sqrt(bass/frames)*8),
   clamp01(sqrt(mid/frames)*8),clamp01(sqrt(treble/frames)*8));
  fflush(stdout);
 }
 pcm_close(pcm);
 if(stop_signal)fprintf(stderr,"MOON_AUDIO_STOP signal=%d\n",stop_signal);
 return exitCode;
}
