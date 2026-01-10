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

## 🔐 Authentication

JanusLeaf uses **JWT (JSON Web Token)** authentication. All endpoints except `/api/auth/*` require a valid JWT token.

### Authentication Flow

```
1. Register: POST /api/auth/register
2. Login:    POST /api/auth/login    → Returns JWT token
3. Use API:  Include token in header → Authorization: Bearer <token>
```

### Token Details

| Property | Value |
|----------|-------|
| Type | Bearer Token |
| Location | `Authorization` header |
| Format | `Bearer eyJhbGciOiJIUzI1NiIs...` |
| Expiration | 24 hours |
| Refresh | Use `/api/auth/refresh` endpoint |

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
  "content": "Had a great morning walk in the park. Work was stressful but I managed to finish the project. Enjoyed dinner with family.",
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

## 🔌 API Endpoints

### Authentication 🔐

#### 1. Register New User

Create a new user account.

```
POST /api/auth/register
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "username": "John",
  "password": "SecurePass123!"
}
```

**Validation Rules:**
- `email`: Valid email format, unique
- `username`: 2-50 characters
- `password`: Min 8 chars, 1 uppercase, 1 lowercase, 1 number

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "John",
  "createdAt": "2026-01-10T09:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Email already registered

---

#### 2. Login

Authenticate and receive JWT token.

```
POST /api/auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "John"
  }
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials

---

#### 3. Refresh Token

Get a new access token using refresh token.

```
POST /api/auth/refresh
```

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired refresh token

---

#### 4. Get Current User

Get the authenticated user's profile.

```
GET /api/auth/me
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "John",
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T09:00:00Z"
}
```

---

#### 5. Update Profile

Update the authenticated user's profile.

```
PUT /api/auth/me
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "username": "Johnny"
}
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "Johnny",
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T10:30:00Z"
}
```

---

#### 6. Change Password

Change the authenticated user's password.

```
POST /api/auth/change-password
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "currentPassword": "SecurePass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Response:** `200 OK`
```json
{
  "message": "Password changed successfully"
}
```

**Error Responses:**
- `400 Bad Request` - Current password incorrect
- `400 Bad Request` - New password doesn't meet requirements

---

#### 7. Logout

Invalidate refresh token (optional - tokens can also just expire).

```
POST /api/auth/logout
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "message": "Logged out successfully"
}
```

---

### Notes 📝

> ⚠️ **All note endpoints require authentication.** Include `Authorization: Bearer <token>` header.

#### 1. Get Recent Notes (Homepage)

Returns paginated list of notes sorted by most recent, perfect for the homepage view.

```
GET /api/notes
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | Integer | 0 | Page number (0-indexed) |
| `size` | Integer | 20 | Items per page (max 100) |
| `sortBy` | String | `createdAt` | Sort field |
| `sortDir` | String | `desc` | Sort direction (`asc`/`desc`) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "content": "Had a great morning walk...",
      "moodScore": 7,
      "moodSummary": "A balanced day...",
      "highlights": ["Morning walk", "Project completion"],
      "createdAt": "2026-01-10T09:30:00Z",
      "updatedAt": "2026-01-10T09:30:00Z",
      "analyzedAt": "2026-01-10T09:30:05Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3,
  "hasNext": true,
  "hasPrevious": false
}
```

---

#### 2. Get Single Note

Retrieve a specific note by ID.

```
GET /api/notes/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Note unique identifier |

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Had a great morning walk in the park...",
  "moodScore": 7,
  "moodSummary": "A balanced day with both challenges and positive moments",
  "highlights": ["Morning walk in nature", "Project completion achievement"],
  "createdAt": "2026-01-10T09:30:00Z",
  "updatedAt": "2026-01-10T09:30:00Z",
  "analyzedAt": "2026-01-10T09:30:05Z"
}
```

**Error Responses:**
- `404 Not Found` - Note does not exist

---

#### 3. Create Note

Create a new note. AI analysis is triggered automatically.

```
POST /api/notes
```

