# Projection audio source lead: CarPlay PCM decoder

Status: promising source-code lead; not yet an audio tap and not validated on a vehicle.

## Finding in HMG-API

Inspected:

- `VysseCodes/HMG-API/mods/wireless-carplay-aa/src/hmgapi/mods/wireless/dipo/Dipo.java`
- `VysseCodes/HMG-API/mods/wireless-carplay-aa/native/leafiaaac/leafiaaac.c`

The CarPlay Wireless mod patches `libCarPlayAudio.so` and `libCarPlayUtils.so`. Its converter implementation handles AAC-LC to LPCM decoding for CarPlay music, and Opus to LPCM decoding for speech/calls. The same converter also supports LPCM-to-Opus encoding for the microphone uplink.

This establishes that the CarPlay stack has a decoded PCM stage for incoming music. It does NOT establish that HMG-API exposes those PCM samples to other apps or that a NEMESIS mod can safely subscribe to them.

## Why this matters

A hook at the decoder output or audio-buffer handoff might provide a non-microphone signal for CarPlay music. This is a more concrete investigation target than generic Android playback capture. The converter's PCM buffers may be transient and owned by the CarPlay process; copying them safely, preserving timing, and avoiding audio glitches would need careful implementation and testing.

The code explicitly labels the LPCM-to-Opus encoder path as microphone input. NEMESIS must not use that encoder path. The candidate is only the AAC/Opus decode-to-LPCM path for inbound playback.

## Android Auto status

The HMG-API documentation and hook catalogue show Android Auto session and projection hooks, navigation events, and sensor/video configuration. The searches and files inspected so far did not identify a corresponding PCM/audio-buffer callback for Android Auto. This is not proof none exists; it remains an open source-inspection task.

## Next steps

1. Trace the CarPlay decode path from `AudioConverterFillComplexBuffer` through the caller to the PCM buffer consumed by the output pipeline.
2. Determine whether the buffer contains only inbound music or also other audio, and identify session/source boundaries.
3. Assess whether an HMG-API hook can copy PCM without changing playback, adding latency, or exposing microphone uplink data.
4. Separately inspect the Android Auto GPM/audio path for a matching decoded-PCM stage.
5. Keep any prototype read-only and disabled by default. No audio modification, no microphone permissions, and no microphone fallback.
6. Prove the source using playback while the cabin is quiet; compare analyzer output with music on/off and ensure no samples arrive from microphone activity.
7. Only after that proof, connect the copied PCM stream to NEMESIS's bass/mid/treble analyzer.

## Acceptance criteria

- Music samples are demonstrably from inbound projection playback.
- No `RECORD_AUDIO`, `MediaRecorder`, or microphone-uplink source is used.
- Audio tap is read-only and does not alter or interrupt CarPlay playback.
- Audio loss or session end cleanly stops analysis.
- Vehicle lighting remains disabled during audio-source experiments.
