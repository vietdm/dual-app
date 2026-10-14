#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
MODE="${1:-debug}"
case "$MODE" in debug|release) ;; *) echo 'ERROR: use ./build.sh [debug|release]' >&2; exit 2;; esac
command -v java >/dev/null || { echo 'ERROR: Java 17+ is required.' >&2; exit 1; }
JAVA_MAJOR="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n1)"
[[ "$JAVA_MAJOR" =~ ^[0-9]+$ && "$JAVA_MAJOR" -ge 17 ]] || { echo 'ERROR: Java 17+ is required.' >&2; exit 1; }
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK" && -d "$SDK" ]] || { echo 'ERROR: set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK installation.' >&2; exit 1; }
[[ -f "$SDK/platforms/android-36/android.jar" ]] || { echo 'ERROR: missing Android SDK 36. Install platforms;android-36 using sdkmanager. targetSdk will not be reduced.' >&2; exit 1; }
[[ -x "$SDK/build-tools/36.0.0/aapt2" ]] || { echo 'ERROR: missing build-tools;36.0.0. Install using sdkmanager.' >&2; exit 1; }
[[ -f gradle/wrapper/gradle-wrapper.jar && -f gradle/wrapper/gradle-wrapper.properties && -x gradlew ]] || { echo 'ERROR: Gradle Wrapper is missing or not executable.' >&2; exit 1; }
if [[ "$MODE" == release ]]; then
 for v in DUALPROFILE_KEYSTORE DUALPROFILE_KEYSTORE_PASSWORD DUALPROFILE_KEY_ALIAS DUALPROFILE_KEY_PASSWORD; do
  [[ -n "${!v:-}" ]] || { echo 'ERROR: release signing configuration is missing.' >&2; echo 'Set DUALPROFILE_KEYSTORE, DUALPROFILE_KEYSTORE_PASSWORD, DUALPROFILE_KEY_ALIAS and DUALPROFILE_KEY_PASSWORD.' >&2; exit 1; }
 done
 [[ -f "$DUALPROFILE_KEYSTORE" ]] || { echo 'ERROR: release keystore does not exist.' >&2; exit 1; }
fi
# Escape properties syntax for SDK paths containing spaces, backslashes or colon.
SDK_ESCAPED="${SDK//\\/\\\\}"; SDK_ESCAPED="${SDK_ESCAPED//:/\\:}"; SDK_ESCAPED="${SDK_ESCAPED// /\\ }"
printf 'sdk.dir=%s\n' "$SDK_ESCAPED" > local.properties
VARIANT=Debug; [[ "$MODE" == release ]] && VARIANT=Release
./gradlew --console=plain ":app:test${VARIANT}UnitTest" ":app:lint${VARIANT}" ":app:assemble${VARIANT}" :probe:assembleDebug :probe:bundleDebug
APK="app/build/outputs/apk/$MODE/app-$MODE.apk"
[[ -s "$APK" ]] || { echo 'ERROR: APK missing or empty.' >&2; exit 1; }
BADGING="$("$SDK/build-tools/36.0.0/aapt2" dump badging "$APK")"
[[ "$BADGING" == *"package: name='com.dualprofile.app'"* && "$BADGING" == *"targetSdkVersion:'36'"* ]] || { echo 'ERROR: APK manifest verification failed.' >&2; exit 1; }
"$SDK/build-tools/36.0.0/apksigner" verify "$APK"
mkdir -p dist
cp "$APK" dist/DualProfile.apk
cp probe/build/outputs/apk/debug/probe-debug.apk dist/DualProfileProbe.apk
cp probe/build/outputs/bundle/debug/probe-debug.aab dist/DualProfileProbe.aab
if command -v sha256sum >/dev/null; then sha256sum dist/*.apk > dist/SHA256SUMS; else shasum -a 256 dist/*.apk > dist/SHA256SUMS; fi
printf '\n========================================\nDual Profile build successful\nAPK: %s/dist/DualProfile.apk\n========================================\n' "$ROOT"
