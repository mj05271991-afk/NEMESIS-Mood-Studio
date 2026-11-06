#include "native_lighting_session.h"
int moon_stop(MoonSession *s) {
 if (!s) return -1;
 s->active=0;
 if (!s->restore_pending) return 0;
 if (!s->hw.restore || s->hw.restore(s->hw.ctx,&s->saved)) return -2;
 s->restore_pending=0;return 0;
}
int moon_start(MoonSession *s,int owner) {
 if (!s || !owner || s->active || s->restore_pending || !s->hw.read || !s->hw.set_drive_linked || !s->hw.restore || !s->hw.set_color) return -1;
 MoonState previous;
 if (s->hw.read(s->hw.ctx,&previous) || previous.brightness<0 || previous.brightness>100 || (previous.drive_linked!=0 && previous.drive_linked!=1)) return -2;
 s->saved=previous;s->restore_pending=1;
 if (s->hw.set_drive_linked(s->hw.ctx,0)) {if(moon_stop(s)) return -3;return -2;}
 s->active=1;return 0;
}
int moon_color(MoonSession *s,int r,int g,int b,int brightness) {
 if (!s || !s->active || !s->restore_pending || !s->hw.set_color) return -1;
 if (r<0||r>255||g<0||g>255||b<0||b>255||brightness<0||brightness>100) return -2;
 return s->hw.set_color(s->hw.ctx,(unsigned char)r,(unsigned char)g,(unsigned char)b,brightness)?-3:0;
}
