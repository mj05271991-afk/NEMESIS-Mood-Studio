# HMG-API projection vs. playback-audio findings

Repository examined: `VysseCodes/HMG-API`
Revision examined: `180185de2c57afb2cca4858d0be7828e8f1051d6`

## Result

HMG-API's Android Auto / CarPlay integration is useful for projection connectivity and vehicle integration, but the inspected public source does **not** document a PCM/audio-loopback feed or bass/mid/treble telemetry for mods. Do not treat projection being connected or audio playing through the speakers as proof that a mod can read that audio.

## Evidence in source

1. `docs/api-plan.md`, section 6 (Projection):
   - Documents wired/wireless session started/ended, navigation owner, turn-by-turn data, navigation events, Android Auto density and sensor settings.
   - Does not list audio PCM, playback capture, audio-session data, audio-level telemetry, or a spectrum feed.
2. `mods/wireless-carplay-aa/mod.json`:
   - Declares a projection part and an optional `audio` part.
   - The optional audio part is associated with CarPlay native library patches, not a general playback capture API.
3. `mods/wireless-carplay-aa/recipes/carplay-audio-formats.recipe`:
   - Its own header says it enables AAC (music) and Opus (voice) on Wi-Fi so the iPhone can set up wireless audio; wired is unchanged.
   - This is codec/format negotiation support, not an audio tap or analyzer.
4. `mods/wireless-carplay-aa/recipes/carplay-utils-converter.recipe`:
   - Forwards AudioConverter calls to the bundled AAC/Opus converter library. This is audio codec conversion, not a documented feed of decoded samples to another mod.
5. `docs/api-plan.md` describes Ambient Sound (nmz787) explicitly as **microphone-driven ambient light**, using a system-UID worker to call the ambient-light service.
6. `docs/vehicle-commands.md` documents ambient-light control through the vehicle lease and stock system service. That is lighting output, separate from audio input.

## Implication for NEMESIS

- HMG-API remains promising for safer, coordinated EV6 lighting control through its ambient-light lease, subject to on-car verification and the known first-use snapshot concern.
- HMG-API's projection API is useful for detecting Android Auto sessions and navigation state, but the inspected source does not establish access to Android Auto's actual music samples.
- The CarPlay audio patch is a codec compatibility patch; it does not establish sample capture either.
- Kyle's reported microphone-free behavior may use a separate implementation, privileged interface, or a different audio route. The HMG-API source inspected here does not identify which.
- Do not implement a microphone fallback. Do not patch AudioFlinger, audio policy, or vendor audio HAL on the owner's EV6 as an exploratory experiment.

## Next fastest evidence-gathering steps

1. Inspect the exact HMG-API release asset and Wireless mod package to confirm the deployed code matches the reviewed source revision.
2. Inspect the EV6's mapped Android Auto / DiPO / GPM audio callback paths for a documented callback that exposes decoded PCM or samples, not just connection/focus/codec negotiation.
3. Inspect the CarPlay native converter integration only to determine whether decoded sample buffers are ever surfaced to a mod. Do not assume the converter itself is a tap.
4. If no supported sample path exists, ask the HMG-API maintainer a narrow technical question: whether any public hook or service exposes Android Auto / CarPlay decoded playback PCM or audio-level data to a mod without microphone capture.
5. Keep live Music Sync disabled with an explicit unavailable status until a playback feed is demonstrated on the exact EV6 firmware.

## Release gate

A final APK must be statically checked for microphone capture APIs and RECORD_AUDIO permission. The exact on-car test must demonstrate that playing music over Android Auto or CarPlay changes analyzer bands while no microphone permission or microphone input is present. A connected-session event alone does not pass this test.
