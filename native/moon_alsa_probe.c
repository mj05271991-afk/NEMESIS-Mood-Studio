/* Explicit EV6 ALSA diagnostic; no MICOM lighting writes. */
#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <signal.h>
#include <tinyalsa/asoundlib.h>
#include "moon_audio_backend.h"
/* Kyle's playback-side guard: capture only while device2 plays 48kHz music. */
static int music_playing(void){
 const char *paths[2]={
  "/proc/asound/card0/pcm2p/sub0/status",
  "/proc/asound/card0/pcm2p/sub0/hw_params"
 };
 const char *need[2]={"RUNNING","rate: 48000"};
 for(int i=0;i<2;i++){
  FILE *f=fopen(paths[i],"r");if(!f)return 0;
  char buf[2048];size_t n=fread(buf,1,sizeof(buf)-1,f);fclose(f);
  buf[n]=0;if(!strstr(buf,need[i]))return 0;
 }
 return 1;
}
static volatile sig_atomic_t interrupted=0;
static void stop(int sig){(void)sig;interrupted=1;}
static int run(int capture,int loopback,int continuous){
 unsigned blocks=0;
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
 if(loopback&&!music_playing()){fputs("Playback is not RUNNING at 48000 Hz; refusing loopback capture\\n",stderr);rc=10;goto done;}
 if(loopback&&original==0){
  changed=1; /* attempt restoration even after partial enable failure */
  if(mixer_ctl_set_value(ctl,0,1)||mixer_ctl_get_value(ctl,0)!=1){fputs("Loopback enable failed\n",stderr);rc=5;goto done;}
 }
 if(interrupted){rc=8;goto done;}
 struct pcm_config cfg={0};
 cfg.channels=2;cfg.rate=48000;cfg.period_size=2048;cfg.period_count=16;cfg.format=PCM_FORMAT_S16_LE;
 struct pcm *pcm=pcm_open(0,2,PCM_IN,&cfg);
 if(!pcm||!pcm_is_ready(pcm)){fprintf(stderr,"Capture unavailable: %s\n",pcm?pcm_get_error(pcm):"null");if(pcm)pcm_close(pcm);rc=6;goto done;}
 unsigned char samples[8192];
 do {
 if(loopback&&!music_playing()){puts("Playback stopped or changed rate; stopping capture");break;}
 if(interrupted||pcm_readi(pcm,samples,2048)<0){fprintf(stderr,"Capture read interrupted or failed: %s\n",pcm_get_error(pcm));rc=7;break;}
 else{
  MoonAudioMeter meter;MoonAudioBands bands;
  if(moon_audio_analyze(samples,sizeof samples,&meter)||moon_audio_bands(samples,sizeof samples,&bands))rc=8;
  else printf("frames=%lu rms=%.6f peak=%.6f bass=%.6f mid=%.6f treble=%.6f\n",meter.frames,meter.rms,meter.peak,bands.bass,bands.mid,bands.treble);
 }
 fflush(stdout);
  blocks++;
 } while(continuous&&!interrupted&&blocks<200);
 if(continuous&&blocks>=200)puts("Monitor session limit reached; restoring loopback");
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
 if(argc!=2){fputs("Usage: moon-alsa-probe --probe|--capture-once|--capture-loopback-once|--monitor-loopback\n",stderr);return 2;}
 if(!strcmp(argv[1],"--probe"))return run(0,0,0);
 if(!strcmp(argv[1],"--capture-once"))return run(1,0,0);
 if(!strcmp(argv[1],"--capture-loopback-once"))return run(1,1,0);
 if(!strcmp(argv[1],"--monitor-loopback"))return run(1,1,1);
 return 2;
}
