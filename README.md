# 🍃 JanusLeaf

> A mood-tracking journal app with AI-powered sentiment analysis

JanusLeaf helps you track your daily moods by analyzing your journal entries. Write about your day, and AI gives you a score from 1-10 reflecting how positive your day was.

<p align="center">
  <img src="docs/Simulator Screenshot - iPhone 17 Pro - 2026-02-05 at 11.09.06.png" alt="JanusLeaf iOS App" width="300"/>
</p>

---

## 🎯 Features

- 🔐 **User Accounts** - Secure registration and JWT authentication
- 📝 **Daily Journal** - Write your thoughts with a rich markdown editor
- 🤖 **AI Mood Analysis** - Automatic mood scoring from 1-10 (powered by OpenRouter)
- 💡 **Personalized Inspiration** - AI-generated quotes based on your journal themes
- 📊 **Mood Insights** - Visualize your mood patterns over time
- 📱 **Native Mobile Apps** - iOS (SwiftUI) + Android (Compose) via Kotlin Multiplatform

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │  Oracle Cloud   │     │    Supabase     │
│  Mobile App     │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│  (KMP + SwiftUI)│     │  Backend        │     │                 │
│                 │     │                 │     │                 │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │                 │
                     │   OpenRouter    │
                     │   (AI Models)   │
                     │                 │
                     └─────────────────┘
```

| Component | Service |
|-----------|---------|
| **Backend** | Oracle Cloud Free Tier (2 instances for failover) |
| **Database** | Supabase PostgreSQL |
| **Fallback** | Render.com (auto-deploys from main branch) |
| **AI** | OpenRouter API (Claude, GPT-4, etc.) |

---

## 📁 Project Structure

```
JanusLeaf/
├── backend/                    # Spring Boot backend
│   ├── src/                    # Kotlin source code
│   ├── docs/                   # API documentation
│   └── scripts/                # Deployment scripts
├── app/                        # KMP mobile app
│   ├── shared/                 # Kotlin Multiplatform shared module
│   ├── composeApp/             # Android UI (Compose)
│   ├── iosApp/                 # iOS UI (SwiftUI)
│   └── docs/                   # Mobile app documentation
└── docs/                       # Project-level docs
```

---

## 📖 Documentation

### 📱 Mobile App

See **[app/README.md](app/README.md)** for mobile app documentation:

- [Getting Started](app/README.md#getting-started)
- [Architecture](app/README.md#architecture)
- [Configuration](app/README.md#configuration)
- [Design System](app/README.md#design-system)

### ⚙️ Backend API

See **[backend/README.md](backend/README.md)** for backend documentation:

- [Quick Start](backend/README.md#-quick-start)
- [Data Models](backend/README.md#%EF%B8%8F-data-models)
- [API Reference](backend/README.md#-api-quick-reference)

Detailed API docs in [backend/docs/](backend/docs/):

- [Authentication API](backend/docs/AUTH.md)
- [Journal API](backend/docs/JOURNAL.md)
- [Inspirational Quote API](backend/docs/INSPIRATION.md)
- [Health Check API](backend/docs/HEALTH.md)
- [Deployment Guide](backend/docs/DEPLOY.md)
- [Secrets Management](backend/docs/SECRETS.md)

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- JDK 21 (for backend development)
- Xcode 15+ (for iOS)
- Android Studio (for Android)

### Option 1: Run Backend with Docker

```bash
cd backend

# Set environment variables
export OPENROUTER_API_KEY=your-key-here
export JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars

# Start everything
docker-compose up -d
```

### Option 2: Local Development

```bash
cd backend

# Start only the database
./scripts/start-db.sh

# Run the app
./gradlew bootRun
```

### Run Mobile Apps

```bash
cd app

# iOS (Debug - connects to localhost)
./scripts/run-ios.sh

# Android
./scripts/run-android.sh
```

---

## 🔮 Roadmap

- [x] Backend API (Spring Boot + Kotlin)
- [x] User authentication (JWT with refresh tokens)
- [x] Journal CRUD with AI mood scoring
- [x] Personalized inspirational quotes
- [x] iOS app (SwiftUI)
- [x] Production deployment (Oracle Cloud + Supabase)
- [x] Multi-server failover
- [ ] Android app (full implementation)
- [ ] Mood charts/graphs (detailed analytics)
- [ ] Daily reminders (push notifications)
- [ ] Export functionality (PDF, JSON)
- [ ] Offline support

---

## 📄 License

MIT License - Feel free to use for personal projects!

---

*Made with 💚 for better mental health awareness*
