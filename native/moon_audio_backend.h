#ifndef MOON_AUDIO_BACKEND_H
#define MOON_AUDIO_BACKEND_H
#include <stddef.h>
/* Offline PCM interface, 48kHz stereo signed 16-bit little-endian.
   Hardware ALSA capture is not enabled by this module. */
typedef struct { unsigned long frames; double rms; double peak; } MoonAudioMeter;
int moon_audio_analyze(const void *data,size_t bytes,MoonAudioMeter *out);
#endif
