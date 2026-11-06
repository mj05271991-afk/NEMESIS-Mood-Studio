#ifndef MOON_NATIVE_CONTROLLER_H
#define MOON_NATIVE_CONTROLLER_H
#include "native_lighting_session.h"
#include "native_ownership_lock.h"
typedef struct { MoonSession session; MoonLock lock; int owned; } MoonController;
int moon_controller_begin(MoonController *,const char *,int);
int moon_controller_color(MoonController *,int,int,int,int);
int moon_controller_end(MoonController *);
#endif
