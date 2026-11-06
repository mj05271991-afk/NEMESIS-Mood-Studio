#ifndef MOON_NATIVE_HMG_ADAPTER_H
#define MOON_NATIVE_HMG_ADAPTER_H
#include "native_lighting_session.h"
typedef struct { void *ctx; int (*read_state)(void *,MoonState *); int (*set_drive_link)(void *,int); int (*write_rgb)(void *,unsigned char,unsigned char,unsigned char,int); int (*restore_state)(void *,const MoonState *); int external_exclusion_verified; } MoonHmgTransport;
int moon_hmg_bind(MoonSession *,MoonHmgTransport *);
#endif
