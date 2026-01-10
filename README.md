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
├── backend/                 # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/janusleaf/
│   │   │   │       ├── controller/
│   │   │   │       ├── service/
│   │   │   │       ├── repository/
│   │   │   │       ├── model/
│   │   │   │       ├── dto/
│   │   │   │       └── config/
│   │   │   └── resources/
│   │   └── test/
│   ├── Dockerfile
│   └── build.gradle.kts
├── android/                 # Android application (coming soon)
├── docker-compose.yml
├── docs/
│   ├── API_DOCUMENTATION.md
│   └── TODO.md
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- OpenAI API Key

### 1. Clone & Configure

```bash
cd JanusLeaf
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY
```

### 2. Start Services

```bash
docker-compose up -d
```

### 3. Test the API

```bash
# Health check
curl http://localhost:8080/api/health

# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "username": "John", "password": "SecurePass123!"}'

# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePass123!"}' \
  | jq -r '.accessToken')

# Create a note (authenticated)
curl -X POST http://localhost:8080/api/notes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"content": "Had a wonderful day today!"}'

# Get all notes (authenticated)
curl http://localhost:8080/api/notes \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📖 API Documentation

See [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md) for full API reference.

### Quick Reference

**Authentication (Public)**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create account |
| `POST` | `/api/auth/login` | Login, get JWT |
| `POST` | `/api/auth/refresh` | Refresh token |

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
- Secure token storage

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
- Logout

---

## 🔮 Roadmap

- [x] Backend API design
- [x] User authentication design (JWT)
- [ ] Spring Boot implementation
- [ ] Docker setup
- [ ] Android app
- [ ] Mood charts/graphs
- [ ] Daily reminders
- [ ] Export functionality
- [ ] iOS app (Kotlin Multiplatform)

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
