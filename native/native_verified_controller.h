#ifndef MOON_VERIFIED_CONTROLLER_H
#define MOON_VERIFIED_CONTROLLER_H
#include "native_controller.h"
#include "native_transport_gate.h"
int moon_verified_begin(MoonController *,MoonHmgTransport *,const MoonTransportChecks *,const char *);
#endif
