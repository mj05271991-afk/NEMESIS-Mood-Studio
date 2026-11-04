# NEMESIS Moon Studio — Android source scaffold

This package contains the Moon Studio interface and an Android API 19 application scaffold with native microphone capture. It is **source code, not a compiled APK**.

## Included
- `app/src/main/assets/index.html` — Moon Studio UI, six requested color presets, custom color input, nine effects, manual brightness, sensitivity/smoothing/speed, and local profiles.
- `MoonStudioActivity.java` — fullscreen WebView host and JavaScript bridge.
- `AudioBandCapture.java` — native microphone capture and three audio-level outputs.
- Android manifest and Gradle project files.

## Explicit boundaries
- No HMG-API dependency or use.
- No Binder lighting writes or EV6 lighting-control code is included.
- Audio level is used only for color/effect response. The brightness slider remains a manual UI setting and is not driven by audio.
- This scaffold has not been compiled or tested on the EV6 head unit. The Android 4.4 WebView may not support all modern CSS/JavaScript used by the current interface; a compatibility pass is still required.
- The audio-band implementation is a basic level estimator, not a validated spectral filter bank. Treat it as an engineering starting point, not production audio analysis.

## Build
Open this folder in Android Studio with a compatible Android Gradle Plugin/Gradle/JDK setup, allow Gradle sync, then build the `app` debug APK. This project targets API 19 and compiles against API 28. No APK is supplied because no Android build has been run or verified here.

## Next engineering work
1. Backport the UI to the EV6's Android 4.4 WebView (ES5 and legacy CSS).
2. Replace the simple audio-level estimator with validated bass/mid/treble filtering.
3. Compile and test on an emulator/device before any EV6 head-unit installation.
4. Keep vehicle lighting output disabled until firmware-specific read-only status and restore behavior are independently verified.
