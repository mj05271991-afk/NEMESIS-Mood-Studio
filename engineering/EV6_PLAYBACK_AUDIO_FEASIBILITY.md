# EV6 playback-audio feasibility investigation

## Decision
NEMESIS Music Sync remains microphone-free. Do not add a microphone fallback, including the phone microphone or head-unit microphone. If no verified playback feed is available, live Music Sync remains unavailable and Demo Signal is clearly labeled as simulated.

## Platform finding
Android's public AudioPlaybackCapture API was added in Android 10 (API 29). It captures eligible playback from other apps only under platform, player-policy, user-consent, and permission constraints. It is not available on Android 4.4-era platforms. The current NEMESIS app targets API 28 and has no RECORD_AUDIO declaration in its manifest.

References:
- Android Developers: https://developer.android.com/media/platform/av-capture
- Android API reference: https://developer.android.com/reference/android/media/AudioPlaybackCaptureConfiguration
- Android 10 release notes: https://source.android.com/docs/whatsnew/android-10-release

## Candidate paths, in priority order
1. **Existing EV6/HMG-API/vendor playback interface:** inspect supported service APIs, broadcasts, Binder interfaces, or documented audio-level telemetry. Prefer a supported interface that returns playback PCM or trustworthy band levels without changing system audio services.
2. **Privileged system integration:** only investigate if the head unit's own audio service exposes a supported privileged tap. This may require system-level permissions or platform/vendor changes and is not an ordinary APK-only feature. Do not patch AudioFlinger, audio policy, or vendor HAL on the user's head unit as an exploratory test.
3. **External audio signal path:** consider only if the user explicitly approves additional hardware or a separate signal source. This is not the requested built-in infotainment-only solution, so it is not a default substitute.

## Immediate investigation tasks
- Identify exact head-unit Android/API level and firmware from read-only device properties; do not infer it from phone SDK or app target SDK.
- Inspect HMG-API and EV6 vendor service source for playback-audio or audio-level interfaces; distinguish audio playback control from audio capture.
- Search the firmware/system image for documented audio service endpoints and permission requirements without modifying the head unit.
- Prototype only against a recorded test stream or simulator until a supported live source is established.
- Verify bass/mid/treble response against audio playing through the EV6 infotainment system while stationary, with no microphone permission or microphone APIs in the final package.

## Release gates
- Static scan of source and final APK: no MediaRecorder, microphone AudioSource, AudioRecord microphone capture, WebView getUserMedia, or microphone fallback.
- Manifest/runtime permission scan: no RECORD_AUDIO for Music Sync.
- Live Music Sync stays disabled with a clear "EV6 playback audio unavailable" status until the playback path is proven.
- Final APK and installer are inspected after build; record hashes, provenance, and test results.
