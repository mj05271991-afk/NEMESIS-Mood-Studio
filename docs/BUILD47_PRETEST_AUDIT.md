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

- `MoonStudioActivity.java`: removed construction and lifecycle of `AudioBandCapture`, removed the `NemesisAudio` JavaScript bridge, and removed the activity-destroy microphone stop call. The unused `AudioBandCapture.java` implementation was subsequently deleted from this branch.
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


## 10. Audio route investigation

A targeted search of the public HMG-API repository for `REMOTE_SUBMIX`, `AudioRecord`, `audioflinger`, and audio-output capture terms returned no implementation or mapped car-audio capture point. This does not prove no route exists on the vehicle, but there is currently no verified supported app-level stereo loopback path in the available map/repository. The head unit is Android API 19, so Android 10's AudioPlaybackCapture API is not an option. A privileged/system audio tap or a documented OEM service may be required; do not try undocumented Binder transactions or claim feasibility until verified on-device.

The old microphone capture implementation has now been deleted from the Build 47 audit branch entirely.

Commit: `65636c77380b285d292356b1769d4ea11a5d7964`.

## 11. HMG-API integration boundary

A source review of HMG-API's current EV6 implementation confirms that its SML status path is internal to HMG-API:

- `LightRelay` is installed in `system_server` and listens for `com.mobis.intent.action.SML_STATUS_CHANGED`.
- It forwards the nine-byte status to the HMG-API vehicle daemon over the local socket named `hmgapi-vehicle`.
- HMG-API's `CarCallbacks` notes that a listener binder registered through the regular app-side route was accepted but did not receive the EV6 SML status broadcast.
- HMG-API's hook-point map documents the MICOM methods for requesting status, setting RGB, and setting brightness, but the inspected files do not define a documented third-party application IPC contract for obtaining the relayed status or issuing ambient-light commands.

This makes HMG-API a valuable reference and potential integration dependency, but not yet a drop-in app-level backend for NEMESIS. Do not connect to the private local socket, assume its permissions, or duplicate its privileged relay. First identify a documented public IPC/service contract or obtain an explicit integration path from the HMG-API maintainers. Until then, standalone NEMESIS must remain fail-closed when it cannot capture and validate the original lighting state.

Reference source paths in VysseCodes/HMG-API:
- `core/runtime/java/hmgapi/hook/LightRelay.java`
- `core/runtime/java/hmgapi/vehicle/CarCallbacks.java`
- `docs/hook-points.md` (EV6 MICOM SML method map)

This is a source-level integration finding, not an on-vehicle test result.


## 12. Supported HMG-API vehicle-command integration path

A further source review found a documented app-level route that is materially better than attempting to consume HMG-API's private vehicle socket:

- HMG-API's `docs/mod-reference.md` explicitly supports declaring a mod's own APK under `apps` and vehicle commands under `vehicle.commands`.
- The documented `hmgapi-client.jar` exposes `HmgApi.connect()`, `available()`, `mod()`, `vehicle(...)`, and `command(name, args...)` to an app registered to an enabled mod. Calls are authenticated by the app's UID and the enabled mod declaration; this is the supported client boundary, not the private `@hmgapi-vehicle` socket.
- The HMG-API client documentation gives `api.command("ambient_light", "color", "ff8000")` as an example. The vehicle command docs specify `color <rrggbb>`, `brightness <0-10>`, `release`, and `owner_change release|keep`.
- HMG-API's ambient-light command implementation is documented as a single-holder lease: it saves the owner's light, restores it when the mod releases/turns off/is removed or its process disappears, honors the owner's `block_ambient_light` switch, and limits changes to 10 per second. Its vehicle readings include `gear.park`.
- HMG-API's mod reference says an existing Gen5W project can be offered as an HMG-API mod without changing its existing package; HMG-API runs on top of NEMESIS. This suggests an optional NEMESIS HMG-API mod package can coexist with the standalone app.

### Proposed architecture (not implemented)

1. Keep NEMESIS standalone as the primary product and preserve its existing package/install route.
2. Add a separate HMG-API mod package that declares the NEMESIS APK and only the required vehicle capabilities, initially `gear.park` and `ambient_light`.
3. Bundle/use the documented HMG-API client library in the registered app and call the named `ambient_light` command API on a background thread. Use the daemon's lease/restore behavior rather than duplicating HMG-API's private relay or socket.
4. When HMG-API is absent, retain the standalone backend only if its own status-capture and restore sequence is proven on the actual EV6. Never run both backends against the same lighting session.
5. Keep the UI explicit about which backend is active and fail closed if the selected backend is unavailable or refuses a command.

