#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ZIG:?Set ZIG to the Zig compiler executable}"
mkdir -p arm32-static-build
"$ZIG" cc -target arm-linux-musleabihf -mcpu=cortex_a7 -static -O2 -std=c11 -Wall -Wextra -Werror -o arm32-static-build/moonstudiod moonstudiod.c moon_audio_backend.c -lm
file arm32-static-build/moonstudiod | grep -E 'ELF 32-bit.*ARM'
if readelf -l arm32-static-build/moonstudiod | grep -q INTERP; then
 echo 'Expected a fully static ARMv7 executable, found interpreter' >&2
 exit 3
fi
echo 'Built static ARMv7 offline daemon (no live ALSA or MICOM).'