**Request Body:**
```json
{
  "content": "Today I woke up feeling refreshed. Had a productive meeting at work and learned something new about Kotlin coroutines."
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "content": "Today I woke up feeling refreshed...",
  "moodScore": null,
  "moodSummary": null,
  "highlights": null,
  "createdAt": "2026-01-10T14:22:00Z",
  "updatedAt": "2026-01-10T14:22:00Z",
  "analyzedAt": null,
  "analysisStatus": "PENDING"
}
```

> **Note:** The `moodScore` will be `null` initially. The AI analysis runs asynchronously. Poll the note or use the analysis endpoint to check status.

**Validation Errors:** `400 Bad Request`
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Content must be between 1 and 5000 characters",
  "timestamp": "2026-01-10T14:22:00Z"
}
```

---

#### 4. Update Note

Update an existing note. Re-triggers AI analysis if content changed.

```
PUT /api/notes/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Note unique identifier |

**Request Body:**
```json
{
  "content": "Updated note content with more details about my day..."
}
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Updated note content with more details...",
  "moodScore": null,
  "moodSummary": null,
  "highlights": null,
  "createdAt": "2026-01-10T09:30:00Z",
  "updatedAt": "2026-01-10T15:45:00Z",
  "analyzedAt": null,
  "analysisStatus": "PENDING"
}
```

**Error Responses:**
- `404 Not Found` - Note does not exist

---

#### 5. Delete Note

Permanently delete a note.

```
DELETE /api/notes/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | UUID | Note unique identifier |

**Response:** `204 No Content`

**Error Responses:**
- `404 Not Found` - Note does not exist

---

### Analysis

#### 6. Get Analysis Status

Check the AI analysis status for a note.

```
GET /api/notes/{id}/analysis
```

**Response:** `200 OK`
```json
{
  "noteId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "moodScore": 7,
  "moodSummary": "A balanced day with both challenges and positive moments",
  "highlights": ["Morning walk in nature", "Project completion"],
  "analyzedAt": "2026-01-10T09:30:05Z"
}
```

**Status Values:**
- `PENDING` - Analysis not yet started
- `PROCESSING` - AI is analyzing
- `COMPLETED` - Analysis finished
- `FAILED` - Analysis failed (will retry)

---

#### 7. Retry Analysis

Manually trigger re-analysis for a note.

```
POST /api/notes/{id}/analyze
```

**Response:** `202 Accepted`
```json
{
  "noteId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Analysis has been queued"
}
```

---

### Statistics

#### 8. Get Mood Statistics

Get aggregated mood statistics for dashboard/insights.

```
GET /api/stats
```

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `from` | Date | 30 days ago | Start date (ISO format) |
| `to` | Date | today | End date (ISO format) |

**Response:** `200 OK`
```json
{
  "period": {
    "from": "2025-12-10",
    "to": "2026-01-10"
  },
  "totalNotes": 28,
  "averageMoodScore": 6.8,
  "highestScore": 9,
  "lowestScore": 4,
  "moodDistribution": {
    "low": 3,
    "medium": 12,
    "high": 13
  },
  "dailyScores": [
    { "date": "2026-01-10", "score": 7 },
    { "date": "2026-01-09", "score": 8 },
    { "date": "2026-01-08", "score": 6 }
  ]
}
```

---

### Health Check

#### 9. Health Check

Check if the API is running.

```
GET /api/health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "version": "1.0.0",
  "database": "UP",
  "aiService": "UP",
  "timestamp": "2026-01-10T14:30:00Z"
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

---

## 📱 Mobile App Integration

### Recommended Flow

1. **Homepage Load:**
   ```
   GET /api/notes?page=0&size=20
   ```

2. **Create New Note:**
   ```
   POST /api/notes
   Body: { "content": "..." }
   ```
   Then poll for analysis or show "Analyzing..." state

3. **View Note Detail:**
   ```
   GET /api/notes/{id}
   ```

4. **Pull to Refresh:**
   ```
   GET /api/notes?page=0&size=20
   ```

---

## 🔒 Future Enhancements (v2.0)

- [ ] User authentication (JWT)
- [ ] Multiple users support
- [ ] Note tags/categories
- [ ] Search functionality
- [ ] Export notes
- [ ] Mood trends graphs
- [ ] Push notifications for daily reminders
