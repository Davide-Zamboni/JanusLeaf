# JanusLeaf - Development TODO List

> 📅 Created: January 10, 2026  
> 🎯 Goal: Mood-tracking journal with AI analysis

---

## Phase 1: Project Setup 🏗️

- [ ] **1.1** Initialize Spring Boot project with Kotlin
  - Spring Web
  - Spring Data JPA
  - PostgreSQL Driver
  - Spring Validation
  - Spring Security
  - Kotlin Coroutines support
  - JJWT (JWT library)

- [ ] **1.2** Create Docker configuration
  - [ ] `Dockerfile` for Spring Boot app
  - [ ] `docker-compose.yml` with PostgreSQL
  - [ ] `.env.example` file

- [ ] **1.3** Configure application properties
  - [ ] Database connection
  - [ ] OpenAI API key placeholder
  - [ ] JWT secret and expiration
  - [ ] Server port and profiles

---

## Phase 2: Database Layer 🗄️

- [ ] **2.1** Create `User` entity
  ```kotlin
  @Entity
  @Table(name = "users")
  data class User(
      @Id val id: UUID = UUID.randomUUID(),
      @Column(unique = true) val email: String,
      val username: String,
      val passwordHash: String,
      val createdAt: Instant = Instant.now(),
      val updatedAt: Instant = Instant.now()
  )
  ```

- [ ] **2.2** Create `UserRepository` interface
  - `findByEmail(email: String): User?`
  - `existsByEmail(email: String): Boolean`

- [ ] **2.3** Create `Note` entity
  ```kotlin
  @Entity
  @Table(name = "notes")
  data class Note(
      @Id val id: UUID = UUID.randomUUID(),
      val userId: UUID,  // Foreign key to User
      val content: String,
      val moodScore: Int?,
      val moodSummary: String?,
      val highlights: List<String>?,
      val createdAt: Instant = Instant.now(),
      val updatedAt: Instant = Instant.now(),
      val analyzedAt: Instant?
  )
  ```

- [ ] **2.4** Create `NoteRepository` interface
  - `findAllByUserIdOrderByCreatedAtDesc(userId: UUID)` - for homepage
  - Pagination support

- [ ] **2.5** Create database migration scripts (Flyway/Liquibase)

---

## Phase 3: Authentication 🔐

- [ ] **3.1** Configure Spring Security
  - [ ] Security filter chain
  - [ ] Password encoder (BCrypt)
  - [ ] Disable CSRF (API only)
  - [ ] Stateless session

- [ ] **3.2** Implement JWT utilities
  - [ ] `JwtTokenProvider` - generate & validate tokens
  - [ ] `JwtAuthenticationFilter` - extract token from header
  - [ ] Access token (24h) + Refresh token (7 days)

- [ ] **3.3** Create Auth DTOs
  - [ ] `RegisterRequest` (email, username, password)
  - [ ] `LoginRequest` (email, password)
  - [ ] `AuthResponse` (tokens + user)
  - [ ] `RefreshTokenRequest`
  - [ ] `ChangePasswordRequest`

- [ ] **3.4** Create `AuthService`
  - [ ] `register()` - create user, hash password
  - [ ] `login()` - verify credentials, generate tokens
  - [ ] `refreshToken()` - validate & issue new access token
  - [ ] `changePassword()` - verify old, set new

- [ ] **3.5** Create `AuthController`
  - [ ] `POST /api/auth/register`
  - [ ] `POST /api/auth/login`
  - [ ] `POST /api/auth/refresh`
  - [ ] `GET /api/auth/me`
  - [ ] `PUT /api/auth/me`
  - [ ] `POST /api/auth/change-password`
  - [ ] `POST /api/auth/logout`

- [ ] **3.6** Create `@CurrentUser` annotation
  - Extract user from JWT for use in controllers

---

## Phase 4: API Endpoints 🔌

### Notes CRUD (All require authentication)

- [ ] **4.1** `GET /api/notes` - List user's notes (paginated)
  - Query params: `page`, `size`, `sortBy`, `sortDir`
  - Returns: Paginated list with mood scores
  - Filter by current user (from JWT)

- [ ] **4.2** `GET /api/notes/{id}` - Get single note
  - Returns: Full note with analysis
  - Error: 404 if not found or not owned by user

- [ ] **4.3** `POST /api/notes` - Create new note
  - Body: `{ "content": "..." }`
  - Associates with current user
  - Triggers async AI analysis
  - Returns: Created note (score pending)

- [ ] **4.4** `PUT /api/notes/{id}` - Update note
  - Only owner can update
  - Re-triggers AI analysis if content changed
  - Returns: Updated note

- [ ] **4.5** `DELETE /api/notes/{id}` - Delete note
  - Only owner can delete
  - Returns: 204 No Content

### Analysis

- [ ] **4.6** `GET /api/notes/{id}/analysis` - Get analysis status
  - Returns: Analysis status and results

- [ ] **4.7** `POST /api/notes/{id}/analyze` - Retry analysis
  - Returns: 202 Accepted

### Statistics

- [ ] **4.8** `GET /api/stats` - Get mood statistics
  - Query params: `from`, `to`
  - Returns: Aggregated mood data for current user

### Health

- [ ] **4.9** `GET /api/health` - Health check (no auth required)
  - Returns: Service status

---

## Phase 5: AI Integration 🤖

- [ ] **5.1** Create `OpenAIService` class
  - HTTP client for OpenAI API
  - Retry logic with exponential backoff

- [ ] **5.2** Design mood analysis prompt
  ```
  Analyze this journal entry and provide:
  1. Mood score (1-10, where 10 is most positive)
  2. Brief summary (max 100 chars)
  3. Key highlights (positive and negative points)
  
  Journal entry: {content}
  ```

