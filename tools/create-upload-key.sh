#!/usr/bin/env bash
# Creates the Play "upload key" and a git-ignored keystore.properties next to it.
# Keep the .jks file and passwords safe and backed up: you need them for every update.
set -euo pipefail
cd "$(dirname "$0")/.."

KEYSTORE=upload-keystore.jks
if [[ -e $KEYSTORE ]]; then
  echo "$KEYSTORE already exists; not overwriting." >&2
  exit 1
fi

read -rsp "Choose a keystore password (6+ characters): " PASS; echo
keytool -genkeypair -v -keystore "$KEYSTORE" -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS"

cat > keystore.properties <<PROPS
storeFile=$KEYSTORE
storePassword=$PASS
keyAlias=upload
keyPassword=$PASS
PROPS

echo
echo "Created $KEYSTORE and keystore.properties (both git-ignored)."
echo "For GitHub Actions, add these repository secrets:"
echo "  ANDROID_KEYSTORE_BASE64  = output of: base64 -w0 $KEYSTORE"
echo "  ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD = the password you chose"
echo "  ANDROID_KEY_ALIAS        = upload"
