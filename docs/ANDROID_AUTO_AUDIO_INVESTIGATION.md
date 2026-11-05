# Android Auto / CarPlay audio investigation (Build 47)

Status: investigation only; no audio-capture implementation is approved.

## Requirements

- Music Sync must not use the phone microphone or the EV6 cabin microphone.
- Android Auto is the first projection path to investigate; CarPlay is a later compatibility target.
- No microphone fallback. If a supported playback-audio source is unavailable, report Music Sync as unavailable.
- Do not install experimental packages on the vehicle until the source path and recovery behavior are verified off-car.

## Verified HMG-API capabilities

Reviewed the public HMG-API README, `docs/api-plan.md`, `docs/hook-points.md`, and `docs/vehicle-commands.md`.

- HMG-API describes Projection support for Android Auto and CarPlay sessions, navigation ownership/events, turn-by-turn data, and Android Auto sensor configuration.
- Its documented Android Auto hook points include GPM connection/guidance state and sensor/video configuration hooks.
- Its documented CarPlay hook points expose connection/mode/speech state.
- Ambient Sound (nmz787) is explicitly described by HMG-API as microphone-driven. It uses a system-UID worker and the HMG-API ambient-light vehicle lease.
- HMG-API's documented Ambient Sound integration controls lighting through the car's stock services; this is not evidence that HMG-API exposes a playback PCM/audio stream.
- The reviewed Projection API documentation does not document a PCM stream, audio buffer callback, loopback tap, or audio-analysis API.

Therefore: HMG-API is a promising lighting/vehicle integration layer and a source of projection-session metadata, but current public documentation does not establish that it can deliver Android Auto or CarPlay playback audio to NEMESIS.

## Important platform constraint

Android Auto/CarPlay being the playback route does not itself grant an ordinary app access to the decoded audio. Projection session hooks and navigation events are metadata, not audio samples. Android playback capture is version- and policy-dependent and must not be assumed available on the EV6 head unit. Do not add a microphone permission or silently substitute microphone input.

## Next engineering steps

1. Inspect HMG-API's actual Projection implementation and the Android Auto/CarPlay library hook code for any audio-buffer or PCM callback, not just session events.
2. Inspect Kyle's publicly available source/package if it becomes available; specifically identify the audio source class and whether it consumes PCM, an internal audio callback, or a microphone.
3. On an off-car test harness, enumerate the head-unit Android version, audio policy, audio service interfaces, and app playback-capture policy. Record evidence before choosing an implementation.
4. If no supported playback tap exists, report that limitation honestly. Do not ship a microphone-based substitute as the requested Music Sync.
5. Keep NEMESIS's audio analyzer modular: a source interface feeding a validated bass/mid/treble analyzer, with no source enabled by default until verified.
6. Keep vehicle lighting output disabled until the EV6 firmware-specific status and restoration path is independently verified.

## Acceptance criteria

- No `RECORD_AUDIO` permission and no `MediaRecorder.AudioSource.MIC` path in the production Music Sync build.
- A source test proves that actual Android Auto playback samples reach the analyzer while the cabin is quiet.
- FM/radio behavior is explicitly reported as unsupported unless a separate non-microphone source is proven.
- Audio source loss stops reactive updates safely and does not interfere with manual lighting controls.
