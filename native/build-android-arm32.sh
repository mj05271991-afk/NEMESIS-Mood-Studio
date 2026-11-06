#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to an installed Android NDK directory}"
HOST="$(find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [[ -z "$HOST" || ! -x "$HOST/bin/clang" || ! -x "$HOST/bin/llvm-ar" || ! -d "$HOST/sysroot/usr/include" ]]; then
  echo 'Android NDK LLVM compiler/sysroot missing' >&2; exit 2
fi
OUT="${MOON_BUILD_OUT:-$ROOT/arm32-ndk-build}"
mkdir -p "$OUT"
files=(native_controller native_hmg_adapter native_lighting_session native_ownership_lock native_transport_gate native_verified_controller moon_audio_backend)
for f in "${files[@]}"; do
  "$HOST/bin/clang" --target=armv7a-linux-androideabi21 --sysroot="$HOST/sysroot" -std=c11 -D_GNU_SOURCE -Wall -Wextra -Werror -fPIC -O2 -c "$ROOT/$f.c" -o "$OUT/$f.o"
  "$HOST/bin/llvm-readelf" -h "$OUT/$f.o" | grep -q 'Machine:.*ARM' || { echo "Non-ARM object: $f" >&2; exit 3; }
done
objects=(); for f in "${files[@]}"; do objects+=("$OUT/$f.o"); done
"$HOST/bin/llvm-ar" rcs "$OUT/libmoon_controller.a" "${objects[@]}"
"$HOST/bin/llvm-ar" t "$OUT/libmoon_controller.a"
echo 'ARM32 static library built; NOT a runnable EV6 daemon.'

# Staging-only executable: offline lifecycle tests, no vehicle writes or audio capture.
"$HOST/bin/clang" --target=armv7a-linux-androideabi21 --sysroot="$HOST/sysroot" -std=c11 -D_GNU_SOURCE -Wall -Wextra -Werror -O2 "$ROOT/moonstudiod.c" "$OUT/moon_audio_backend.o" -lm -o "$OUT/moonstudiod"
"$HOST/bin/llvm-readelf" -h "$OUT/moonstudiod" | grep -q 'Machine:.*ARM'
echo 'Built offline ARM32 daemon harness; hardware control is disabled.'
