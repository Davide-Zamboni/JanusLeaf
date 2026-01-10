# JanusLeaf API Documentation

> **Version:** 1.0.0  
> **Base URL:** `http://localhost:8080/api`  
> **Technology Stack:** Spring Boot 3.x + Kotlin + PostgreSQL + Docker

---

## 📋 Overview

JanusLeaf is a mood-tracking journal application that uses AI to analyze daily notes and provide a positivity score from 1-10.

### Core Features
- 🔐 User registration and authentication (JWT)
- 📝 Create and manage daily notes
- 🤖 Automatic AI-powered mood analysis
- 📊 View mood scores and trends
- 📅 Browse notes by date

---

## 📚 API Documentation

| Document | Description |
|----------|-------------|
| [Authentication](AUTH.md) | User registration, login, tokens, logout |
| [Health Check](HEALTH.md) | API health status endpoint |

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

#### Example User Response
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "John",
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T09:00:00Z"
}
```

### Note

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `userId` | `UUID` | Owner user ID (from JWT, not exposed in API) |
| `content` | `String` | The note text (max 5000 chars) |
| `moodScore` | `Integer` | AI-generated score 1-10 (null if pending) |
| `moodSummary` | `String` | AI-generated brief summary |
| `highlights` | `List<String>` | Key positive/negative points extracted |
| `createdAt` | `DateTime` | When the note was created |
| `updatedAt` | `DateTime` | Last modification timestamp |
| `analyzedAt` | `DateTime` | When AI analysis completed (null if pending) |

> **Note:** Users can only access their own notes. The `userId` is automatically set from the JWT token.

#### Example Note Response
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Had a great morning walk in the park...",
  "moodScore": 7,
  "moodSummary": "A balanced day with both challenges and positive moments",
  "highlights": [
    "Morning walk in nature",
    "Project completion achievement",
    "Quality family time"
  ],
  "createdAt": "2026-01-10T09:30:00Z",
  "updatedAt": "2026-01-10T09:30:00Z",
  "analyzedAt": "2026-01-10T09:30:05Z"
}
```

---

## ❌ Error Responses

All errors follow this format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "details": {},
  "timestamp": "2026-01-10T14:30:00Z",
  "path": "/api/notes/invalid-id"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `INVALID_TOKEN` | 401 | Invalid, expired, or revoked token |
| `USER_ALREADY_EXISTS` | 409 | Email already registered |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Invalid request data |
| `AI_SERVICE_ERROR` | 503 | AI analysis temporarily unavailable |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 🐳 Docker Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | - | PostgreSQL connection string |
| `DATABASE_USERNAME` | Yes | - | Database username |
| `DATABASE_PASSWORD` | Yes | - | Database password |
| `JWT_SECRET` | Yes | - | Secret key for JWT signing (min 32 chars) |
| `JWT_ACCESS_EXPIRATION` | No | 900000 | Access token expiration in ms (15 min) |
| `JWT_REFRESH_EXPIRATION` | No | 604800000 | Refresh token expiration in ms (7 days) |
| `OPENAI_API_KEY` | Yes | - | OpenAI API key for mood analysis |
| `SERVER_PORT` | No | 8080 | Server port |

### docker-compose.yml

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/janusleaf
      - DATABASE_USERNAME=janusleaf
      - DATABASE_PASSWORD=secretpassword
      - JWT_SECRET=your-256-bit-secret-key-here-change-in-production-min-32-chars
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=janusleaf
      - POSTGRES_USER=janusleaf
      - POSTGRES_PASSWORD=secretpassword
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U janusleaf"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```
