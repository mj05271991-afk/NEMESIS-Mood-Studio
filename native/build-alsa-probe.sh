#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
: "${ZIG:?Set ZIG to the Zig compiler}"
TINY_REV=a7cd26a17fe2add2ff38d2f281ed0b919187a068
TINY="$ROOT/third_party/tinyalsa"
if [ ! -d "$TINY/.git" ]; then
 mkdir -p "$ROOT/third_party"
 git clone https://github.com/tinyalsa/tinyalsa "$TINY"
fi
git -C "$TINY" checkout --detach "$TINY_REV"
OUT="$ROOT/arm32-static-build"
mkdir -p "$OUT"
"$ZIG" cc -target arm-linux-musleabihf -mcpu=cortex_a7 -static -Os -I"$TINY/include" -o "$OUT/moon-alsa-probe" "$ROOT/moon_alsa_probe.c" "$TINY/src/mixer.c" "$TINY/src/mixer_hw.c" "$TINY/src/snd_card_plugin.c"
file "$OUT/moon-alsa-probe" | grep -E 'ELF 32-bit.*ARM'
! readelf -l "$OUT/moon-alsa-probe" | grep -q INTERP
echo 'Static ARMv7 read-only tinyalsa probe built; car execution unverified.'
