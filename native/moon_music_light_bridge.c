#include "moon_music_light_bridge.h"
#include <math.h>
int moon_music_bridge_update(MoonMusicBridge *b,const MoonAudioMeter *m,const MoonAudioBands *a){
 if(!b||!m||!a||!b->controller||!b->enabled)return -1;
 if(!b->controller->owned||!b->controller->session.active)return -2;
 if(!isfinite(m->rms)||!isfinite(a->bass)||!isfinite(a->mid)||!isfinite(a->treble))return -3;
 double total=a->bass+a->mid+a->treble;
 if(total<=0)return 0;
 double level=m->rms*3.0;
 if(level<0)level=0;
 if(level>1)level=1;
 unsigned r=(unsigned)(255.0*level*a->bass/total);
 unsigned g=(unsigned)(255.0*level*a->mid/total);
 unsigned bl=(unsigned)(255.0*level*a->treble/total);
 if(r>255)r=255;
 if(g>255)g=255;
 if(bl>255)bl=255;
 /* At 2048 samples / 48kHz, minimum 3 buffers gives >=128ms per write. */
 if(++b->frames_since_write<3)return 0;
 b->frames_since_write=0;
 if(r==b->last_r&&g==b->last_g&&bl==b->last_b)return 0;
 int rc=moon_controller_color(b->controller,(int)r,(int)g,(int)bl,100);
 if(!rc){b->last_r=r;b->last_g=g;b->last_b=bl;}
 return rc;
}
