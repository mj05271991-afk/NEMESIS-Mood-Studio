# Moon Studio — Kyle audio integration contract

Reference: https://github.com/dibble9012/light-vibes (README.md, build.sh, src/lightvibesd.c).
Do not replace Moon Studio's own package identity or approved UI.

## Confirmed source architecture
- Static ARMv7 musl daemon, compiled with Zig; tinyalsa pinned at a7cd26a17fe2add2ff38d2f281ed0b919187a068.
- ALSA card 0, device 2, capture pcmC0D2c, mixer control `Device2 Tx to Rx Internal Loopback Mode`.
- Enable loopback only while service actively captures; disable and release on stop and calls.
- 44.1 kHz and 48 kHz media paths; release when playback switches to call/assistant rate.
- 2048-sample Hann FFT, 1024-sample hop; beat/spectrum/pulse processing in the background daemon.
- State stored in one-value files, read periodically; do not use microphone AudioRecord as production source.
- Existing Moon Studio light control and restore logic must not be regressed.

## Integration gates (not yet complete)
1. Build Moon-owned daemon and test audio analysis off-car using synthetic PCM.
2. Add daemon lifecycle to the existing NEMESIS/HMG-API package, keeping its working USB layout.
3. Define a bounded, authenticated local control/status bridge between the UI and daemon. Do not run continuous high-rate light writes in the WebView.
4. Use real audio status to display active/inactive; never label a demo signal as vehicle audio.
5. Confirm Android Auto media actually reaches this ALSA tap on the target EV6 (Kyle documents Android Auto as *not measured*).
6. Verify safe release on calls, engine-off, app close, and restart, and restore original lighting.
7. Confirm visual fidelity to approved purple-blue Moon Studio dashboard and test head-unit performance.

## Current implementation reality
`engineering/native/moon_audio_probe.c` is a standalone diagnostic, not the daemon.
`app/src/main/java/com/nemesis/moonstudio/AudioBandCapture.java` still uses microphone input.
No new production audio integration or validated installer is claimed by this document.
