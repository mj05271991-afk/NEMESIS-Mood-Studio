#include "native_controller.h"
int moon_controller_begin(MoonController *c,const char *path,int external_exclusion_verified){
 if(!c||!path||!external_exclusion_verified||c->owned||c->lock.fd!=-1||c->session.restore_pending) return -1;
 if(moon_lock_acquire(&c->lock,path)) return -2;
 c->owned=1;
 int rc=moon_start(&c->session,external_exclusion_verified);
 if(rc){if(!c->session.restore_pending){moon_lock_release(&c->lock);c->owned=0;}return rc;}
 return 0;
}
int moon_controller_color(MoonController *c,int r,int g,int b,int brightness){
 if(!c||!c->owned||c->lock.fd<0) return -1;
 return moon_color(&c->session,r,g,b,brightness);
}
int moon_controller_end(MoonController *c){
 if(!c||!c->owned) return -1;
 int rc=moon_stop(&c->session);if(rc) return rc;
 moon_lock_release(&c->lock);c->owned=0;return 0;
}
