# JanusLeaf Backend API

> Spring Boot 3.x + Kotlin + PostgreSQL

**Base URL:** `http://localhost:8080/api`

---

## Overview

JanusLeaf backend provides REST APIs for the mood-tracking journal application. It handles user authentication, journal entries with AI-powered mood analysis, and personalized inspirational quotes.

---

## 📚 Documentation

See the [docs/](docs/) folder for detailed API documentation:

| Document | Description |
|----------|-------------|
| [Authentication](docs/AUTH.md) | User registration, login, JWT tokens, logout |
| [Journal](docs/JOURNAL.md) | Create, read, update, delete journal entries |
| [Inspirational Quotes](docs/INSPIRATION.md) | AI-generated personalized quotes |
| [Health Check](docs/HEALTH.md) | API health status endpoint |
| [Deployment](docs/DEPLOY.md) | Deploy to Oracle Cloud with Supabase |
| [Secrets Management](docs/SECRETS.md) | git-crypt encrypted secrets |

---

## 🚀 Quick Start

### Prerequisites

- JDK 21
- Docker (for database)
- OpenRouter API key (for AI features)

### Option 1: Docker (Full Stack)

```bash
export OPENROUTER_API_KEY=your-key-here
export JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars

docker-compose up -d
```

### Option 2: Local Development

```bash
# Start database
./scripts/start-db.sh

# Run the app
./gradlew bootRun
```

### Test the API

```bash
# Health check
curl http://localhost:8080/api/health

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "username": "John", "password": "SecurePass123!"}'
```

---

## 🗄️ Data Models

### User

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `email` | `String` | User email (unique, used for login) |
| `username` | `String` | Display name |
| `passwordHash` | `String` | Bcrypt hashed password (never returned) |
| `createdAt` | `DateTime` | Registration timestamp |
| `updatedAt` | `DateTime` | Last profile update |

### JournalEntry

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `userId` | `UUID` | Owner user ID (from JWT, not exposed in API) |
| `title` | `String` | Entry title (defaults to date if not provided) |
| `body` | `String` | The journal content (max 50,000 chars) |
| `moodScore` | `Integer` | AI-generated score 1-10 (null if pending) |
| `entryDate` | `LocalDate` | Date the entry is for |
| `version` | `Long` | Optimistic locking version for concurrent edits |
| `createdAt` | `DateTime` | When the entry was created |
| `updatedAt` | `DateTime` | Last modification timestamp |

### InspirationalQuote

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `quote` | `String` | AI-generated inspirational quote |
| `tags` | `String[]` | 4 thematic tags from journal themes |
| `generatedAt` | `DateTime` | When quote was generated |

---

## 📡 API Quick Reference

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Create account |
| `POST` | `/api/auth/login` | Login, get tokens |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `POST` | `/api/auth/logout` | Revoke refresh token |

### Authentication (Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/auth/me` | Get current user |
| `PUT` | `/api/auth/me` | Update profile |
| `POST` | `/api/auth/change-password` | Change password |
| `POST` | `/api/auth/logout-all` | Logout all devices |

### Journal (Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/journal` | List user's entries |
| `POST` | `/api/journal` | Create new entry |
| `GET` | `/api/journal/{id}` | Get entry by ID |
| `GET` | `/api/journal/range` | Get entries by date range |
| `PATCH` | `/api/journal/{id}` | Update entry metadata |
| `PATCH` | `/api/journal/{id}/body` | Update entry body |
| `DELETE` | `/api/journal/{id}` | Delete entry |

### Inspiration (Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/inspiration` | Get personalized AI quote |

### System

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health` | Health check (no auth) |

---

## ❌ Error Responses

All errors follow this format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "details": {},
  "timestamp": "2026-01-10T14:30:00Z",
  "path": "/api/journal/invalid-id"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `INVALID_TOKEN` | 401 | Invalid, expired, or revoked token |
| `USER_ALREADY_EXISTS` | 409 | Email already registered |
| `CONCURRENT_MODIFICATION` | 409 | Resource was modified by another request |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `NOTE_NOT_FOUND` | 404 | Journal entry not found |
| `ACCESS_DENIED` | 403 | No access to the requested resource |
| `VALIDATION_ERROR` | 400 | Invalid request data |
| `AI_SERVICE_ERROR` | 503 | AI analysis temporarily unavailable |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## ⚙️ Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | - | PostgreSQL connection string |
| `DATABASE_USERNAME` | Yes | - | Database username |
| `DATABASE_PASSWORD` | Yes | - | Database password |
| `JWT_SECRET` | Yes | - | Secret key for JWT signing (min 32 chars) |
| `JWT_ACCESS_EXPIRATION` | No | 900000 | Access token expiration in ms (15 min) |
| `JWT_REFRESH_EXPIRATION` | No | 604800000 | Refresh token expiration in ms (7 days) |
| `OPENROUTER_API_KEY` | Yes | - | OpenRouter API key for AI features |
| `SERVER_PORT` | No | 8080 | Server port |

See [docs/SECRETS.md](docs/SECRETS.md) for secrets management with git-crypt.

---

## 🧪 Running Tests

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# All tests
./gradlew check
```

---

## 🌐 Deployment

See [docs/DEPLOY.md](docs/DEPLOY.md) for full deployment guide.

```bash
make deploy         # Build and deploy to Oracle Cloud
make logs           # View logs
make health         # Check API health
```