- [ ] **5.3** Create `MoodAnalysisResult` data class
  ```kotlin
  data class MoodAnalysisResult(
      val score: Int,
      val summary: String,
      val highlights: List<String>
  )
  ```

- [ ] **5.4** Implement async analysis
  - Use Spring `@Async` or Kotlin coroutines
  - Update note after analysis completes

- [ ] **5.5** Handle AI failures gracefully
  - Retry mechanism
  - Fallback status

---

## Phase 6: Service Layer 📦

- [ ] **6.1** Create `NoteService`
  - Business logic for CRUD operations
  - Validation rules
  - Ownership verification

- [ ] **6.2** Create `AnalysisService`
  - Orchestrate AI analysis
  - Handle async processing

- [ ] **6.3** Create `StatsService`
  - Calculate mood statistics
  - Aggregate data by period and user

---

## Phase 7: DTOs & Mapping 🔄

- [ ] **7.1** Create Request DTOs
  - `CreateNoteRequest`
  - `UpdateNoteRequest`

- [ ] **7.2** Create Response DTOs
  - `NoteResponse`
  - `NoteListResponse` (paginated)
  - `AnalysisResponse`
  - `StatsResponse`
  - `ErrorResponse`

- [ ] **7.3** Create mappers (Entity <-> DTO)

---

## Phase 8: Error Handling ⚠️

- [ ] **8.1** Create custom exceptions
  - `NoteNotFoundException`
  - `NoteAccessDeniedException`
  - `AnalysisFailedException`
  - `UserAlreadyExistsException`
  - `InvalidCredentialsException`

- [ ] **8.2** Create `GlobalExceptionHandler`
  - Consistent error response format
  - Proper HTTP status codes

---

## Phase 9: Testing 🧪

- [ ] **9.1** Unit tests for services
- [ ] **9.2** Integration tests for repositories
- [ ] **9.3** API tests for controllers
- [ ] **9.4** Authentication tests
- [ ] **9.5** Mock OpenAI service for tests

---

## Phase 10: Documentation 📚

- [x] **10.1** API documentation (this file + API_DOCUMENTATION.md)
- [x] **10.2** README.md with setup instructions
- [ ] **10.3** Postman/Insomnia collection

---

## Phase 11: Deployment 🚀

- [ ] **11.1** Final Docker image optimization
- [ ] **11.2** Environment variable documentation
- [ ] **11.3** Health checks configuration
- [ ] **11.4** Logging configuration

---

## API Summary Table

| Method | Endpoint | Description | Priority | Auth |
|--------|----------|-------------|----------|------|
| `POST` | `/api/auth/register` | Register new user | 🔴 High | ❌ |
| `POST` | `/api/auth/login` | Login, get JWT | 🔴 High | ❌ |
| `POST` | `/api/auth/refresh` | Refresh token | 🔴 High | ❌ |
| `GET` | `/api/auth/me` | Get current user | 🟡 Medium | ✅ |
| `PUT` | `/api/auth/me` | Update profile | 🟡 Medium | ✅ |
| `POST` | `/api/auth/change-password` | Change password | 🟢 Low | ✅ |
| `POST` | `/api/auth/logout` | Logout | 🟢 Low | ✅ |
| `GET` | `/api/notes` | List user's notes | 🔴 High | ✅ |
| `GET` | `/api/notes/{id}` | Get single note | 🔴 High | ✅ |
| `POST` | `/api/notes` | Create note | 🔴 High | ✅ |
| `PUT` | `/api/notes/{id}` | Update note | 🟡 Medium | ✅ |
| `DELETE` | `/api/notes/{id}` | Delete note | 🟡 Medium | ✅ |
| `GET` | `/api/notes/{id}/analysis` | Analysis status | 🟡 Medium | ✅ |
| `POST` | `/api/notes/{id}/analyze` | Retry analysis | 🟢 Low | ✅ |
| `GET` | `/api/stats` | Mood statistics | 🟢 Low | ✅ |
| `GET` | `/api/health` | Health check | 🔴 High | ❌ |

---

## Quick Start Order

For MVP (Minimum Viable Product), implement in this order:

1. ✅ Project setup + Docker (Phase 1)
2. ✅ User entity + repository (Phase 2.1-2.2)
3. ✅ Spring Security + JWT setup (Phase 3.1-3.2)
4. ✅ `POST /api/auth/register` - Register (Phase 3.5)
5. ✅ `POST /api/auth/login` - Login (Phase 3.5)
6. ✅ Note entity + repository (Phase 2.3-2.4)
7. ✅ `POST /api/notes` - Create note (Phase 4.3)
8. ✅ `GET /api/notes` - List notes (Phase 4.1)
9. ✅ `GET /api/notes/{id}` - View note (Phase 4.2)
10. ✅ OpenAI integration (Phase 5)
11. ✅ Health check (Phase 4.9)

This gives you a working app where you can:
- Register and login 🔐
- Write notes ✍️
- See all your notes with scores 📋
- View individual notes 👁️

---

## Mobile App Screens (Reference)

| Screen | API Calls |
|--------|-----------|
| **Welcome/Splash** | Check stored token validity |
| **Register** | `POST /api/auth/register` |
| **Login** | `POST /api/auth/login` |
| **Homepage** | `GET /api/notes` |
| **Add Note** | `POST /api/notes` |
| **View Note** | `GET /api/notes/{id}` |
| **Edit Note** | `PUT /api/notes/{id}` |
| **Profile** | `GET /api/auth/me`, `PUT /api/auth/me` |
| **Change Password** | `POST /api/auth/change-password` |
| **Stats** (future) | `GET /api/stats` |
