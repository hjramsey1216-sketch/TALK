#!/bin/bash
# Builds an installable APK for TALK.
# Run this from the project root (where this file lives) after you've:
#   1. Installed Android Studio (which installs the Android SDK + a JDK)
#   2. Added your Firebase google-services.json into app/
#   3. Run this once from a terminal that has `adb`/`sdkmanager` on PATH
#      (Android Studio's "Terminal" tab already has this set up for you)
#
# Usage:
#   ./build_apk.sh debug      # unsigned, installable via `adb install`, good for testing on your own device
#   ./build_apk.sh release    # signed, ready to share with others or upload to Play Console

set -e

MODE="${1:-debug}"

if [ "$MODE" == "debug" ]; then
    ./gradlew assembleDebug
    echo ""
    echo "Built: app/build/outputs/apk/debug/app-debug.apk"
    echo "Install directly on a plugged-in device with:"
    echo "  adb install -r app/build/outputs/apk/debug/app-debug.apk"

elif [ "$MODE" == "release" ]; then
    if [ ! -f "keystore.jks" ]; then
        echo "No keystore.jks found — generating one now."
        echo "You'll be asked for a keystore password and some identity info (name/org/etc)."
        keytool -genkeypair -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias talk
        echo ""
        echo "IMPORTANT: back up keystore.jks and remember its password."
        echo "If you lose it, you can never publish an update to this app again under the same identity."
    fi
    ./gradlew assembleRelease \
        -Pandroid.injected.signing.store.file="$(pwd)/keystore.jks" \
        -Pandroid.injected.signing.store.password="${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD env var first}" \
        -Pandroid.injected.signing.key.alias=talk \
        -Pandroid.injected.signing.key.password="${KEYSTORE_PASSWORD}"
    echo ""
    echo "Built: app/build/outputs/apk/release/app-release.apk"
    echo "This one is signed and installable on any Android device with 'install from unknown sources' allowed."
else
    echo "Usage: ./build_apk.sh [debug|release]"
    exit 1
fi
