# EV6 integration status

Target reported by owner: 2022 Kia EV6 GT-Line.

Expected head-unit fingerprint (must still be confirmed on the actual unit):
- `ro.product.model=wp_daudioplus`
- `ro.product.device=wp_daudioplus_cvev_us`
- `ro.build.product2=CVEV.USA.S5W_M.V015.012.260507`
- Android runtime: API 19.

## Firmware-specific API findings

The HMG-API EV6 USA 260507 map identifies the MICOM interface:
- Service: `com.hkmc.micom.system`
- Interface: `com.hkmc.micom.ISystemService`
- Read status request: `requestSMLStatus()`
- RGB write: `setSMLRGB(byte[], byte[], byte[])`
- Brightness write: `setSMLConfiguration(int, int)`, with setting 5
- Park check: `getGearPStatus()`, where 1 means Park

The documented color channels are 2-byte big-endian values. The vehicle accepts its 64-color custom palette. Brightness raw values are 1=off and 2-11=levels 1-10. NEMESIS uses a 100 ms write interval (maximum 10 writes/sec) and checks firmware/Park before writes.

**Do not use guessed raw Binder transaction IDs.** The source map confirms the named interface methods; it does not establish a safe hard-coded transaction number for NEMESIS.

## Important status/restore blocker

HMG-API's own source explicitly says a listener registered from the ordinary app-process Binder callback path did not receive `SML_STATUS_CHANGED` on the EV6. HMG-API solves this with `LightRelay` running inside `system_server`: it receives the system broadcast and forwards the 9-byte status to the privileged HMG vehicle service over a local socket.

NEMESIS currently registers a receiver in its ordinary app process. That is **not equivalent** to HMG-API's privileged relay and cannot be assumed to receive the event. Therefore NEMESIS cannot yet guarantee it has captured the owner's original color/brightness. The app must not report LIGHT CONTROL READY or enable writes until a supported status source is proven.

A normal APK cannot install a receiver into `system_server`. A standalone implementation needs a separately installed privileged component/approved system hook with a secure relay, or an explicitly supported HMG-assisted integration. Do not fake this capability in the app process.

## Current implementation status

- Direct named MICOM method calls and Park/firmware gates exist in source.
- The status request now uses the mapped `requestSMLStatus()` method rather than a hard-coded transaction ID.
- The app-process status receiver remains insufficient until the privileged relay question is solved.
- Physical RGB/brightness writes, status readback, and restore have not been verified on the owner's EV6.
- Do not treat a successful Android build as proof of vehicle control.

## Safe test boundary

Until status capture and original-state preservation are solved and validated, do not enable lighting writes. Keep the vehicle parked for any later controlled test. First prove status readback; then prove one static color, brightness, and restoration. Rainbow/Dual Color and audio-reactive behavior come only after the basic write/read/restore cycle is confirmed.
