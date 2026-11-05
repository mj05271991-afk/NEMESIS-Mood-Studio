# NEMESIS Music Sync audio-source requirements

## Non-negotiable behavior
- NEMESIS must never use the phone microphone or the EV6 head-unit microphone as a fallback.
- Do not request Android RECORD_AUDIO permission for Music Sync.
- Do not use MediaRecorder, AudioRecord, MediaProjection audio capture, WebView getUserMedia, navigator.mediaDevices.getUserMedia, or any microphone AudioSource.
- If a verified EV6 infotainment playback-audio interface is unavailable, Music Sync must report "EV6 playback audio unavailable" and remain off. Demo Signal may remain available for visual/effect testing, but must be clearly labeled as demo and must not be represented as live music.

## Permitted live source
Use only a documented or experimentally verified interface that supplies the EV6 infotainment system's actual playback stream or trustworthy bass/mid/treble levels derived from that stream. Verify its permissions, privilege requirements, supported firmware, audio routing, and behavior when other apps play audio before enabling it.

## Acceptance tests before release
1. Static/source scan confirms no microphone capture API, microphone AudioSource, or microphone fallback exists in the packaged APK.
2. Manifest and runtime permission review confirms no RECORD_AUDIO permission is declared or requested for Music Sync.
3. With no verified EV6 playback source, starting Music Sync fails closed with a clear status; no microphone prompt appears and no audio is captured.
4. Demo Signal works independently and is labeled as simulated input.
5. Only after a real EV6 playback source is proven: verify bass, midrange, and treble response against audio played through the infotainment system, with the vehicle stationary.
6. Re-run the scan against the final APK, not only source code, and retain the APK hash and build artifact provenance.

## Current status
Direct EV6 infotainment playback capture is not yet verified. Therefore a release must not claim true car-audio Music Sync. The safe behavior is microphone-free and disabled for live input until the source is proven.
