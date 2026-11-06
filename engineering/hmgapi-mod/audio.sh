#!/system/bin/sh
# Moon Studio native service (staging; do not ship until tested).
# This service uses only Moon Studio paths; never stops Kyle's app.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
BIN="$MOD/moonaudiod"
SETTINGS=/data/data/com.nemesis.moonstudio/files/settings
DEFAULTS="$MOD/defaults"

if [ ! -f "$BIN" ]; then
  echo "Moon Studio audio daemon missing: $BIN" >&2
  exit 1
fi
# Extracted ZIP contents may lose executable permissions.
chmod 755 "$BIN" || exit 1
if [ ! -d "$SETTINGS" ]; then
  mkdir -p "$SETTINGS" || {
    echo "Cannot create Moon Studio settings: $SETTINGS" >&2
    exit 1
  }
fi
# Native daemon defaults to disabled; settings must explicitly enable it.
exec "$BIN" "$SETTINGS" "$DEFAULTS"
