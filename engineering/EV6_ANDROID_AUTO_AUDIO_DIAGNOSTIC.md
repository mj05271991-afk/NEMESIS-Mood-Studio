# EV6 Android Auto playback-path diagnostic (read-only)

Purpose: collect evidence from the exact EV6 head unit while Android Auto is connected and music is playing. This is the next required step because public HMG-API hooks and the checked EV6 framework map do not expose an audio PCM callback.

## Safety

- Read-only commands only. No service calls, Binder transactions, property writes, package changes, audio recording, or lighting commands.
- Do not use `screenrecord`, `AudioRecord`, `MediaRecorder`, or microphone permissions.
- Run only while parked. Stop music before disconnecting USB/ADB if needed.
- Capture one baseline with Android Auto disconnected, then a second capture with Android Auto connected and music playing at a moderate volume. Keep the phone/cabin quiet; this is not a microphone test.

## Host-side ADB capture

Run these from a computer with Android Platform Tools and ADB access to the head unit. Save outputs separately for the two states.

```sh
adb shell getprop > getprop.txt
adb shell ps > processes.txt
adb shell service list > services.txt
adb shell dumpsys audio > audio-service.txt
adb shell dumpsys media.audio_flinger > audio-flinger.txt
adb shell dumpsys media.audio_policy > audio-policy.txt
adb shell dumpsys media.player > media-player.txt
adb logcat -d -v time -s AudioFlinger AudioPolicyManager AudioTrack AudioRecord AudioService MediaPlayer NuPlayer AAudio > audio-log.txt
```

Some dumpsys services or log tags may not exist on this Android build; a failed optional dump is not a reason to run a write command or install anything.

## Compare the two captures

Look for changes in:
- active output thread / output handle / mixer thread
- active playback tracks and their PID / session ID / format / sample rate / channel count
- audio policy output device and route changes
- media-server or projection-related process activity
- timestamps coinciding with Android Auto music start/stop

Do not assume a process name or track belongs to Android Auto based only on timing. Correlate the PID/session/route across both captures and confirm with a repeat start/stop test.

## Decision gate

A candidate path is worth further work only if the capture shows a stable, attributable playback track or output path for Android Auto music. The next step would be a source-level feasibility review of a read-only tap at that exact point, including permissions/SELinux/process boundaries and firmware compatibility.

If the dumps expose only a shared mixer with no safe per-track PCM access, do not inject a recorder or modify the HAL as a shortcut. Report that the stock firmware does not expose a safe supported source and reassess options.

## Current codebase status

The NEMESIS Build 47 branch is currently a UI + EV6 read-only lighting probe scaffold. The manifest declares no audio permissions, and the Java source tree currently has no Android Auto PCM capture implementation. The browser UI has a demo signal for preview and explicitly reports that live EV6 stereo audio is not connected. No production music-sync source is implemented, and this investigation has not produced an APK.
