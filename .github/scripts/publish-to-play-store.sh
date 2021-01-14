#!/bin/bash

# Make Bash strict
set -o nounset
set -o errexit
set -o pipefail

VERSION_NAME="$1"

set +x  # Disable debug (if enabled) so we won't leak secrets accidentally
echo "${ANDROID_KEYSTORE}" | base64 -d > /tmp/keystore.jks
exec ./gradlew app:publish \
  -PenableGpp=true \
  -PversionName="${VERSION_NAME}" \
  -PsigningKeystorePath=/tmp/keystore.jks
