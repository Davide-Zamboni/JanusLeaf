# 🍃 JanusLeaf App

> Kotlin Multiplatform mobile application for JanusLeaf mood-tracking journal

A beautiful, modern journaling app built with Compose Multiplatform, targeting both Android and iOS from a single codebase.

---

## 🏗️ Architecture

The app follows **Clean Architecture** principles with **MVI (Model-View-Intent)** pattern for the presentation layer.

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐       │
│  │   Screens   │   │  ViewModels │   │   States    │       │
│  │  (Compose)  │◄──│    (MVI)    │◄──│  & Events   │       │
│  └─────────────┘   └─────────────┘   └─────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                            │
│  ┌─────────────┐   ┌─────────────┐                          │
│  │   Models    │   │ Repository  │                          │
│  │             │   │ Interfaces  │                          │
│  └─────────────┘   └─────────────┘                          │
├─────────────────────────────────────────────────────────────┤
│                       DATA LAYER                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐       │
│  │  Remote API │   │ Repository  │   │   Secure    │       │
│  │   (Ktor)    │   │    Impl     │   │   Storage   │       │
│  └─────────────┘   └─────────────┘   └─────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Compose Multiplatform** | Shared UI across platforms |
| **Ktor Client** | HTTP networking |
| **Kotlinx Serialization** | JSON parsing |
| **Koin** | Dependency injection |
| **Kotlinx Coroutines** | Async operations |
| **Kotlinx DateTime** | Date/time handling |
| **Napier** | Multiplatform logging |

---

## 📁 Project Structure

```
app/
├── composeApp/
│   └── src/
│       ├── commonMain/          # Shared code
│       │   └── kotlin/com/janusleaf/app/
│       │       ├── domain/      # Domain models & interfaces
│       │       │   ├── model/
│       │       │   └── repository/
│       │       ├── data/        # Data layer implementation
│       │       │   ├── local/
│       │       │   ├── remote/
│       │       │   └── repository/
│       │       ├── presentation/# UI layer
│       │       │   ├── auth/
│       │       │   ├── home/
│       │       │   ├── components/
│       │       │   └── theme/
│       │       └── di/          # Dependency injection
│       ├── androidMain/         # Android-specific code
│       └── iosMain/             # iOS-specific code
├── iosApp/                      # iOS Xcode project
└── gradle/
```

---

## 🚀 Getting Started

### Prerequisites

- **JDK 17+**
- **Android Studio Hedgehog or later** (with KMP plugin)
- **Xcode 15+** (for iOS)

### Running the Backend

The app requires the JanusLeaf backend to be running:

```bash
cd ../backend
./scripts/start-db.sh
./gradlew bootRun
```

### Running on Android

```bash
# From the app directory
./gradlew :composeApp:installDebug

# Or open in Android Studio and run
```

### Running on iOS

#### 1. Build the iOS framework

**Important:** You must build for the correct architecture based on your target:

| Target | Build Command |
|--------|---------------|
| **iPhone Simulator** | `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` |
| **Physical iPhone** | `./gradlew :composeApp:linkDebugFrameworkIosArm64` |
| **Both (recommended)** | `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64` |

For **development** (local backend):
```bash
# Simulator only
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Physical device only
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Both simulator and device
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64
```

For **production** (remote servers with failover):
```bash
# Simulator only
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -PuseProduction=true

# Physical device only
./gradlew :composeApp:linkDebugFrameworkIosArm64 -PuseProduction=true

# Both simulator and device (recommended)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64 -PuseProduction=true
```

#### 2. Open in Xcode

Open `iosApp/iosApp.xcodeproj` in Xcode

#### 3. Build and Run

- Select your target (Simulator or Physical Device)
- **Clean Build Folder** (Product → Clean Build Folder) if you changed the Gradle build
- Build and Run

> ⚠️ **Common Issue**: If the app works on simulator but not on physical device, you likely built only for simulator. Run the `linkDebugFrameworkIosArm64` task for physical devices.

---

## 🎨 Design System

The app features a custom design system inspired by **2026 design trends**:

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

## 🔐 Security

### Token Storage

- **Android**: EncryptedSharedPreferences with Android Keystore
- **iOS**: Keychain with hardware encryption

### Authentication Flow

```
1. Login/Register → Store tokens securely
2. API Calls → Auto-inject access token
3. Token Expired → Auto-refresh with refresh token
4. Refresh Failed → Redirect to login
```

---

## 📱 Screens

### Authentication (Implemented ✅)
- Login with email/password
- Register with email/username/password
- Form validation with animated errors
- Secure token management

### Home (Placeholder)
- Welcome screen after login
- Logout functionality

### Coming Soon
- Journal entry list
- Create/edit notes
- AI mood analysis
- Statistics & trends
- Profile management

---

## 🔧 Configuration

### Environment Setup (Development vs Production)

The app supports **build-time environment configuration** via Gradle flags:

#### Production Build

```bash
# Android
./gradlew :composeApp:assembleDebug -PuseProduction=true
./gradlew :composeApp:assembleRelease -PuseProduction=true

# iOS - Build shared framework for both simulator and device
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :composeApp:linkDebugFrameworkIosArm64 -PuseProduction=true
# Then build in Xcode (works for both simulator and physical device)
```

#### Development Build (local backend)

```bash
# Android (default - no flag needed)
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

### API Base URL Configuration

Update your production URL in `composeApp/src/commonMain/kotlin/com/janusleaf/app/data/remote/ApiConfig.kt`:

```kotlin
object ApiConfig {
    // Production URL
    const val PRODUCTION_BASE_URL = "http://158.180.228.188:8080"
    
    // Development URLs (automatic per platform):
    // - Android Emulator: 10.0.2.2:8080
    // - iOS Simulator: localhost:8080
}
```

### How It Works

| Flag | Environment | Backend URL |
|------|-------------|-------------|
| `-PuseProduction=true` | Production | Your Render URL (HTTPS) |
| *(default)* | Development | localhost / 10.0.2.2 |

The build will output which environment was configured:
```
🔧 BuildConfig generated: USE_PRODUCTION = true
```

### ⚠️ Notes for Production

- **HTTPS Required**: Render uses HTTPS by default ✓
- **Cold Starts**: Free Render instances spin down after inactivity (~30s wake-up time)
- **No Trailing Slash**: Ensure your URL doesn't end with `/`

---

## 📄 License

MIT License - Part of the JanusLeaf project
