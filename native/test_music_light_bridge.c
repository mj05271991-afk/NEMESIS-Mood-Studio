#include "moon_music_light_bridge.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>
typedef struct { int writes; unsigned r,g,b; } Fake;
static int color(void *p,unsigned char r,unsigned char g,unsigned char b,int brightness){
 Fake *f=p;assert(brightness==40);f->writes++;f->r=r;f->g=g;f->b=b;return 0;
}
int main(void){
 Fake fake={0};
 MoonController ctl;memset(&ctl,0,sizeof ctl);
 ctl.owned=1;ctl.lock.fd=4;ctl.session.active=1;ctl.session.restore_pending=1;
 ctl.session.hw.ctx=&fake;ctl.session.hw.set_color=color;
 MoonMusicBridge bridge={0};bridge.controller=&ctl;bridge.enabled=1;bridge.brightness_percent=40;
 MoonAudioMeter meter={0};meter.rms=.5;
 MoonAudioBands bands={0};bands.bass=1;bands.mid=0;bands.treble=0;
 assert(moon_music_bridge_update(&bridge,&meter,&bands)==0);
 assert(moon_music_bridge_update(&bridge,&meter,&bands)==0);
 assert(fake.writes==0);
 assert(moon_music_bridge_update(&bridge,&meter,&bands)==0);
 assert(fake.writes==1&&fake.r==255&&fake.g==0&&fake.b==0);
 for(int i=0;i<6;i++)assert(moon_music_bridge_update(&bridge,&meter,&bands)==0);
 assert(fake.writes==1);
 bridge.enabled=0;
 assert(moon_music_bridge_update(&bridge,&meter,&bands)==-1);
 ctl.session.active=0;bridge.enabled=1;
 assert(moon_music_bridge_update(&bridge,&meter,&bands)==-2);
 puts("PASS: gated music-light RGB, rate limit, deduplication");
 return 0;
}
