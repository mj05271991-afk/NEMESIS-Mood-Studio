# Moon Studio standalone HMG lighting service — NOT yet vehicle verified
This service runs Kyle's proven full audio-to-MICOM daemon (adapted under AGPL-3.0)
using Moon Studio's own identity, settings directory and mod path. It is
independent of the Android activity and therefore can continue after closing the UI.

The service is disabled unless `/data/data/com.nemesis.moonstudio/files/enable_moon_lighting`
exists. It refuses to start while Kyle's `lightvibesd` is running, and uses
an exclusive directory lock for Moon Studio instances. Never create the marker
until compatibility, MICOM restore behavior, and concurrent-writer exclusion
have been tested on the EV6. A stale lock after a crash must be diagnosed, not
blindly deleted. `exec` means shell traps do not run after daemon startup;
the daemon itself must restore the original light state on normal exit.

Kyle's daemon defaults enable effects unless overridden. The packaged
`defaults/enabled` is set to 0; explicit app settings must enable it.
