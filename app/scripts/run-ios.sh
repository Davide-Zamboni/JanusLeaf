#!/usr/bin/env bash
set -euo pipefail

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$APP_DIR"

DERIVED_DATA="${DERIVED_DATA:-/tmp/janusleaf-ios}"
BUNDLE_ID="com.janusleaf.app"

BOOTED_UDID=$(python3 - <<'PY'
import json
import subprocess

data = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "booted", "-j"]))
for runtime, devices in data.get("devices", {}).items():
    for d in devices:
        if d.get("state") == "Booted":
            print(d.get("udid"))
            raise SystemExit(0)
raise SystemExit(0)
PY
)

if [[ -z "$BOOTED_UDID" ]]; then
  TARGET_UDID=$(python3 - <<'PY'
import json
import subprocess

data = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "-j"]))
for runtime, devices in data.get("devices", {}).items():
    for d in devices:
        if not d.get("isAvailable", False):
            continue
        name = d.get("name", "")
        if "iPhone" in name:
            print(d.get("udid"))
            raise SystemExit(0)
raise SystemExit(1)
PY
)
  if [[ -z "$TARGET_UDID" ]]; then
    echo "No available iPhone simulators found. Create one in Xcode." >&2
    exit 1
  fi
  echo "Booting simulator: $TARGET_UDID"
  xcrun simctl boot "$TARGET_UDID" || true
  open -a Simulator
  DEVICE_UDID="$TARGET_UDID"
else
  DEVICE_UDID="$BOOTED_UDID"
fi

xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination "id=$DEVICE_UDID" -derivedDataPath "$DERIVED_DATA" build

APP_PATH="$DERIVED_DATA/Build/Products/Debug-iphonesimulator/JanusLeaf.app"
if [[ ! -d "$APP_PATH" ]]; then
  echo "App not found at $APP_PATH" >&2
  exit 1
fi

xcrun simctl install "$DEVICE_UDID" "$APP_PATH"
xcrun simctl launch "$DEVICE_UDID" "$BUNDLE_ID"
