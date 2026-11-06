/*
 * Moon Studio EV6 ALSA capture prototype.
 * Standalone diagnostic; NOT installed or launched by the APK yet.
 * Never enable or change the vehicle mixer switch from this tool.
 * Capture only if an existing authorized loopback is already enabled.
 *
 * Usage: moon_audio_probe [card] [device]
 * Emits BANDS bass mid treble (0..1) every ~100 ms.
 */
#include <tinyalsa/asoundlib.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <stdint.h>

#define RATE 48000
#define CHANNELS 2
#define FRAMES 4800
static volatile sig_atomic_t running=1;
static void quit(int signum){(void)signum;running=0;}
static float clamp01(double x){return x<0?0:x>1?1:(float)x;}
int main(int argc,char **argv){
 unsigned card=argc>1?(unsigned)atoi(argv[1]):0;
 unsigned device=argc>2?(unsigned)atoi(argv[2]):2;
 struct pcm_config cfg={0};
 cfg.channels=CHANNELS;cfg.rate=RATE;cfg.period_size=1024;cfg.period_count=4;
 cfg.format=PCM_FORMAT_S16_LE;
 signal(SIGINT,quit);signal(SIGTERM,quit);
 struct pcm *pcm=pcm_open(card,device,PCM_IN,&cfg);
 if(!pcm||!pcm_is_ready(pcm)){
  fprintf(stderr,"MOON_AUDIO_ERROR open card%u device%u: %s\n",card,device,pcm?pcm_get_error(pcm):"null");
  if(pcm)pcm_close(pcm);return 2;
 }
 fprintf(stderr,"MOON_AUDIO_READY card%u device%u\n",card,device);
 int16_t buf[FRAMES*CHANNELS];
 /* One-pole lowpass filters split the stream into low/mid/high components.
    This is a diagnostic energy estimator, not Kyle's production FFT. */
 const double alphaBass=1.0-exp(-2.0*3.141592653589793*250.0/RATE);
 const double alphaMid=1.0-exp(-2.0*3.141592653589793*2000.0/RATE);
 double low=0,lowmid=0;
 while(running){
  if(pcm_readi(pcm,buf,FRAMES)<0){fprintf(stderr,"MOON_AUDIO_ERROR read: %s\n",pcm_get_error(pcm));break;}
  double bass=0,mid=0,treble=0;
  for(int i=0;i<FRAMES;i++){
   double sample=((double)buf[i*2]+buf[i*2+1])/(2.0*32768.0);
   low+=alphaBass*(sample-low);
   lowmid+=alphaMid*(sample-lowmid);
   double b=low,m=lowmid-low,t=sample-lowmid;
   bass+=b*b;mid+=m*m;treble+=t*t;
  }
  printf("BANDS %.4f %.4f %.4f\n",clamp01(sqrt(bass/FRAMES)*8),
   clamp01(sqrt(mid/FRAMES)*8),clamp01(sqrt(treble/FRAMES)*8));
  fflush(stdout);
 }
 pcm_close(pcm);
 return 0;
}
