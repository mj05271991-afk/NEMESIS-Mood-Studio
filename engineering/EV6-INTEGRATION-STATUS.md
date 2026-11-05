# EV6 integration status

Target reported by owner: 2022 Kia EV6 GT-Line.
Expected head-unit fingerprint:
- `ro.product.model=wp_daudioplus`
- `ro.product.device=wp_daudioplus_cvev_us`
- `ro.build.product2=CVEV.USA.S5W_M.V015.012.260507`
- Android runtime: API 19.

## Implemented in NEMESIS source

NEMESIS now has a standalone direct SML controller; it does not require HMG-API:
- Read current lighting state using `ISystemService` status transaction 74 and the SML status broadcast.
- Save the original RGB and brightness before enabling writes.
- Set RGB using `ISystemService.setSMLRGB(byte[], byte[], byte[])` (documented Binder method 78); each channel is encoded as a two-byte big-endian value.
- Set brightness using `ISystemService.setSMLConfiguration(5, rawLevel)` (documented Binder method 77); raw values 1=off and 2-11=levels 1-10.
- Map requested colors to the EV6's 64 supported custom colors.
- Limit writes to no more than 10 per second.
- Require the exact expected firmware and Park status before every write.
- Restore the captured original color and brightness on STOP / RELEASE or disconnect, and attempt restore when the activity is destroyed.

The source is wired to Static, Rainbow, Dual Color, color selection, and manual brightness. Audio-reactive and party modes remain out of this test build.

## Not yet validated

The controller code has been committed, but compilation and actual vehicle operation must still be verified. In particular:
- The app process must have permission to access the MICOM system service and invoke its SML methods on this head unit.
- The 74 status broadcast must arrive with the expected nine-byte payload.
- Method 77/78 calls must succeed and the physical lights must respond.
- Restore behavior must be confirmed on the owner's exact firmware.

The interface method names, signatures, palette, channel encoding, and command values are sourced from HMG-API's EV6 USA 260507 hook map and vehicle command implementation. HMG-API is used only as source documentation; NEMESIS does not call its runtime or depend on it.

## Safe first vehicle test

1. Keep the EV6 parked and select Custom Color in the factory mood-lamp settings.
2. Launch NEMESIS and press CONNECT & ENABLE LIGHTS.
3. Confirm the UI reports LIGHT CONTROL READY before selecting a color.
4. Test one static preset first, then brightness.
5. Test Rainbow and Dual Color only after Static writes and readback work.
6. Press STOP / RELEASE and confirm the original color and brightness return.

If connection, Park validation, or status capture fails, the app must not send lighting writes. If a write fails, record the exact UI error and do not repeatedly retry. This build is not considered vehicle-validated until the physical response and restoration have both been confirmed.
