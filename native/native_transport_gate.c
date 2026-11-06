#include "native_transport_gate.h"
int moon_transport_preflight(const MoonTransportChecks *c){
 if(!c||!c->verify_hmg_version||!c->verify_vehicle_fingerprint||!c->verify_micom_abi||!c->verify_exclusive_writer) return -1;
 if(c->verify_hmg_version(c->ctx)!=1) return -2;
 if(c->verify_vehicle_fingerprint(c->ctx)!=1) return -3;
 if(c->verify_micom_abi(c->ctx)!=1) return -4;
 if(c->verify_exclusive_writer(c->ctx)!=1) return -5;
 return 0;
}
int moon_transport_bind_verified(MoonSession *s,MoonHmgTransport *t,const MoonTransportChecks *c){
 if(!s||!t||s->active||s->restore_pending) return -1;
 s->hw=(MoonHardware){0};
 int rc=moon_transport_preflight(c);if(rc) return rc;
 return moon_hmg_bind(s,t);
}
