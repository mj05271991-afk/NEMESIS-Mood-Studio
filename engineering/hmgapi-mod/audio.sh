#!/system/bin/sh
# Moon Studio HMG-API lighting service, based on the v0.2.0 lifecycle.
# Never compete with Kyle's active Light Vibes daemon for MICOM/ALSA.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
BIN="$MOD/moonaudiod"
SETTINGS=/data/data/com.nemesis.moonstudio/files/settings
DEFAULTS="$MOD/defaults"
LOCK=/data/hmgapi/moon-studio-lighting.lock

[ -f "$BIN" ] || { echo "Moon Studio: missing $BIN" >&2; exit 1; }
chmod 755 "$BIN" || exit 1

# Keep the service alive until the user explicitly enables Moon Studio.
# Do not touch app-private settings ownership or start audio capture while disabled.
while :; do
  if [ "$(cat "$SETTINGS/enabled" 2>/dev/null)" = "1" ] && ! pidof lightvibesd >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

# A stale lock is deliberately not removed automatically: two lighting daemons
# competing for the same ALSA/MICOM device can leave vehicle settings inconsistent.
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "Moon Studio: lighting lock already held" >&2
  exit 3
fi
CHILD=
cleanup() {
  [ -z "$CHILD" ] || { kill "$CHILD" 2>/dev/null; wait "$CHILD" 2>/dev/null; }
  rmdir "$LOCK" 2>/dev/null
}
trap 'cleanup; exit 143' TERM
trap 'cleanup; exit 130' INT
trap 'cleanup' EXIT

# Background service is independent of the Android dashboard lifecycle.
"$BIN" "$SETTINGS" "$DEFAULTS" &
CHILD=$!
wait "$CHILD"
STATUS=$?
CHILD=
exit "$STATUS"
