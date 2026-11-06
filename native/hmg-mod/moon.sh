#!/system/bin/sh
# Moon Studio HMG service staging: independent of the Android UI.
# This service is disabled until an explicit opt-in marker exists.
MOD=/data/hmgapi/mods/${HMGAPI_MOD:-moon-studio}
SETTINGS=/data/data/com.nemesis.moonstudio/files
[ -f "$SETTINGS/enable_native_audio" ] || exit 0
[ -x "$MOD/moon-alsa-probe" ] || exit 1
# No MICOM commands: only audio analysis. Each run restores the mixer state.
while [ -f "$SETTINGS/enable_native_audio" ]; do
  "$MOD/moon-alsa-probe" --monitor-rgb >> "$SETTINGS/native-audio.log" 2>&1
  sleep 2
done
