#ifndef MOON_MUSIC_LIGHT_BRIDGE_H
#define MOON_MUSIC_LIGHT_BRIDGE_H
#include "native_controller.h"
#include "moon_audio_backend.h"
typedef struct { MoonController *controller; int enabled; unsigned last_r,last_g,last_b; unsigned frames_since_write; } MoonMusicBridge;
/* No hardware access until a separately verified controller session is active. */
int moon_music_bridge_update(MoonMusicBridge *,const MoonAudioMeter *,const MoonAudioBands *);
#endif
