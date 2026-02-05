#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$APP_DIR"

SDK_DIR=""
if [[ -f local.properties ]]; then
  SDK_DIR=$(grep '^sdk.dir=' local.properties | head -n1 | cut -d= -f2- || true)
fi
if [[ -z "$SDK_DIR" && -n "${ANDROID_HOME:-}" ]]; then
  SDK_DIR="$ANDROID_HOME"
fi

ADB="adb"
EMULATOR="emulator"
if [[ -n "$SDK_DIR" ]]; then
  if [[ -x "$SDK_DIR/platform-tools/adb" ]]; then
    ADB="$SDK_DIR/platform-tools/adb"
  fi
  if [[ -x "$SDK_DIR/emulator/emulator" ]]; then
    EMULATOR="$SDK_DIR/emulator/emulator"
  fi
fi

if [[ "$ADB" == "adb" ]]; then
  command -v adb >/dev/null 2>&1 || { echo "adb not found in PATH and sdk.dir/ANDROID_HOME not set"; exit 1; }
else
  [[ -x "$ADB" ]] || { echo "adb not found at $ADB"; exit 1; }
fi

if [[ "$EMULATOR" == "emulator" ]]; then
  command -v emulator >/dev/null 2>&1 || { echo "emulator not found in PATH and sdk.dir/ANDROID_HOME not set"; exit 1; }
else
  [[ -x "$EMULATOR" ]] || { echo "emulator not found at $EMULATOR"; exit 1; }
fi

DEVICE_COUNT=$($ADB devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  AVD_NAME="${AVD_NAME:-}"
  if [[ -z "$AVD_NAME" ]]; then
    AVD_NAME=$($EMULATOR -list-avds | head -n1 || true)
  fi
  if [[ -z "$AVD_NAME" ]]; then
    echo "No Android emulators found. Create one in Android Studio first."
    exit 1
  fi
  echo "Starting emulator: $AVD_NAME"
  nohup "$EMULATOR" -avd "$AVD_NAME" >/tmp/janusleaf-android-emulator.log 2>&1 &
  echo "Waiting for emulator to boot..."
  $ADB wait-for-device
  $ADB shell input keyevent 82 || true
fi

./gradlew :composeApp:installDebug
$ADB shell am start -n com.janusleaf.app/.MainActivity
