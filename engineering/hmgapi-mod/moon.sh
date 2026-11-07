#!/system/bin/sh
# Moon Studio v0.2.0-compatible HMG-API service entrypoint.
# Independent of dashboard; only runs with explicit user opt-in.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
SETTINGS=/data/data/com.nemesis.moonstudio/files/settings
ENABLE=/data/data/com.nemesis.moonstudio/files/enable_moon_lighting
LOCK=/data/hmgapi/moon-studio-lighting.lock
while [ ! -f "$ENABLE" ] || [ "$(cat "$SETTINGS/enabled" 2>/dev/null)" != "1" ]; do sleep 2; done
[ -x "$MOD/moonaudiod" ] || exit 1
if pidof lightvibesd >/dev/null 2>&1; then
  echo "Moon Studio: Light Vibes already owns lighting" >&2
  exit 2
fi
if ! mkdir "$LOCK" 2>/dev/null; then
  echo "Moon Studio: lighting lock held" >&2
  exit 3
fi
CHILD=
cleanup() {
  [ -z "$CHILD" ] || { kill "$CHILD" 2>/dev/null; wait "$CHILD" 2>/dev/null; }
  rmdir "$LOCK" 2>/dev/null
}
trap 'cleanup; exit 143' TERM
trap 'cleanup; exit 130' INT
trap cleanup EXIT
mkdir -p "$SETTINGS"
"$MOD/moonaudiod" "$SETTINGS" "$MOD/defaults" &
CHILD=$!
wait "$CHILD"
STATUS=$?
CHILD=
exit "$STATUS"
