# Android Auto-only Music Sync investigation (Build 47)

Status: source inspection completed for the current public HMG-API implementation. No playback-audio tap found in the inspected Android Auto hooks or repository audio references. Investigation remains open; no APK/build has been produced by this investigation.

## Scope decision

NEMESIS targets Android Auto only. CarPlay is out of scope because the target vehicle/user setup does not use CarPlay.

Hard requirements:
- No phone microphone or vehicle microphone.
- No microphone fallback.
- Use actual Android Auto music playback audio if a supported non-microphone source can be proven.
- If unavailable, show a clear unsupported/unavailable state rather than silently switching sources.

## HMG-API Android Auto source findings

Inspected:
- `core/runtime/java/hmgapi/hook/ProjectionHooks.java`
- `mods/wireless-carplay-aa/src/hmgapi/mods/wireless/Wireless.java`
- `docs/hook-points.md`, especially the GPM section
- `mods/wireless-carplay-aa/settings.json`
- EV6 US 260507 GPM hook map

The Android Auto GPM hooks expose:
- connection and disconnection events
- navigation focus
- navigation guidance status
- navigation messages (destination/charging stops and turn distance/time)
- video density configuration
- sensor/fuel/EV connector configuration

The documented GPM hook-point catalogue lists GalReceiver, NavigationStatus, VideoSink, SensorSource, and navigation methods. It does not list an audio receiver, audio decoder, PCM callback, or playback-buffer method.

The documented projection broadcasts contain event names, integer values, and navigation payload bytes. They are not audio samples.

A repository code search for Android Auto audio/receiver audio, AudioRecord, AudioTrack, AudioFlinger, AudioService, AudioSystem, playback output, and related audio-framework symbols did not surface an Android Auto playback tap in the public HMG-API source. This is a bounded repository search, not proof that no such path exists in the head unit's proprietary binaries.

The HMG-API Wireless mod's `audio` toggle is explicitly for CarPlay music and Siri over Wi-Fi (AAC/Opus), not Android Auto audio. The CarPlay decoder lead is irrelevant to this Android Auto-only project and must not be treated as an available solution.

HMG-API's Android Auto native patch of `libautoreceiver_infobank.so` is documented for navigation protocol/turn-by-turn data. It is not evidence of an audio callback.

## Platform consideration

The HMG-API repository's EV6 unit-command documentation describes the US EV6 260507 head unit as Android 4.4-era. Android's modern AudioPlaybackCapture API is therefore not a viable assumption for this head unit. Do not add RECORD_AUDIO or MediaRecorder microphone capture as a workaround.

## Technical direction

Android Auto session state and music samples are separate data paths. The GPM hooks prove that HMG-API can observe a projection session; they do not prove that the same process receives music PCM.

The next investigation must identify the actual audio route on the target firmware:
1. Determine which head-unit process receives Android Auto media audio and which service owns the output stream.
2. Inspect the EV6 firmware's audio policy/HAL and media-routing configuration, not just GPM's navigation hooks.
3. Identify whether a privileged, read-only PCM tap exists at a documented or safely hookable point before audio reaches the vehicle output.
4. Verify the candidate against Android Auto music playback and non-music states (navigation prompts, calls, silence) so prompts/calls are not mistaken for music.
5. If no safe PCM tap exists, stop and report the limitation rather than introducing microphone capture or risky system modifications.

No audio routing/HAL implementation should be changed until the exact EV6 firmware path is identified. No live playback stream should be altered; the desired tap is read-only.

## NEMESIS implementation boundary

Keep the analyzer modular:
- audio source interface
- PCM format/rate/channel validation
- bass/mid/treble analysis
- smoothing and effect timing
- lighting output adapter

Do not wire a production source into the analyzer until its provenance is proven to be Android Auto playback PCM. Keep lighting output disabled during audio-source experiments. Separately validate the lighting lease/status/restore path before in-car testing.

## Acceptance criteria

- Android Auto only; no CarPlay feature dependency.
- No RECORD_AUDIO permission and no MediaRecorder.AudioSource.MIC in production Music Sync.
- Demonstrated Android Auto playback PCM reaches the analyzer while cabin microphones are unavailable/unused.
- Navigation prompts and calls are not incorrectly treated as music.
- No audio modification, playback glitches, or microphone fallback.
- Loss of Android Auto audio stops reactive effects cleanly.
- Unsupported audio source is reported honestly.
