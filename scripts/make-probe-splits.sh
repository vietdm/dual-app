#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
[[ $# -eq 1 && -f "$1" ]] || { echo 'Usage: make-probe-splits.sh /path/to/official-bundletool.jar'; exit 2; }
BUNDLETOOL="$(cd -- "$(dirname -- "$1")" && pwd)/$(basename -- "$1")"
cd "$ROOT"
./scripts/prepare-probe-native.sh
./gradlew -PprobeNative=true :probe:bundleDebug
mkdir -p dist/split-fixtures
# Bundletool default debug signing uses the existing development debug key, never a production key.
java -jar "$BUNDLETOOL" build-apks --bundle=probe/build/outputs/bundle/debug/probe-debug.aab --output=dist/split-fixtures/probe.apks --overwrite
python3 - <<'PY'
from zipfile import ZipFile
from pathlib import Path
root=Path('dist/split-fixtures')
with ZipFile(root/'probe.apks') as z:
 for name in z.namelist():
  if name.startswith('splits/') and name.endswith('.apk'):
   (root/Path(name).name).write_bytes(z.read(name))
PY
cat > dist/split-fixtures/samsung-api36.json <<'SPEC'
{"supportedAbis":["arm64-v8a"],"supportedLocales":["vi","en"],"screenDensity":450,"sdkVersion":36}
SPEC
cat > dist/split-fixtures/x86-api36.json <<'SPEC'
{"supportedAbis":["x86_64"],"supportedLocales":["vi","en"],"screenDensity":450,"sdkVersion":36}
SPEC
python3 - <<'PYREMOVE'
import shutil
for generated in ('dist/split-fixtures/samsung','dist/split-fixtures/x86'):
 shutil.rmtree(generated,ignore_errors=True)
PYREMOVE
java -jar "$BUNDLETOOL" extract-apks --apks=dist/split-fixtures/probe.apks --device-spec=dist/split-fixtures/samsung-api36.json --output-dir=dist/split-fixtures/samsung
java -jar "$BUNDLETOOL" extract-apks --apks=dist/split-fixtures/probe.apks --device-spec=dist/split-fixtures/x86-api36.json --output-dir=dist/split-fixtures/x86
echo 'Use only the APKs in the selected device directory (samsung or x86). Do not combine alternate base masters or ABI variants. See docs/MANUAL_TEST.md.'
