# Build 47 Pre-Test Engineering Audit

Base: Build 46 commit `f9384268badc4e33c7582e3d211130e95b49d431`
Branch: `build47-pretest-audit`
Scope: investigation and documentation only. This branch does not change the Build 46 APK or the default branch.

## Executive summary

Build 46 is a successful package build, but the source review found several items that must be resolved or explicitly validated before claiming full EV6 functionality.

## 1. Audio input: current implementation is microphone capture

File: `app/src/main/java/com/nemesis/moonstudio/AudioBandCapture.java`

- `AudioRecord` is constructed with `MediaRecorder.AudioSource.MIC`.
- Therefore this capture class requests microphone input; it does not capture the EV6 media playback stream.
- The capture class has no success/failure callback. `start()` returns normally if buffer sizing, initialization, or recording startup fails.
- `MoonStudioActivity.AudioBridge.start()` does not report whether capture actually started.
- In `index.html`, `startMic()` sets `nativeAudio=true` and displays `Head-unit microphone` immediately after calling the bridge. Since the bridge method is void and native start may silently fail, the UI can report active capture without receiving any bands.
- The HTML fallback calls `getUserMedia({audio:true})`, which is also microphone capture.

### Recommendation

Do not silently switch to a microphone. Change the native bridge contract to return or asynchronously report a real capture state and source label. Until a verified car-media input exists, label this mode accurately as unavailable / microphone / demo, and keep demo explicitly separate from real audio.

No public EV6 car-audio PCM capture API was identified in the inspected HMG-API EV6 260507 framework map or hook-point documentation. The mapped MICOM data service exposes categorized vehicle data callbacks; the inspected docs do not describe media PCM samples. This is not proof that no vendor/private path exists, but there is no verified supported direct stereo feed in the current source.

Android playback-capture APIs are not a safe assumption for this head unit: the project targets Android API 19 / compile SDK 28, and the EV6 platform is an older Android build. A future investigation should inspect the actual head-unit audio routing/HAL and permissions on-device before selecting a capture approach. Do not add a microphone permission or relabel MIC as car audio as a workaround.

## 2. Audio status and UI truthfulness

Files:
- `AudioBandCapture.java`
- `MoonStudioActivity.java`
- `app/src/main/assets/index.html`

### Recommendation

Add explicit native capture states (for example: STOPPED, STARTING, ACTIVE, FAILED) and a source identifier. Only display ACTIVE after AudioRecord reports initialized and recording. Surface permission / routing / read failures to the WebView. Add a watchdog for repeated read errors and stop/release cleanly.

Keep the demo signal available for interface testing, but make it impossible to mistake demo values for live car audio.

## 3. Firmware safety gate needs confirmation

File: `app/src/main/java/com/nemesis/moonstudio/Ev6LightingProbe.java`

- The expected firmware constant is `CVEV.USA.S5W_M.V015.012.260507`.
- `firmwareMismatch()` currently reads `ro.build.product2` and compares that property to the firmware constant.
- The HMG-API EV6 260507 map confirms the target car map identity `CVEV.USA.0000.V015.012.260507`, but the inspected map/docs do not establish `ro.build.product2` as the correct property for the head-unit firmware string.
- This check may block a correct car or validate an unrelated property; it must not be weakened or bypassed to make connection work.

### Recommendation

On the EV6, read candidate build properties and the mapped BuildInfo fields read-only, record their exact values, and compare them with the installed firmware/map identity. Keep all writes blocked until a deterministic, verified match exists. Do not guess a replacement property.

## 4. Lighting status and restore

Files:
- `Ev6LightingProbe.java`
- `Ev6LightingController.java`
- `MoonStudioActivity.java`

- The app requests status using the named `requestSMLStatus()` AIDL method, then waits up to four seconds for `com.mobis.intent.action.SML_STATUS_CHANGED`.
- The receiver and status-extra delivery have not been verified in the actual EV6 app process.
- The controller captures a nine-byte response, validates RGB/brightness, checks Park, and saves the original values before writes.
- Restore is attempted from Stop and Activity destruction, but a restore failure on Activity destruction is intentionally swallowed and cannot be treated as confirmed restoration.
- The current manifest does not declare `RECORD_AUDIO`; the native MIC capture therefore has an additional likely permission/runtime blocker. This should be resolved through explicit state reporting, not by enabling microphone use contrary to the product requirement.

### Recommendation

