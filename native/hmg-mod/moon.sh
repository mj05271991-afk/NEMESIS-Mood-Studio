#!/system/bin/sh
# Moon Studio standalone service, derived from Kyle's HMG mod lifecycle.
# Explicit opt-in required. Never run concurrently with Kyle's Light Vibes.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
SETTINGS=/data/data/com.nemesis.moonstudio/files/settings
ENABLE=/data/data/com.nemesis.moonstudio/files/enable_moon_lighting
LOCK=/data/hmgapi/moon-studio-lighting.lock
# Keep the HMG service alive while disabled, so the app can opt in later.
# No MICOM or ALSA access occurs until the marker and enabled setting exist.
while [ ! -f "$ENABLE" ] || [ "$(cat "$SETTINGS/enabled" 2>/dev/null)" != "1" ]; do sleep 2; done
[ -x "$MOD/moon-lightd" ] || exit 1
# Fail closed if Kyle's daemon appears active; both use the same MICOM and ALSA mixer.
if pidof lightvibesd >/dev/null 2>&1; then
  echo "Moon Studio: Light Vibes is active; refusing concurrent lighting control" >&2
  exit 2
fi
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "Moon Studio: another lighting session owns the lock" >&2
  exit 3
fi
cleanup() { rmdir "$LOCK" 2>/dev/null; }
trap cleanup EXIT
trap 'exit 143' TERM
trap 'exit 130' INT
mkdir -p "$SETTINGS"
# The daemon independently restores the original MICOM state on normal exit.
# HMG runs this script independently of Moon Studio's UI.
"$MOD/moon-lightd" "$SETTINGS" "$MOD/defaults" &
CHILD=$!
trap 'kill "$CHILD" 2>/dev/null; wait "$CHILD" 2>/dev/null; cleanup; exit 143' TERM
trap 'kill "$CHILD" 2>/dev/null; wait "$CHILD" 2>/dev/null; cleanup; exit 130' INT
wait "$CHILD"
STATUS=$?
cleanup
exit "$STATUS"
