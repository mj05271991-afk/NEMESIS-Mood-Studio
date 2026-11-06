/* EV6 tinyalsa diagnostics. Never changes mixer controls or vehicle lights. */
#include <stdio.h>
#include <string.h>
#include <tinyalsa/asoundlib.h>
#include "moon_audio_backend.h"
static int probe(void){
 struct mixer *m=mixer_open(0);
 if(!m){fputs("ALSA card 0 mixer unavailable\n",stderr);return 3;}
 const char *name="Device2 Tx to Rx Internal Loopback Mode";
 struct mixer_ctl *ctl=mixer_get_ctl_by_name(m,name);
 if(!ctl){fprintf(stderr,"Missing: %s\n",name);mixer_close(m);return 4;}
 if(mixer_ctl_get_type(ctl)!=MIXER_CTL_TYPE_BOOL){mixer_close(m);return 5;}
 printf("Found %s; current=%d (read only)\n",name,mixer_ctl_get_value(ctl,0));
 mixer_close(m);return 0;
}
static int capture(void){
 struct pcm_config cfg={0};
 cfg.channels=2;cfg.rate=48000;cfg.period_size=2048;cfg.period_count=16;cfg.format=PCM_FORMAT_S16_LE;
 struct pcm *pcm=pcm_open(0,2,PCM_IN,&cfg);
 if(!pcm||!pcm_is_ready(pcm)){fprintf(stderr,"Capture unavailable: %s\n",pcm?pcm_get_error(pcm):"null");if(pcm)pcm_close(pcm);return 6;}
 unsigned char samples[8192];
 if(pcm_readi(pcm,samples,2048)<0){fprintf(stderr,"Read failed: %s\n",pcm_get_error(pcm));pcm_close(pcm);return 7;}
 unsigned long energy=0;
 for(unsigned i=0;i<4096;i++)energy+=samples[i];
 printf("Read 1024 stereo frames; byte sum=%lu (not an audio level)\n",energy);
 pcm_close(pcm);return 0;
}
int main(int argc,char **argv){
 if(argc!=2){fputs("Usage: moon-alsa-probe --probe|--capture-once\n",stderr);return 2;}
 if(!strcmp(argv[1],"--probe"))return probe();
 if(!strcmp(argv[1],"--capture-once"))return capture();
 return 2;
}
