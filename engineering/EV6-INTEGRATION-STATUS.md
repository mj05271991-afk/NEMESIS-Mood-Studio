# EV6 integration status

Target: 2022 Kia EV6, firmware CVEV.USA.S5W_M.V015.012.260507, Android SDK 19.

The current package intentionally contains no lighting-control transactions. The research notes for Binder transaction IDs are not a substitute for validation on this exact firmware. Do not add writes until a read-only status probe, exact firmware gate, baseline capture, and restore/readback procedure have been tested on the vehicle.

HMG-API is explicitly excluded.
