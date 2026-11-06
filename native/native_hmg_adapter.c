#include "native_hmg_adapter.h"
static int read_state(void *p,MoonState *s){MoonHmgTransport *t=p;return t->read_state(t->ctx,s);}
static int drive_link(void *p,int v){MoonHmgTransport *t=p;return t->set_drive_link(t->ctx,v);}
static int rgb(void *p,unsigned char r,unsigned char g,unsigned char b,int brightness){MoonHmgTransport *t=p;return t->write_rgb(t->ctx,r,g,b,brightness);}
static int restore(void *p,const MoonState *s){MoonHmgTransport *t=p;return t->restore_state(t->ctx,s);}
int moon_hmg_bind(MoonSession *s,MoonHmgTransport *t){
 if (!s || !t || !t->external_exclusion_verified || !t->read_state || !t->set_drive_link || !t->write_rgb || !t->restore_state || s->active || s->restore_pending) return -1;
 s->hw=(MoonHardware){.ctx=t,.read=read_state,.set_drive_linked=drive_link,.set_color=rgb,.restore=restore};
 return 0;
}
