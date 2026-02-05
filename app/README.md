# JanusLeaf Mobile App

> Kotlin Multiplatform mobile application for JanusLeaf mood-tracking journal

A modern journaling app with a shared KMP module for data/domain/backend integration, a Compose-based Android UI, and a SwiftUI iOS UI.

---

## Architecture

The app follows **Clean Architecture** principles. Shared domain/data lives in `:shared`, Android UI lives in `:composeApp`, and the iOS UI lives in `iosApp` (SwiftUI) while consuming the shared framework.

```
┌───────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                     │
│  ┌─────────────────────┐   ┌─────────────────────┐        │
│  │ Android UI          │   │ iOS UI              │        │
│  │ (Compose)           │   │ (SwiftUI)           │        │
│  └─────────────────────┘   └─────────────────────┘        │
├───────────────────────────────────────────────────────────┤
│                       DOMAIN LAYER                        │
│  ┌─────────────────┐   ┌─────────────────┐                │
│  │ Models          │   │ Repository      │                │
│  │                 │   │ Interfaces      │                │
│  └─────────────────┘   └─────────────────┘                │
├───────────────────────────────────────────────────────────┤
│                        DATA LAYER                         │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │ Remote API  │   │ Repository  │   │ Secure      │      │
│  │ (Ktor)      │   │ Impl        │   │ Storage     │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
└───────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Technology | Purpose |
|------------|---------|
| **Compose Multiplatform** | Android UI |
| **SwiftUI** | iOS UI |
| **Ktor Client** | HTTP networking |
| **Kotlinx Serialization** | JSON parsing |
| **Koin** | Dependency injection |
| **Kotlinx Coroutines** | Async operations |
| **Kotlinx DateTime** | Date/time handling |
| **Napier** | Multiplatform logging |

---

## Project Structure

```
app/
├── composeApp/                  # Android UI (Compose)
│   └── src/main/
├── shared/                      # KMP shared module (domain + data + backend)
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       └── iosMain/
├── iosApp/                      # iOS SwiftUI Xcode project
├── scripts/
│   ├── run-android.sh
│   └── run-ios.sh
└── docs/                        # Additional documentation
```

---

## Getting Started

### Prerequisites

- **JDK 17+**
- **Android Studio Hedgehog or later** (with KMP plugin)
- **Xcode 15+** (for iOS)

### Running the Backend

The app requires the JanusLeaf backend to be running. See [Backend Documentation](../backend/README.md) for setup.

```bash
cd ../backend
./scripts/start-db.sh
./gradlew bootRun
```

### Running on Android

```bash
# From the app directory
./gradlew :composeApp:installDebug

# Or use the helper script
./scripts/run-android.sh

# Or open in Android Studio and run
```

### Running on iOS

#### Option 1: Use the helper script (recommended)

```bash
./scripts/run-ios.sh          # Debug (localhost)
./scripts/run-ios.sh --prod   # Production servers
```

#### Option 2: Build manually

```bash
# Build the iOS shared framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Open in Xcode
open iosApp/iosApp.xcodeproj

# Build and Run from Xcode
```

#### Option 3: CLI (one-shot)

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.2' -derivedDataPath /tmp/janusleaf-ios build
xcrun simctl install booted /tmp/janusleaf-ios/Build/Products/Debug-iphonesimulator/JanusLeaf.app
xcrun simctl launch booted com.janusleaf.app
```

---

## Agent Launch Notes

These steps are written for AI agents (Codex, Claude CLI, Copilot, Cursor) to reliably launch the apps.

### Android

1. Ensure SDK is configured in `local.properties`:
   ```
   sdk.dir=/Users/USERNAME/Library/Android/sdk
   ```

2. Start an emulator:
   ```bash
   emulator -list-avds
   emulator -avd Pixel_9_Pro
   ```

3. Build + install:
   ```bash
   ./gradlew :composeApp:installDebug
   ```

4. Launch:
   ```bash
   adb shell am start -n com.janusleaf.app/.MainActivity
   ```

### iOS

1. Build shared framework:
   ```bash
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```

2. Launch in Xcode (recommended):
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. Or CLI launch (simulator):
   ```bash
   xcrun simctl list devices
   xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 15' build
   ```

---

## Configuration

### Environment (Development vs Production)

The app supports **build-time environment configuration** via Gradle flags:

| Flag | Environment | Backend URL |
|------|-------------|-------------|
| `-PuseProduction=true` | Production | First available server in `PRODUCTION_SERVERS` |
| *(default)* | Development | localhost / 10.0.2.2 |

#### Production Build

```bash
# Android
./gradlew :composeApp:assembleDebug -PuseProduction=true
./gradlew :composeApp:assembleRelease -PuseProduction=true

# iOS - Build shared framework
./gradlew :shared:embedAndSignAppleFrameworkForXcode -PuseProduction=true
```

#### Development Build (local backend)

```bash
# Android (default - no flag needed)
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

### API Configuration

Update production servers in:
- `shared/src/commonMain/kotlin/com/janusleaf/app/data/remote/ServerAvailabilityManager.kt`

Local dev URLs in:
- `shared/src/androidMain/kotlin/com/janusleaf/app/data/remote/ApiConfig.android.kt`
- `shared/src/iosMain/kotlin/com/janusleaf/app/data/remote/ApiConfig.ios.kt`

---

## Design System

The app features a custom design system inspired by **2026 design trends**.

### Color Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Leaf Green | `#1A2F23` | Primary brand color |
| Sage Green | `#6B8F71` | Accent & highlights |
| Sunrise Gold | `#FFB74D` | Positive mood indicator |
| Dusk Purple | `#9575CD` | Secondary accent |

### Typography

- **Display**: Light weight for hero text
- **Headlines**: SemiBold for page titles
- **Body**: Regular weight for content

### Components

- **JanusTextField**: Custom text field with animated focus states
- **JanusPrimaryButton**: Gradient button with press animation
- **AnimatedBackground**: Flowing organic shapes

---

## Security

### Token Storage

| Platform | Storage |
|----------|---------|
| **Android** | EncryptedSharedPreferences with Android Keystore |
| **iOS** | Keychain with hardware encryption |

### Authentication Flow

```
1. Login/Register → Store tokens securely
2. API Calls → Auto-inject access token
3. Token Expired → Auto-refresh with refresh token
4. Refresh Failed → Redirect to login
```

For full authentication API details, see [Backend Auth Documentation](../backend/docs/AUTH.md).

---

## Screens

### Implemented

| Screen | Platform | Status |
|--------|----------|--------|
| **Auth (Login/Register)** | iOS, Android | ✅ |
| **Home** | iOS | ✅ |
| **Journal List** | iOS | ✅ |
| **Journal Editor** | iOS | ✅ |
| **Mood Insights** | iOS | ✅ |
| **Profile** | iOS | ✅ |

### Coming Soon (Android)

- Home
- Journal List/Editor
- Mood Insights
- Profile

---

## Notes for Production

- **HTTPS Required**: Render uses HTTPS by default
- **Cold Starts**: Free Render instances spin down after inactivity (~30s wake-up time)
- **No Trailing Slash**: Ensure your URL doesn't end with `/`