### Important qualification

The HMG-API source documents this integration contract, but NEMESIS has not yet integrated the client library, packaged a mod, or tested this route in the car. The vehicle-command introduction says the command set was built and tested off-car against a fake car; the ambient-light section separately describes US EV6 260507 behavior and owner-light restoration. Because those statements are not fully consistent about on-car command validation, confirm the current implementation and test with the HMG-API maintainer/build before treating the route as production-ready.

References inspected in VysseCodes/HMG-API:
- `docs/mod-reference.md` — app registration, `hmgapi-client.jar`, and `vehicle.commands`.
- `core/client/java/hmgapi/client/HmgApi.java` — authenticated app client and `command(...)` API.
- `docs/vehicle-commands.md` — ambient-light command, lease, owner restore, block switch, and rate limit.
- `core/runtime/java/hmgapi/engine/Vehicle.java` and `core/runtime/java/hmgapi/vehicle/Hub.java` — command enforcement and vehicle-service routing.

This is a source-level integration design, not an implemented feature or an on-car NEMESIS test.

## 13. HMG-API ambient-light first-use restore gap

A deeper read of the current HMG-API core/runtime/java/hmgapi/vehicle/Commands.java found a safety gap that must be resolved before NEMESIS relies on the documented hmgapi-client.jar command path:

- takeLight(mod) asks for the car's current light status and polls for up to ten 50 ms intervals when lightState() is null.
- If the status still has not arrived, takeLight does not fail or return an error. It leaves lightSaved null, assigns lightBy = mod, and returns to the caller.
- The ambient_light color and ambient_light brightness handlers then proceed to issue the requested change.
- On release, releaseLight() detects that lightSaved is null, logs that the owner's light was not reported, and explicitly reports that it was not restored.

Therefore the HMG-API client command contract is authenticated and documented, but the current ambient-light implementation does not strictly fail closed when the initial owner-state report is missing. The general docs' lease/restore description must not be interpreted as a guarantee that the first-use state capture always succeeds.

### Required upstream correction / acceptance criteria

Before NEMESIS enables this backend, HMG-API should reject the first color/brightness command unless a valid owner light state has been captured. The rejection should leave lightBy unset and send no lighting write. Tests should cover:
- status already available before first command;
- status arriving during the bounded wait;
- status never arriving (command refused, no write, no lease);
- valid saved state restored on release;
- owner changes during a held lease;
- mod disable/process death and the resulting restore path.

Until that correction is merged and tested, NEMESIS must treat HMG-API ambient-light integration as not safe to activate, even though the client API and mod declaration are documented. This finding is based on source inspection; no vehicle command was sent and no car behavior was tested.

Reference: VysseCodes/HMG-API core/runtime/java/hmgapi/vehicle/Commands.java, takeLight, ambient_light handler, and releaseLight.

### Existing upstream test confirms the gap

The HMG-API unit test at core/runtime/test/hmgapi/vehicle/CommandsTest.java explicitly sets the fake car's light state to null, expects a brightness command to return ok, then expects release to report not restored. This is not merely an untested edge case: the current off-car test suite codifies the behavior. NEMESIS should request an upstream change to make missing initial state a refusal, with a revised test asserting that no color/brightness write is sent and no lease is taken.

## 14. HMG-API app lifecycle and standalone-preserving bridge

Review of HMG-API core/runtime/java/hmgapi/engine/ModApps.java and core/client/java/hmgapi/client/HmgApi.java adds an important packaging constraint:

- HMG-API only authenticates an app as a mod app when its package is declared in an enabled mod; HmgapiClient `mod()` returns null when the caller is not an enabled mod app.
- HMG-API's ModApps manager installs declared APKs, disables them when the owning mod is turned off, and uninstalls them when the mod is removed.
- If the same package is already installed outside HMG-API, HMG-API leaves it alone by default. Its `hmgapi.json` `overrides.replace_apps` setting can allow the mod to take over that package, but then the app becomes subject to the mod's disable/remove lifecycle.
- NEMESIS's package is `com.nemesis.moonstudio`; the Build 47 Android manifest and Gradle applicationId agree.
- The NEMESIS installer data is generated inside `.github/workflows/android.yml`, rather than checked in as a repository-root NEMESIS.data file. It declares the same package.

### Architecture correction

Do not register the main `com.nemesis.moonstudio` APK as an HMG-API-managed app if the requirement is that the standalone NEMESIS installation remain independent when the optional HMG-API mod is disabled or removed. Do not use `overrides.replace_apps` for the main app as a shortcut.

