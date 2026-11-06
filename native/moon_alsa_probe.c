/* Read-only ALSA device probe for EV6 playback loopback.
 * No mixer writes or MICOM commands. Requires tinyalsa at build time.
 */
#include <stdio.h>
#include <string.h>
#include <tinyalsa/asoundlib.h>
int main(int argc,char **argv){
 if(argc!=2||strcmp(argv[1],"--probe")){fputs("Usage: moon-alsa-probe --probe\n",stderr);return 2;}
 struct mixer *m=mixer_open(0);
 if(!m){fputs("ALSA card 0 mixer unavailable\n",stderr);return 3;}
 const char *name="Device2 Tx to Rx Internal Loopback Mode";
 struct mixer_ctl *ctl=mixer_get_ctl_by_name(m,name);
 if(!ctl){fprintf(stderr,"Required loopback control absent: %s\n",name);mixer_close(m);return 4;}
 if(mixer_ctl_get_type(ctl)!=MIXER_CTL_TYPE_BOOL){fputs("Unexpected mixer control type\n",stderr);mixer_close(m);return 5;}
 printf("Found %s; current=%d (read only)\n",name,mixer_ctl_get_value(ctl,0));
 mixer_close(m);
 return 0;
}
