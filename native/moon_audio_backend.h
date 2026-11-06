#ifndef MOON_AUDIO_BACKEND_H
#define MOON_AUDIO_BACKEND_H
#include <stddef.h>
/* Offline PCM interface, 48kHz stereo signed 16-bit little-endian.
   Hardware ALSA capture is not enabled by this module. */
typedef struct { unsigned long frames; double rms; double peak; } MoonAudioMeter;
int moon_audio_analyze(const void *data,size_t bytes,MoonAudioMeter *out);
/* Three coarse frequency bands for 48 kHz stereo PCM, 2048-frame windows.
   Magnitudes are normalized 0..1, not calibrated sound pressure levels. */
typedef struct { double bass,mid,treble; } MoonAudioBands;
int moon_audio_bands(const void *data,size_t bytes,MoonAudioBands *out);
#endif
