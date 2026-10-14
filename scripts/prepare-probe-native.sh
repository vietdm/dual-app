#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
NDK="${ANDROID_NDK_HOME:-$SDK/ndk/27.0.12077973}"
HOST=linux-x86_64; [[ "$(uname -s)" == Darwin ]] && HOST=darwin-x86_64
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST/bin"
[[ -x "$TOOLCHAIN/aarch64-linux-android28-clang" ]] || { echo 'ERROR: set ANDROID_NDK_HOME to an installed Android NDK.' >&2; exit 1; }
mkdir -p "$ROOT/probe/fixtures/jniLibs/arm64-v8a" "$ROOT/probe/fixtures/jniLibs/x86_64"
"$TOOLCHAIN/aarch64-linux-android28-clang" -shared -fPIC -Wl,-z,max-page-size=16384 "$ROOT/probe/fixture.c" -o "$ROOT/probe/fixtures/jniLibs/arm64-v8a/libprobe.so"
"$TOOLCHAIN/x86_64-linux-android28-clang" -shared -fPIC -Wl,-z,max-page-size=16384 "$ROOT/probe/fixture.c" -o "$ROOT/probe/fixtures/jniLibs/x86_64/libprobe.so"
echo 'Native ABI fixtures prepared. Build: ./gradlew -PprobeNative=true :probe:bundleDebug'
