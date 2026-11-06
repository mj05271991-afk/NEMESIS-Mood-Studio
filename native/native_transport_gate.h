#ifndef MOON_NATIVE_TRANSPORT_GATE_H
#define MOON_NATIVE_TRANSPORT_GATE_H
#include "native_hmg_adapter.h"
typedef struct { void *ctx; int (*verify_hmg_version)(void *); int (*verify_vehicle_fingerprint)(void *); int (*verify_micom_abi)(void *); int (*verify_exclusive_writer)(void *); } MoonTransportChecks;
int moon_transport_preflight(const MoonTransportChecks *);
int moon_transport_bind_verified(MoonSession *,MoonHmgTransport *,const MoonTransportChecks *);
#endif
