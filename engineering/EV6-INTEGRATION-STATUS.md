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

HMG-API's source distinguishes two mechanisms: a Binder callback listener registered from its vehicle daemon did not receive the SML event on the EV6, so HMG-API adds `LightRelay` inside `system_server`. That relay receives the system broadcast and forwards the 9-byte status to the HMG vehicle daemon, which runs without a normal Android app context.

NEMESIS is a normal Android app and registers a dynamic `BroadcastReceiver` using its Activity context. The HMG source does **not** prove that this ordinary app receiver will fail; it only proves the daemon's Binder callback route did not work. Whether NEMESIS's app-context receiver receives the protected/system broadcast on this exact firmware remains unverified and must be tested on the head unit before claiming original-state capture works.

If the app-context receiver is denied or receives no event, a standalone solution will need an approved privileged relay or another supported status source. Do not claim the relay exists until it is actually implemented and verified.

## Current implementation status

- Direct named MICOM method calls and Park/firmware gates exist in source.
- The status request now uses the mapped `requestSMLStatus()` method rather than a hard-coded transaction ID.
- The app-process status receiver is implemented, but delivery of the SML broadcast to a normal app remains unverified on the target firmware.
- Physical RGB/brightness writes, status readback, and restore have not been verified on the owner's EV6.
- Do not treat a successful Android build as proof of vehicle control.

## Safe test boundary

Until status capture and original-state preservation are solved and validated, do not enable lighting writes. Keep the vehicle parked for any later controlled test. First prove status readback; then prove one static color, brightness, and restoration. Rainbow/Dual Color and audio-reactive behavior come only after the basic write/read/restore cycle is confirmed.
