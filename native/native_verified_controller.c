#include "native_verified_controller.h"
int moon_verified_begin(MoonController *c,MoonHmgTransport *t,const MoonTransportChecks *checks,const char *path){
 if(!c||!t||!checks||!path) return -1;
 if(c->owned||c->lock.fd!=-1||c->session.active||c->session.restore_pending) return -2;
 int rc=moon_transport_bind_verified(&c->session,t,checks);
 if(rc) return rc;
 return moon_controller_begin(c,path,1);
}
