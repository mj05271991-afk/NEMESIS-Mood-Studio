#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
HOST="$(find "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
OUT="${MOON_BUILD_OUT:-$ROOT/arm32-ndk-build}"
files=(native_controller native_hmg_adapter native_lighting_session native_ownership_lock native_transport_gate native_verified_controller)
for f in "${files[@]}"; do
  test -s "$OUT/$f.o"
  "$HOST/bin/llvm-readelf" -h "$OUT/$f.o" | grep -q 'Machine:.*ARM'
done
"$HOST/bin/llvm-ar" t "$OUT/libmoon_controller.a" > "$OUT/archive-members.txt"
for f in "${files[@]}"; do grep -qx "$f.o" "$OUT/archive-members.txt"; done
"$HOST/bin/llvm-nm" -u "$OUT/libmoon_controller.a" > "$OUT/unresolved-symbols.txt"
echo 'PASS: six ARM32 objects and archive membership verified. Real HMG transport remains unlinked.'
