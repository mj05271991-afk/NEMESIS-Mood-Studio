# CarPlay decoded-audio tap candidate

Repository examined: `VysseCodes/HMG-API`
Revision: `180185de2c57afb2cca4858d0be7828e8f1051d6`
File examined: `mods/wireless-carplay-aa/native/leafiaaac/leafiaaac.c`

## New finding

The Wireless CarPlay mod contains a native AAC-LC-to-PCM decoder for the Gen5W CarPlay stack. Its source states that wireless CarPlay music arrives as AAC-LC on the buffered MainHigh stream and that the converter decodes it to 16-bit PCM. The converter's `LeafiaAudioConverterFillComplexBuffer` function copies decoded PCM samples from its internal `Conv.pcm` buffer into the output buffer.

This is the first concrete source-level candidate for obtaining actual CarPlay music samples without a microphone.

## What it does not prove

- It does not provide a public HMG-API method that returns PCM to another mod.
- The code is a codec implementation used by the CarPlay stack, not an ambient-light analyzer.
- It does not establish a corresponding Android Auto PCM tap.
- It does not prove that a safe, stable, low-overhead observer can be attached to this callback on the EV6.
- It does not prove Kyle's implementation uses this path.

## Additional important detail

The same converter also implements LPCM-to-Opus encoding for the CarPlay microphone path (Siri/calls). NEMESIS must not consume or enable that input path. The candidate for Music Sync is strictly the AAC/Opus decode-to-LPCM path, and only for incoming playback audio.

## Potential investigation

A CarPlay-specific proof of concept could observe the PCM output after successful decode and before it is handed to CarPlay's output pipeline, calculate small bass/mid/treble summaries, and pass only those summaries to NEMESIS. It must not copy, retain, transmit, or alter the music stream. Any observer must be non-blocking, bounded, lifecycle-safe, and must never interfere with playback if NEMESIS is unavailable.

This would require a separately reviewed native hook/extension and on-car testing. Do not modify the owner's head unit for this experiment. First prototype off-car against the source and a synthetic PCM stream, then review the exact hook point and failure behavior.

## Android Auto status

The inspected HMG-API Projection API documents Android Auto session/navigation events and sensor integration, not decoded PCM. The current search did not identify an Android Auto equivalent of the CarPlay AAC decoder. Android Auto remains unresolved and must not be claimed as supported by this candidate.

## Recommended next step

Ask the HMG-API maintainer whether the CarPlay converter can safely expose a non-blocking PCM observer callback and whether an equivalent Android Auto audio callback exists. Keep Music Sync microphone-free and report CarPlay / Android Auto support separately until each is demonstrated.
