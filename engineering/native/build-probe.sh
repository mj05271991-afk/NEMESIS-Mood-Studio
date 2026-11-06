#!/usr/bin/env bash
# Build the standalone Moon Studio ALSA diagnostic for the EV6 ARMv7 head unit.
# Not a release installer. Does not enable the loopback mixer switch.
set -euo pipefail
cd "$(dirname "$0")/../.."
TINY="${TINYALSA_DIR:-third_party/tinyalsa}"
PIN="a7cd26a17fe2add2ff38d2f281ed0b919187a068"
if command -v zig >/dev/null 2>&1; then
  ZIG=(zig)
elif python3 -c "import ziglang" >/dev/null 2>&1; then
  ZIG=(python3 -m ziglang)
else
  echo "zig required (install zig or python3 -m pip install ziglang)" >&2; exit 2
fi
if [ ! -f "$TINY/include/tinyalsa/asoundlib.h" ]; then
  echo "tinyalsa missing: clone https://github.com/tinyalsa/tinyalsa into $TINY and checkout $PIN" >&2
  exit 2
fi
if [ -d "$TINY/.git" ]; then
  actual="$(git -C "$TINY" rev-parse HEAD)"
  if [ "$actual" != "$PIN" ]; then
    echo "tinyalsa revision mismatch: expected $PIN, got $actual" >&2
    exit 2
  fi
fi
mkdir -p build/native
"${ZIG[@]}" cc -target arm-linux-musleabihf -mcpu=cortex_a7 -static -Os \
  -I"$TINY/include" \
  -o build/native/moon_audio_probe \
  engineering/native/moon_audio_probe.c \
  "$TINY/src/pcm.c" "$TINY/src/pcm_hw.c" \
  "$TINY/src/limits.c" "$TINY/src/snd_card_plugin.c" -lm
"${ZIG[@]}" cc -target arm-linux-musleabihf -mcpu=cortex_a7 -static -Os \
  -I"$TINY/include" \
  -o build/native/moonaudiod \
  engineering/native/moonaudiod.c \
  "$TINY/src/pcm.c" "$TINY/src/pcm_hw.c" \
  "$TINY/src/limits.c" "$TINY/src/snd_card_plugin.c" \
  "$TINY/src/mixer.c" "$TINY/src/mixer_hw.c" -lm
file build/native/moonaudiod
file build/native/moon_audio_probe
echo "Native binaries ready: build/native/moon_audio_probe and build/native/moonaudiod"
echo "Do not install yet; native integration requires packaging and on-car validation."
