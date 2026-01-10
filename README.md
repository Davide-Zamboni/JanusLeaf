# 🍃 JanusLeaf

> A mood-tracking journal app with AI-powered sentiment analysis

JanusLeaf helps you track your daily moods by analyzing your journal entries. Write about your day, and AI gives you a score from 1-10 reflecting how positive your day was.

---

## 🎯 Features

- 🔐 **User Accounts** - Secure registration and JWT authentication
- 📝 **Write Daily Notes** - Journal your thoughts and experiences
- 🤖 **AI Analysis** - Automatic mood scoring from 1-10
- 📊 **Track Trends** - See your mood patterns over time
- 📱 **Mobile First** - Android app with Kotlin

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  Android App    │────▶│  Spring Boot    │────▶│   PostgreSQL    │
│  (Kotlin)       │     │  Backend        │     │                 │
│                 │     │                 │     │                 │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │                 │
                       │   OpenAI API    │
                       │   (GPT-4)       │
                       │                 │
                       └─────────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Backend** | Spring Boot 3.x + Kotlin |
| **Database** | PostgreSQL 16 |
| **AI** | OpenAI GPT-4 |
| **Mobile** | Kotlin (Android) |
| **Infrastructure** | Docker |

---

## 📁 Project Structure

```
JanusLeaf/
├── backend/                    # Spring Boot application
│   ├── src/
│   │   ├── main/kotlin/com/janusleaf/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   ├── dto/
│   │   │   ├── security/
│   │   │   └── config/
│   │   ├── integrationTest/
│   │   └── test/
│   ├── docs/                   # API documentation
│   │   ├── README.md
│   │   ├── AUTH.md
│   │   └── HEALTH.md
│   ├── scripts/                # Helper scripts
│   │   ├── start-db.sh
│   │   └── stop-db.sh
│   ├── docker-compose.yml      # Full stack
│   ├── docker-compose.dev.yml  # Dev database only
│   ├── Dockerfile
│   └── build.gradle.kts
├── android/                    # Android application (coming soon)
├── docs/
│   └── TODO.md
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- JDK 21 (for local development)
- OpenAI API Key

### Option 1: Full Stack with Docker

```bash
cd JanusLeaf/backend

# Set environment variables
export OPENAI_API_KEY=your-key-here
export JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars

# Start everything
docker-compose up -d
```

### Option 2: Local Development

```bash
cd JanusLeaf/backend

# Start only the database
./scripts/start-db.sh

# Run the app
./gradlew bootRun
```

### 3. Test the API

```bash
# Health check
curl http://localhost:8080/api/health

# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "username": "John", "password": "SecurePass123!"}'

# Login and get tokens
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePass123!"}'
```

---

## 🔐 Authentication

JanusLeaf uses a **hybrid JWT authentication** system:

| Token | Expiration | Storage | Purpose |
|-------|------------|---------|---------|
| **Access Token** | 15 minutes | Client only | API authentication |
| **Refresh Token** | 7 days | Client + PostgreSQL | Get new access tokens |

### Security Features

- ✅ Short-lived access tokens (15 min exposure window)
- ✅ Server-side refresh tokens (instant revocation)
- ✅ Password change revokes all sessions
- ✅ Logout from all devices endpoint

---

## 📖 API Documentation

See [backend/docs/](backend/docs/) for full API reference:

- [Overview & Data Models](backend/docs/README.md)
- [Authentication API](backend/docs/AUTH.md)
- [Health Check API](backend/docs/HEALTH.md)

### Quick Reference

**Authentication (Public)**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create account |
| `POST` | `/api/auth/login` | Login, get tokens |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `POST` | `/api/auth/logout` | Revoke refresh token |

**Authentication (Requires JWT)**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/auth/me` | Get current user |
| `PUT` | `/api/auth/me` | Update profile |
| `POST` | `/api/auth/change-password` | Change password |
| `POST` | `/api/auth/logout-all` | Logout all devices |

**Notes (Requires JWT)**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/notes` | List user's notes |
| `POST` | `/api/notes` | Create new note |
| `GET` | `/api/notes/{id}` | Get note by ID |
| `PUT` | `/api/notes/{id}` | Update note |
| `DELETE` | `/api/notes/{id}` | Delete note |
| `GET` | `/api/stats` | Mood statistics |

---

## 🤖 How AI Analysis Works

1. You write a journal entry
2. The entry is sent to OpenAI GPT-4
3. AI analyzes sentiment, tone, and content
4. Returns:
   - **Score (1-10)**: Overall positivity rating
   - **Summary**: Brief mood description
   - **Highlights**: Key positive/negative points

### Scoring Guide

| Score | Meaning |
|-------|---------|
| 1-3 | Difficult/Challenging day |
| 4-5 | Below average |
| 6 | Neutral/Average |
| 7-8 | Good/Positive day |
| 9-10 | Excellent/Amazing day |

---

## 📱 Mobile App Screens

### Welcome / Login
- Clean login form
- "Don't have an account? Register" link
- Secure token storage (Keystore)

### Register
- Email, username, password fields
- Password requirements indicator
- Auto-login after registration

### Homepage
- List of recent notes with mood scores
- Color-coded by score (red → yellow → green)
- Pull to refresh
- User profile access

### Add Note
- Text input for journal entry
- "Analyzing..." state while AI processes
- Score revealed with animation

### View Note
- Full note content
- Mood score and summary
- Highlights list
- Edit/Delete options

### Profile
- View/edit username
- Change password
- Logout / Logout from all devices

---

## 🔮 Roadmap

- [x] Backend API design
- [x] User authentication (JWT with refresh tokens)
- [x] Spring Boot implementation
- [x] Docker setup
- [x] GitHub Actions CI
- [ ] Android app
- [ ] Mood charts/graphs
- [ ] Daily reminders
- [ ] Export functionality
- [ ] iOS app (Kotlin Multiplatform)

---

## 🧪 Running Tests

```bash
cd backend

# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests
./gradlew check
```

---

## 📄 License

MIT License - Feel free to use for personal projects!

---

## 🙏 Acknowledgments

- OpenAI for GPT-4 API
- Spring Boot team
- JetBrains for Kotlin

---

*Made with 💚 for better mental health awareness*
