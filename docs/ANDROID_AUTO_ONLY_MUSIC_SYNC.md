# Android Auto-only Music Sync investigation (Build 47)

Status: source inspection completed for the current public HMG-API implementation. No playback-audio tap found in the inspected Android Auto hooks. Investigation remains open.

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
- `docs/hook-points.md`
- `mods/wireless-carplay-aa/settings.json`

The Android Auto GPM hooks expose:
- connection and disconnection events
- navigation focus
- navigation guidance status
- navigation messages (destination/charging stops and turn distance/time)
- video density configuration
- sensor/fuel/EV connector configuration

The documented projection broadcasts contain event names, integer values, and navigation payload bytes. They are not audio samples.

The inspected Android Auto hooks do not expose PCM buffers, decoded audio callbacks, a loopback tap, or an audio-analysis API. The HMG-API Wireless mod's audio toggle is explicitly for CarPlay music and Siri over Wi-Fi (AAC/Opus), not Android Auto audio. The CarPlay decoder lead is irrelevant to this Android Auto-only project and must not be treated as an available solution.

## Platform consideration

The HMG-API repository's EV6 unit-command documentation describes the US EV6 260507 head unit as Android 4.4-era. Android's modern AudioPlaybackCapture API is therefore not a viable assumption for this head unit. Do not add RECORD_AUDIO or MediaRecorder microphone capture as a workaround.

## Next steps

1. Continue inspecting the Android Auto GPM app/library and the head unit's audio routing/services for a non-microphone playback PCM source.
2. Determine whether the AA audio decoder/output path is in a process/library that HMG-API can safely hook on this exact EV6 firmware.
3. If a candidate PCM callback is found, validate it off-car with read-only copying and no changes to audio playback.
4. Prove samples track Android Auto music while cabin microphones are unavailable/unused; test session start/stop and silence.
5. Keep NEMESIS analyzer modular (audio source interface -> bass/mid/treble analysis -> lighting effects), but do not enable a source until proven.
6. Keep vehicle lighting output disabled during audio-source experiments; separately validate the lighting lease/status/restore path before in-car testing.

## Acceptance criteria

- Android Auto only; no CarPlay feature dependency.
- No RECORD_AUDIO permission and no MediaRecorder.AudioSource.MIC in production Music Sync.
- Demonstrated Android Auto playback PCM reaches the analyzer while the cabin is quiet.
- No audio modification, playback glitches, or microphone fallback.
- Loss of Android Auto audio stops reactive effects cleanly.
- Unsupported audio source is reported honestly.
