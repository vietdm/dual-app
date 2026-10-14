#!/usr/bin/env bash
set -euo pipefail
[[ $# -eq 1 ]] || { echo 'Usage: generate-dev-keystore.sh /path/to/development.jks'; exit 2; }
[[ ! -e "$1" ]] || { echo 'ERROR: will not overwrite an existing keystore.' >&2; exit 1; }
keytool -genkeypair -keystore "$1" -alias dualprofile-dev -keyalg RSA -keysize 3072 -validity 3650
