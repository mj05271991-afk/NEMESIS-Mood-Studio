# EV6 integration status

Target reported by owner: 2022 Kia EV6.
Expected head-unit fingerprint:
- `ro.product.model=wp_daudioplus`
- `ro.product.device=wp_daudioplus_cvev_us`
- `ro.build.product2=CVEV.USA.S5W_M.V015.012.260507`
- Android SDK/runtime level: API 19.

## Current implementation: read-only diagnostic stage

`Ev6LightingProbe` is firmware-gated and performs only the documented SML status request:
- Binder service: `com.hkmc.micom.system`
- Interface descriptor: `com.hkmc.micom.ISystemService`
- Transaction: `74` (request status)
- Response broadcast: `com.mobis.intent.action.SML_STATUS_CHANGED`
- Response extra: `com.mobis.intent.extra.SML_STATUS`
- Expected response: 9 bytes containing 16-bit big-endian RGB followed by brightness, dim option, and drive mode.

The UI's **CHECK VEHICLE LIGHTS** action requests status and displays validated values. It fails closed if any of the three system-property fingerprint values differ, if the Binder service is absent, if the transaction fails, if no broadcast arrives, or if the response length/ranges are invalid.

## Safety boundary

This version contains no Binder lighting write calls. In particular, it does not issue transactions 75 or 76, does not set brightness or RGB, and does not change the vehicle's lighting. It does not use HMG-API.

The transaction and response contract comes from public EV6 head-unit research and is not yet confirmed on this owner's exact firmware. A successful diagnostic result would be evidence of read-path compatibility only, not permission to enable writes.

## Next validation

1. Build the APK and install it through NEMESIS.
2. With the vehicle in a safe parked state, tap **CHECK VEHICLE LIGHTS** once.
3. Record the exact diagnostic status and returned values.
4. Repeat the read-only check after changing a color or brightness using factory settings, and confirm the reported values change accordingly.
5. Only after repeatable readback should a separately reviewed write/restore design be considered. No write implementation is authorized by this diagnostic stage.
