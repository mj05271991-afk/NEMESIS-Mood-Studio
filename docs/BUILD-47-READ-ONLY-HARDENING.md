# Build 47 read-only hardening notes

## Changes in this pass

- The Android JavaScript bridge now rejects color, brightness, and release calls at the Activity boundary. The controller's independent hard-false output gate remains in place.
- Activity teardown no longer invokes the controller restore method. This diagnostic flow does not capture a session snapshot, so teardown must not imply that a restore transaction is appropriate.
- A late Binder exception is ignored after the probe has already completed or timed out.
- The status result is labeled **STATUS RECEIVED**, not captured or connected. The request screen text now describes a read-only status request rather than suggesting that control will be enabled.
- The vehicle panel is labeled EV6 diagnostics/read-only; the secondary button says STOP PREVIEW; the notice explicitly says this build does not save, change, or restore vehicle lighting.

## Remaining trust boundary

The Activity dynamically registers for `com.mobis.intent.action.SML_STATUS_CHANGED` with no sender permission. No evidence in the current repository proves that the action is protected by the target EV6 Android framework. A valid-looking nine-byte payload therefore cannot yet be treated as authenticated vehicle state. The SML broadcast also carries no request token, so a received event cannot be conclusively correlated with the immediately preceding `requestSMLStatus()` call.

Do not use this broadcast alone to authorize a lighting write or capture a restore baseline. Before a write-capable build, establish sender authentication on the exact EV6 firmware or use a supported authenticated HMG-API status path after its first-use snapshot gap is fixed and tested.

## Verification status

- Source edits are committed to `build47-pretest-audit`.
- No Gradle build, APK, installation, EV6 test, or lighting write was performed in this pass.
- Build 47 remains a source-only, read-only audit branch. Build 46 remains the last known successful CI baseline.

## Additional Activity hardening

- JavaScript-interface refusal callbacks now post WebView updates onto the Android UI thread. Android may invoke `@JavascriptInterface` methods on a background thread, so direct WebView updates from the bridge were unsafe.
- The four-second probe timeout is now stored per active probe and canceled when that probe finishes. Previously, a timeout left over from an earlier request could fire during a later request and incorrectly terminate it.

## Manifest and receiver review

- The launcher Activity has an intent filter and does not explicitly declare `android:exported`. On this app's legacy target configuration the launcher component is expected to be externally launchable; make the launcher export decision explicit when modernizing the target SDK.
- The SML receiver is dynamically registered without a sender permission. This remains unresolved: do not invent a permission string or assume the broadcast is protected. Verify the protected-broadcast policy on the exact EV6 framework image, or replace this path with an authenticated supported API.
- The JavaScript bridge is attached to the local bundled asset page. Keep that WebView restricted to trusted local content; do not load remote or user-controlled HTML into the same WebView while the bridge is attached.