On-car test in Park only: verify firmware identity, request status, confirm original-state capture, apply one static color, apply brightness, then restore. Confirm the returned/observed state after restore. Do not test dynamic modes until this sequence passes. If status is not received, do not enable writes.

## 5. Dual Color and cabin zones

- The HTML currently alternates the two selected colors over time.
- The controller exposes one global SML RGB setter.
- No independent left/right or door-zone control is implemented or established by the inspected mapping.

### Recommendation

Describe this as alternating dual-color mode until separate independently addressable zone APIs are verified. Do not promise simultaneous half-cabin color splitting.

## 6. Build 47 acceptance checklist

- [ ] Verify actual firmware identity using a confirmed read-only source.
- [ ] Verify Park status and the status-change broadcast in the normal app process.
- [ ] Capture the original 9-byte lighting state.
- [ ] Apply one static palette-mapped color and verify the cabin output.
- [ ] Apply brightness and verify the output.
- [ ] Restore original color and brightness and verify the result.
- [ ] Confirm audio UI never reports ACTIVE unless native capture is actually running.
- [ ] Keep live car-audio sync marked unavailable until an EV6 stereo feed is demonstrated.
- [ ] Keep Build 46 as the untouched baseline until the car test is complete.

## References inspected

- NEMESIS Build 46 source commit: `f9384268badc4e33c7582e3d211130e95b49d431`
- HMG-API EV6 map: `maps/kia/ev6/usa/260507/framework.json`
- HMG-API EV6 hook documentation: `docs/hook-points.md`
- HMG-API mapped MICOM data service: `framework.micomdata_service.register` / `unregister`

This audit is engineering documentation, not proof of on-vehicle behavior. No APK was built or hardware behavior validated by this audit.


## 7. Build 47 microphone-path correction (source change, not yet built)

Two source changes were committed on this audit branch after the initial review:

- `MoonStudioActivity.java`: removed construction and lifecycle of `AudioBandCapture`, removed the `NemesisAudio` JavaScript bridge, and removed the activity-destroy microphone stop call. The dormant `AudioBandCapture.java` source file remains in the repository but is no longer instantiated or exposed to the WebView.
- `index.html`: replaced the native-microphone and browser `getUserMedia` fallback with an explicit unavailable state. Attempting the live-audio action now reports that EV6 stereo audio is not connected and explicitly states that NEMESIS will not use the phone or head-unit microphone. Demo Signal remains separate for visual testing.

This is a deliberate product-truthfulness and privacy correction. It does **not** provide car-audio capture. The live car-audio feature remains unavailable pending on-device investigation of the EV6 audio route/HAL or another verified source.

Commits:
- Activity bridge removal: `95f5bd012bdc3de78dc69414b1706ab14fdb8928`
- UI microphone fallback removal: `3a0d4ac4bd0018838d911293c68a7d62f48a9c87`

The repository workflow currently triggers on pushes to `main` or manual dispatch; no branch build has run for this audit branch. These changes have not been compiled, installed, or tested in a vehicle. Do not distribute as a replacement APK until a build and review pass.


## 8. Firmware gate correction

The earlier firmware gate was incorrect: it read `ro.build.product2` and expected `CVEV.USA.S5W_M.V015.012.260507`. HMG-API's EV6 test fixtures and verified map identify the actual product string as `CVEV.USA.0000.V015.012.260507` in `ro.build.product`. The NEMESIS probe now reads that property and compares the exact mapped product string. This is source-corrected from HMG-API evidence; actual property values on this particular vehicle still need confirmation on-device.

Commit: `4a6801f921724aa1c7ca8e495ab9be64530e4d97`.

## 9. EV6 lighting status delivery limitation

HMG-API's EV6 `CarCallbacks.java` documents that a listener binder was accepted but the regular app-side status callback never received the SML status broadcast. HMG-API instead installs `LightRelay` in `system_server`, where it receives `com.mobis.intent.action.SML_STATUS_CHANGED` and forwards the 9-byte status through the privileged `hmgapi-vehicle` local socket to its vehicle daemon. Therefore NEMESIS's ordinary Activity-context `BroadcastReceiver` cannot be assumed to receive the status broadcast. Its current four-second wait and `LIGHT CONTROL READY` state are unverified and may time out on the target head unit.

Do not bypass this by guessing Binder transaction IDs or relaxing the requirement to capture original state. For standalone NEMESIS, a supported, permissioned status relay is still needed. If HMG-API is installed, investigate a documented integration/IPC contract rather than assuming its private local socket is available to third-party apps. Until then, fail closed and do not send lighting writes without a verified original-state capture.