Preferred future design: a separate, small NEMESIS HMG-API bridge APK with its own package, declared and managed by the optional HMG-API mod. The standalone NEMESIS app remains installed and independently launchable. The bridge can use the documented hmgapi-client.jar and its own mod authorization; it must communicate with the main app through a deliberately designed, permission-protected IPC boundary. Both apps must be signed consistently for any signature-level permission. The bridge must fail closed if HMG-API is absent, the mod is disabled, or the ambient-light command refuses.

This bridge architecture is a design recommendation only; no bridge APK, IPC contract, HMG-API mod folder, or HMG-API integration has been implemented or tested. It also does not remove the ambient-light first-use restore gap in Section 13: upstream fail-closed behavior and actual EV6 validation remain prerequisites.

References: VysseCodes/HMG-API core/runtime/java/hmgapi/engine/ModApps.java; core/client/java/hmgapi/client/HmgApi.java; docs/mod-reference.md. NEMESIS app identity: app/build.gradle and app/src/main/AndroidManifest.xml.

## 15. Standalone restore state is process-local

Inspection of app/src/main/java/com/nemesis/moonstudio/Ev6LightingController.java and MoonStudioActivity.java found that the original SML snapshot is held only in the static in-memory `saved` array. It is cleared after a successful restore. MoonStudioActivity calls restore() from onDestroy(), but that is only a best-effort normal lifecycle callback.

If Android kills the process, the app crashes, or the head unit loses power, the process-local snapshot is lost and onDestroy() is not guaranteed to run. The current installer declares only the APK; it does not install a persistent standalone recovery service or durable state journal.

### Required standalone recovery design

Before the first lighting write, persist and validate the captured original state atomically. A recovery component must own the session independently of the Activity, monitor process/session lifecycle, attempt restore on orderly stop, and recover a pending session after app restart or head-unit reboot when the vehicle is safely in Park. The UI must show a pending-restore/recovery state and must not start another lighting session over an unresolved one. Define behavior when the car is not in Park at recovery time; do not silently discard the snapshot.

An Android service in the same app process is not by itself proof against process death or force-stop. Any root/system recovery service or watchdog needs a separate security, installer, permission, and lifecycle review. Until a durable recovery design is implemented and tested, standalone direct writes remain an unverified test path and should not be described as crash-safe.

This is a source-level lifecycle finding, not an observed vehicle failure. No lighting writes or vehicle tests were performed during this audit.

## 16. HMG-API upstream test confirms the exact fail-open path

Located and read `core/runtime/test/hmgapi/vehicle/CommandsTest.java` (the path is `core/runtime/test/...`, not `core/runtime/tests/...`). Its ambient-light tests explicitly assert this sequence:

1. First `ambient_light color ff8000` calls `lightStatus`, then immediately writes RGB even though FakeCar.lightState() is still null.
2. After a holder change, `tick()` releases the holder and requests status again.
3. Another mod is allowed to set brightness with no state snapshot.
4. Release returns `not restored: the car hadn't reported its light yet`.

The matching implementation is `core/runtime/java/hmgapi/vehicle/Commands.java`: `takeLight(String)` polls ten times at 50 ms but returns `void` and sets `lightBy = mod` even if `lightSaved` remains null. Both color and brightness handlers then proceed to the vehicle write. `releaseLight()` clears the lease and returns without restoring when the snapshot is null.

### Proposed upstream change (not applied)

Make acquisition a prerequisite, not a best-effort side effect:

- Replace `void takeLight(String mod)` with a result-bearing acquisition method that obtains a valid snapshot before setting `lightBy`.
- If no valid state arrives within the bounded wait, return a refusal such as `refused: the car's original ambient light state wasn't reported; no change was sent`.
- In both `color` and `brightness` branches, return that refusal immediately; do not update `lightAt`, add a `lightSent` record, or call `car.lightColor` / `car.lightBrightness`.
- Set `lightBy` only after validating and retaining the complete snapshot (RGB/theme channels and brightness in supported ranges).
- Preserve the current release behavior for a valid snapshot, and ensure no lease remains after a failed acquisition.

### Required test changes

Update FakeCar to simulate: (a) no status ever arrives; (b) status arrives during the bounded wait; (c) a valid status is already present. Assert that case (a) returns refusal for both color and brightness, makes only the `light status` request, leaves `lightBy` and `lightSaved` unset, and sends no write. Assert cases (b) and (c) capture the exact owner state, acquire the lease, and restore all four values on release. Also assert malformed/out-of-range status is treated exactly like no status. Keep the existing valid-theme restore test.

