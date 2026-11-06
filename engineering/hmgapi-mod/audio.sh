#!/system/bin/sh
# Development staging only; this mod must not be packaged until the daemon is built and tested.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
BIN="$MOD/moonaudiod"
SETTINGS=/data/data/com.nemesis.moonstudio/files/settings
DEFAULTS="$MOD/defaults"
if [ ! -x "$BIN" ]; then
  echo "Moon Studio audio daemon missing: $BIN" >&2
  exit 1
fi
exec "$BIN" "$SETTINGS" "$DEFAULTS"
