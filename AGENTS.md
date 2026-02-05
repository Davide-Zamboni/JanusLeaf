# Codex Project Instructions

## Android Validation
- For Android app verification, run `./app/scripts/run-android.sh` from repo root (or `./scripts/run-android.sh` from `app/`).
- Do not treat `./gradlew :composeApp:assembleDebug` as sufficient app-level validation by itself.
- Use `assembleDebug` only as a compile check; use `run-android.sh` as the functional smoke test.

## iOS Validation
- For iOS app verification, run `./app/scripts/run-ios.sh` from repo root (or `./scripts/run-ios.sh` from `app/`).
- Do not treat shared-module-only build tasks as sufficient app-level validation by themselves.
- Use Gradle/Xcode build tasks as compile checks; use `run-ios.sh` as the functional smoke test.

## Which Validation To Run
- Run only Android (`run-android.sh`) when changes are Android-only UI/DI/navigation/viewmodel code in `app/composeApp/src/androidMain`.
- Run only iOS (`run-ios.sh`) when changes are iOS SwiftUI/app wiring only in `app/iosApp`.
- Run both scripts when changes are in shared code (`app/shared`), networking/domain/data/repository logic, auth/session logic, or anything consumed by both apps.
- Run both scripts before final sign-off on cross-platform refactors or package moves.
- If you only need a fast syntax/compile check, use build tasks (`assembleDebug` / Xcode build), then follow with the relevant run script(s) before claiming app-level validation.