These are concrete proposed upstream code/test changes based on current source. They have not been submitted to or merged into VysseCodes/HMG-API, and the NEMESIS repository remains source-audit only.

## 17. EV6 SML status delivery path is not proven for the standalone app

Further source review of HMG-API `core/runtime/java/hmgapi/hook/LightRelay.java`, `vehicle/CarCallbacks.java`, and `vehicle/Hub.java` clarifies the status-delivery architecture:

- HMG-API registers `com.mobis.intent.action.SML_STATUS_CHANGED` in `system_server` via `LightRelay`, then relays the 9-byte payload to the vehicle service over its private local socket.
- HMG-API explicitly documents that a binder listener registered against the car's system-service callback interface was accepted but never received SML status broadcasts on the EV6. That is why it uses the `system_server` broadcast relay.
- HMG-API's vehicle process itself cannot receive Android broadcasts because it runs under `app_process` without an application context.

NEMESIS Build 47 currently registers a dynamic `BroadcastReceiver` from its ordinary Activity process and expects that receiver to receive the SML status broadcast after calling `requestSMLStatus()`. HMG-API's architecture does not prove that this ordinary app receiver is blocked, but it also provides no evidence that the app receiver works. This is a hard on-device validation gate: do not treat the receiver registration or successful method invocation as proof that the original state was captured.

Before any write can be enabled, test the standalone app on the exact EV6 firmware and establish that (1) the app can look up and invoke `ISystemService.requestSMLStatus()`, (2) the app's receiver actually receives a status broadcast, (3) the payload is exactly 9 bytes and passes range validation, and (4) the capture corresponds to the current owner's state. If ordinary-app delivery fails, use a supported relay/service integration with a clear permission boundary; do not guess Binder transaction codes or weaken the requirement to capture a valid original state.

Source references: HMG-API `core/runtime/java/hmgapi/hook/LightRelay.java`, `core/runtime/java/hmgapi/vehicle/CarCallbacks.java`, `core/runtime/java/hmgapi/vehicle/Hub.java`. No EV6 hardware test was performed.

## 18. Recovery journal must not overwrite a newer owner choice

A durable snapshot alone is not a safe crash-recovery solution. The car's own lighting menu may be used after NEMESIS loses its process or while the app is closed. Blindly replaying the original snapshot after a reboot could overwrite that newer owner choice.

### Safer recovery journal protocol

Persist a versioned journal atomically before the first vehicle write. Minimum fields:

- schema version and session ID;
- firmware/product identity and capture timestamp;
- original 9-byte status (all RGB/theme channels plus brightness and any validated status fields);
- last NEMESIS-commanded full status, updated only after the corresponding command is confirmed by a fresh status report;
- session phase (`CAPTURED`, `WRITE_PENDING`, `ACTIVE`, `RESTORE_PENDING`, `CONFLICT`, `RESTORED`);
- monotonically increasing command sequence to reject stale confirmations.

Recovery must request a fresh status and validate firmware, payload length, channel ranges, and Park before doing anything. If the fresh state matches the last confirmed NEMESIS state, restore the original state. If it differs, do not overwrite it: enter `CONFLICT`, keep the journal, show the current and saved states, and require an explicit owner decision. If no fresh state arrives or Park cannot be confirmed, keep the journal pending and send no writes. Never clear the journal merely because an Activity or service started or stopped.

Write ordering: atomically persist `WRITE_PENDING` and intended target before sending a command; send one command; wait for a fresh status confirmation; atomically update the last-confirmed state and phase. On restore, persist `RESTORE_PENDING`, restore color and brightness with status confirmation between them, then mark `RESTORED` and clear/rotate the journal only after the final state is confirmed. A crash at any point therefore leaves enough information to retry safely or surface a conflict.

### Recovery lifecycle

An Activity `onDestroy()` callback is only a best-effort cleanup path. A future implementation needs an explicit user-visible `Restore`/`Recover` action on every launch, plus a boot/session recovery trigger only if the target EV6's Android 9 build and installer support it. A boot receiver or background service must not issue blind writes: it follows the same fresh-status, Park, and compare-before-restore rules. Force-stop and power-loss behavior must be tested on the vehicle; Android may defer or suppress automatic startup.

This protocol is a design requirement, not implemented code. It deliberately prefers a visible unresolved recovery state over silently restoring stale values or discarding the saved owner state.
