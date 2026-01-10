# Journal API

> Base URL: `/api/journal`

---

## Overview

The Journal API allows users to create, read, update, and delete their daily journal entries. Each entry can have AI-powered mood analysis that scores the content from 1-10.

### Key Features

- 📝 **Create entries** with optional title (defaults to today's date)
- ✏️ **Google Docs-like editing** - Update body content with version control for concurrent edit detection
- 📅 **Date-based entries** - Each entry has an associated date
- 🤖 **Mood scoring** - AI-generated positivity score from 1 to 10
- 🔒 **User isolation** - Users can only access their own entries

### Version Control (Optimistic Locking)

Journal entries support optimistic locking for concurrent edit detection. Each entry has a `version` field that increments on every update. When updating the body, you can optionally provide an `expectedVersion` to ensure you're editing the latest version.

```
┌─────────────────────────────────────────────────────────────┐
│                 CONCURRENT EDIT DETECTION                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Client A: GET entry (version: 3)                          │
│  Client B: GET entry (version: 3)                          │
│                                                             │
│  Client A: PATCH body (expectedVersion: 3) → SUCCESS       │
│            → Entry now at version: 4                        │
│                                                             │
│  Client B: PATCH body (expectedVersion: 3) → CONFLICT      │
│            → Needs to refresh and retry                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Endpoints

### 1. Create Journal Entry

Create a new journal entry. Title defaults to today's date if not provided.

```
POST /api/journal
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "title": "A Great Day",
  "body": "Today was wonderful! I accomplished so much.",
  "entryDate": "2024-01-15"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | `String` | No | Entry title (max 255 chars). Defaults to entry date if not provided. |
| `body` | `String` | No | Entry content (max 50,000 chars). Defaults to empty string. |
| `entryDate` | `LocalDate` | No | Date for the entry (YYYY-MM-DD). Defaults to today. |

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "A Great Day",
  "body": "Today was wonderful! I accomplished so much.",
  "moodScore": null,
  "entryDate": "2024-01-15",
  "version": 0,
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T09:00:00Z"
}
```

**Examples:**

Creating an empty entry (just to start writing):
```json
{}
```
→ Creates entry with today's date as title, empty body

Creating entry without title:
```json
{
  "body": "My thoughts for today..."
}
```
→ Creates entry with today's date as title

---

### 2. Get Journal Entry

Get a specific journal entry by ID.

```
GET /api/journal/{id}
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "A Great Day",
  "body": "Today was wonderful! I accomplished so much.",
  "moodScore": 8,
  "entryDate": "2024-01-15",
  "version": 2,
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T10:30:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Entry not found or doesn't belong to user

---

### 3. List Journal Entries

Get paginated list of journal entries, ordered by entry date (newest first).

```
GET /api/journal?page=0&size=20
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | `Integer` | 0 | Page number (0-indexed) |
| `size` | `Integer` | 20 | Page size (max 100) |

**Response:** `200 OK`
```json
{
  "entries": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "title": "A Great Day",
      "bodyPreview": "Today was wonderful! I accomplished so much. The morning started with...",
      "moodScore": 8,
      "entryDate": "2024-01-15",
      "updatedAt": "2026-01-10T10:30:00Z"
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

> **Note:** `bodyPreview` is truncated to 150 characters with "..." appended if longer.

---

### 4. Get Entries by Date Range

Get journal entries within a specific date range.

```
GET /api/journal/range?startDate=2024-01-01&endDate=2024-01-31
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `startDate` | `LocalDate` | Yes | Start date (inclusive, YYYY-MM-DD) |
| `endDate` | `LocalDate` | Yes | End date (inclusive, YYYY-MM-DD) |

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "title": "January 15",
    "bodyPreview": "A productive day...",
    "moodScore": 8,
    "entryDate": "2024-01-15",
    "updatedAt": "2026-01-15T10:30:00Z"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "title": "January 10",
    "bodyPreview": "Started the week...",
    "moodScore": 7,
    "entryDate": "2024-01-10",
    "updatedAt": "2026-01-10T09:00:00Z"
  }
]
```

---

### 5. Update Entry Body

Update the body content of a journal entry. Supports optimistic locking for concurrent edit detection.

```
PATCH /api/journal/{id}/body
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "body": "Updated content with more details about my day...",
  "expectedVersion": 2
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `body` | `String` | Yes | New body content (max 50,000 chars) |
| `expectedVersion` | `Long` | No | Expected version for optimistic locking. If provided and doesn't match, returns 409 Conflict. |

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "body": "Updated content with more details about my day...",
  "version": 3,
  "updatedAt": "2026-01-10T11:00:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Entry not found
- `409 Conflict` - Version mismatch (concurrent edit detected)

---

### 6. Update Entry Metadata

Update title and/or mood score of a journal entry.

```
PATCH /api/journal/{id}
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "title": "My Wonderful Day",
  "moodScore": 9
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | `String` | No | New title (max 255 chars) |
| `moodScore` | `Integer` | No | Mood score (1-10) |

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "My Wonderful Day",
  "body": "Today was wonderful...",
  "moodScore": 9,
  "entryDate": "2024-01-15",
  "version": 3,
  "createdAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T11:30:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid mood score (must be 1-10)
- `404 Not Found` - Entry not found

---

### 7. Delete Journal Entry

Delete a journal entry.

```
DELETE /api/journal/{id}
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "message": "Journal entry deleted successfully"
}
```

**Error Responses:**
- `404 Not Found` - Entry not found

---

## 📱 Mobile App Integration

### Creating a New Entry Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   CREATE ENTRY FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. User taps "New Entry"                                  │
│     → POST /api/journal with {} (empty body)               │
│     → Store returned entry ID and version                  │
│                                                             │
│  2. User starts typing                                     │
│     → Auto-save every few seconds:                         │
│       PATCH /api/journal/{id}/body                         │
│       { "body": "current content" }                        │
│                                                             │
│  3. User can optionally:                                   │
│     - Edit title: PATCH /api/journal/{id}                  │
│     - Change date: (set at creation time)                  │
│                                                             │
│  4. AI analyzes content and sets moodScore                 │
│     (handled by backend asynchronously)                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Handling Concurrent Edits

When editing on multiple devices:

1. **Fetch entry** - Store the `version` from response
2. **Before save** - Include `expectedVersion` in request
3. **On 409 Conflict**:
   - Fetch latest entry
   - Show user both versions
   - Let user merge or choose

### Offline Support

For offline-first apps:
1. Queue body updates locally
2. When online, replay updates with version checks
3. Handle conflicts gracefully

---

## Data Model

### JournalEntry

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `userId` | `UUID` | Owner (from JWT, not in response) |
| `title` | `String` | Entry title (max 255 chars) |
| `body` | `String` | Entry content (max 50,000 chars) |
| `moodScore` | `Integer?` | AI-generated score 1-10 (nullable) |
| `entryDate` | `LocalDate` | Date the entry is for |
| `version` | `Long` | Optimistic locking version |
| `createdAt` | `Instant` | Creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |
