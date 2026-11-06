/* Explicit EV6 ALSA diagnostic; no MICOM lighting writes. */
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <signal.h>
#include <tinyalsa/asoundlib.h>
#include "moon_audio_backend.h"
static volatile sig_atomic_t interrupted=0;
static void stop(int sig){(void)sig;interrupted=1;}
static int run(int capture,int loopback){
 struct mixer *m=mixer_open(0);
 if(!m){fputs("ALSA card0 unavailable\n",stderr);return 3;}
 const char *name="Device2 Tx to Rx Internal Loopback Mode";
 struct mixer_ctl *ctl=mixer_get_ctl_by_name(m,name);
 if(!ctl||mixer_ctl_get_type(ctl)!=MIXER_CTL_TYPE_BOOL){
  fprintf(stderr,"Required boolean loopback switch unavailable: %s\n",name);mixer_close(m);return 4;
 }
 int original=mixer_ctl_get_value(ctl,0),changed=0,rc=0;
 printf("Loopback original=%d\n",original);
 if(!capture){mixer_close(m);return 0;}
 if(loopback&&original==0){
  if(mixer_ctl_set_value(ctl,0,1)||mixer_ctl_get_value(ctl,0)!=1){fputs("Loopback enable failed\n",stderr);rc=5;goto done;}
  changed=1;
 }
 if(interrupted){rc=8;goto done;}
 struct pcm_config cfg={0};
 cfg.channels=2;cfg.rate=48000;cfg.period_size=2048;cfg.period_count=16;cfg.format=PCM_FORMAT_S16_LE;
 struct pcm *pcm=pcm_open(0,2,PCM_IN,&cfg);
 if(!pcm||!pcm_is_ready(pcm)){fprintf(stderr,"Capture unavailable: %s\n",pcm?pcm_get_error(pcm):"null");if(pcm)pcm_close(pcm);rc=6;goto done;}
 unsigned char samples[8192];
 if(interrupted||pcm_readi(pcm,samples,2048)<0){fprintf(stderr,"Capture read interrupted or failed: %s\n",pcm_get_error(pcm));rc=7;}
 else{
  MoonAudioMeter meter;MoonAudioBands bands;
  if(moon_audio_analyze(samples,sizeof samples,&meter)||moon_audio_bands(samples,sizeof samples,&bands))rc=8;
  else printf("frames=%lu rms=%.6f peak=%.6f bass=%.6f mid=%.6f treble=%.6f\n",meter.frames,meter.rms,meter.peak,bands.bass,bands.mid,bands.treble);
 }
 pcm_close(pcm);
done:
 if(changed){
  if(mixer_ctl_set_value(ctl,0,original)||mixer_ctl_get_value(ctl,0)!=original){fputs("WARNING: loopback restoration failed\n",stderr);rc=9;}
  else puts("Loopback restored");
 }
 mixer_close(m);
 return rc;
}
int main(int argc,char **argv){
 signal(SIGINT,stop);signal(SIGTERM,stop);
 if(argc!=2){fputs("Usage: moon-alsa-probe --probe|--capture-once|--capture-loopback-once\n",stderr);return 2;}
 if(!strcmp(argv[1],"--probe"))return run(0,0);
 if(!strcmp(argv[1],"--capture-once"))return run(1,0);
 if(!strcmp(argv[1],"--capture-loopback-once"))return run(1,1);
 return 2;
}
