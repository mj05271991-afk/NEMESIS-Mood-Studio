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
# Do not create the app-private settings directory as root.
# Android creates it with the app's UID when Moon Studio first launches.
# Until then, the daemon reads the packaged fallback defaults.
# Native daemon defaults to disabled; settings must explicitly enable it.
exec "$BIN" "$SETTINGS" "$DEFAULTS"
