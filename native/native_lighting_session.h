#ifndef MOON_NATIVE_LIGHTING_SESSION_H
#define MOON_NATIVE_LIGHTING_SESSION_H
typedef struct { unsigned char r,g,b; int brightness; int drive_linked; } MoonState;
typedef struct { void *ctx; int (*read)(void *,MoonState *); int (*set_drive_linked)(void *,int); int (*set_color)(void *,unsigned char,unsigned char,unsigned char,int); int (*restore)(void *,const MoonState *); } MoonHardware;
typedef struct { MoonHardware hw; MoonState saved; int active,restore_pending; } MoonSession;
int moon_start(MoonSession *,int);
int moon_color(MoonSession *,int,int,int,int);
int moon_stop(MoonSession *);
#endif
