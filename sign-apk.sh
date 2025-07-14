#!/usr/bin/env bash
#
# sign-apk.sh  –  Sign an *unsigned* APK with apksigner.
#
# Usage:  ./sign-apk.sh path/to/your-unsigned.apk
#
# Environment variables you may override:
#   KEYSTORE   – JKS/PKCS12 path   (default: $HOME/private/programming/active/android.keystore)
#   SDK_ROOT   – Android SDK root         (default: $ANDROID_SDK_ROOT or $HOME/Android/Sdk)
#

set -euo pipefail

# 1 ── validate input ---------------------------------------------------------
if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <unsigned-apk>" >&2
  exit 1
fi
UNSIGNED_APK=$1
[[ -f "$UNSIGNED_APK" ]] || { echo "ERROR: file not found – $UNSIGNED_APK" >&2; exit 1; }

# 2 ── defaults ---------------------------------------------------------------
KEYSTORE=${KEYSTORE:-"$HOME/private/programming/active/android.keystore"}
KS_ALIAS=${KS_ALIAS:-upload}
SDK_ROOT=${SDK_ROOT:-${ANDROID_SDK_ROOT:-"$HOME/Android/Sdk"}}

# 3 ── locate apksigner -------------------------------------------------------
if command -v apksigner >/dev/null 2>&1; then
  APKSIGNER=$(command -v apksigner)
else
  LATEST_BT=$(ls -1 "$SDK_ROOT/build-tools" 2>/dev/null | sort -V | tail -n1)
  APKSIGNER="$SDK_ROOT/build-tools/$LATEST_BT/apksigner"
fi
[[ -x "$APKSIGNER" ]] || { echo "ERROR: apksigner not found." >&2; exit 1; }

# 4 ── derive output file name ------------------------------------------------
DIR=$(dirname "$UNSIGNED_APK")
BASE=$(basename "$UNSIGNED_APK" .apk)
SIGNED_APK="$DIR/${BASE/-unsigned/-signed}.apk"   # e.g. app-release.apk

# 5 ── sign (apksigner will prompt for passwords) -----------------------------
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --out "$SIGNED_APK" \
  "$UNSIGNED_APK"

# 6 ── verify -----------------------------------------------------------------
"$APKSIGNER" verify --print-certs "$SIGNED_APK" && \
echo "✓ Signed APK written to: $SIGNED_APK"

