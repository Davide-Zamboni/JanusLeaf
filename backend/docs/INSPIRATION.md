# Inspirational Quote API

> Base URL: `/api/inspiration`

---

## Overview

The Inspirational Quote API provides AI-generated personalized inspirational quotes based on the user's journal entries. Each user receives a unique quote that reflects themes, emotions, and insights from their last 20 journal entries.

### Key Features

- 🌟 **Personalized quotes** - AI analyzes your journal entries to create meaningful, relevant inspiration
- 🏷️ **Thematic tags** - 4 tags extracted from recurring themes in your journals
- 🔄 **Smart regeneration** - Quotes update automatically based on your journaling activity
- ⚡ **Background processing** - Generation happens asynchronously without blocking requests

### Quote Generation Flow

```
┌─────────────────────────────────────────────────────────────┐
│              INSPIRATIONAL QUOTE GENERATION                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Regeneration Triggers:                                    │
│                                                             │
│  1. NEW JOURNAL ENTRY                                      │
│     → User creates entry via POST /api/journal             │
│     → Quote marked for regeneration (needsRegeneration)    │
│                                                             │
│  2. DAILY REFRESH                                          │
│     → Checked when lastGeneratedAt > 24 hours ago          │
│                                                             │
│  3. FIRST-TIME USER                                        │
│     → User has journal entries but no quote yet            │
│                                                             │
│  Processing:                                               │
│  - Scheduled job runs every 30 seconds                     │
│  - Finds quotes needing regeneration                       │
│  - AI analyzes last 20 journal entries                     │
│  - Generates new quote + 4 thematic tags                   │
│  - Saves to database (overwrites previous)                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Why Background Processing?

- ✅ Non-blocking API responses
- ✅ Survives server restarts
- ✅ Works with multiple server instances
- ✅ Handles AI API rate limits gracefully

---

## Endpoints

### 1. Get Inspirational Quote

Get the current AI-generated inspirational quote for the authenticated user.

```
GET /api/inspiration
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "quote": "Your journey through reflection shows remarkable growth. Each entry reveals strength you may not see, but it's there—woven through your words like threads of resilience.",
  "tags": ["growth", "resilience", "self-discovery", "reflection"],
  "generatedAt": "2026-01-10T09:00:00Z",
  "updatedAt": "2026-01-10T09:00:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier for the quote |
| `quote` | `String` | The personalized inspirational quote (1-3 sentences) |
| `tags` | `String[]` | 4 thematic tags extracted from journal themes |
| `generatedAt` | `Instant` | When this quote was generated |
| `updatedAt` | `Instant` | Last modification timestamp |

**Response:** `404 Not Found` (No quote yet)
```json
{
  "message": "No inspirational quote generated yet. One will be created shortly based on your journal entries."
}
```

This occurs when:
- User is new and hasn't written any journal entries yet
- Quote generation is still in progress (check again in ~30 seconds)

---

## 📱 Mobile App Integration

### Displaying the Quote

```
┌─────────────────────────────────────────────────────────────┐
│                   QUOTE DISPLAY FLOW                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. App loads / User opens dashboard                       │
│     → GET /api/inspiration                                 │
│                                                             │
│  2. On 200 OK:                                             │
│     → Display quote prominently                            │
│     → Show tags as badges/chips                            │
│     → Cache locally for offline access                     │
│                                                             │
│  3. On 404 Not Found:                                      │
│     → Show placeholder: "Your inspiration is brewing..."   │
│     → Retry after 30-60 seconds                            │
│     → Or prompt user to write their first entry            │
│                                                             │
│  4. Refresh strategy:                                      │
│     → Pull to refresh                                      │
│     → Auto-refresh when returning to app                   │
│     → After creating new journal entry                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### UI Suggestions

**Quote Card:**
```
┌────────────────────────────────────────────────────┐
│  ✨ Your Daily Inspiration                         │
│                                                    │
│  "Your journey through reflection shows           │
│   remarkable growth. Each entry reveals           │
│   strength you may not see..."                    │
│                                                    │
│  ┌──────────┐ ┌───────────┐ ┌────────────┐       │
│  │ growth   │ │ resilience│ │self-discovery│      │
│  └──────────┘ └───────────┘ └────────────┘       │
│  ┌────────────┐                                   │
│  │ reflection │                                   │
│  └────────────┘                                   │
│                                                    │
│  Generated today                                   │
└────────────────────────────────────────────────────┘
```

### Caching Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    CACHING STRATEGY                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Cache quote locally on device                          │
│     - Store id, quote, tags, generatedAt                   │
│                                                             │
│  2. Refresh conditions:                                    │
│     - App foreground after 1+ hours                        │
│     - After creating new journal entry                     │
│     - Manual pull-to-refresh                               │
│     - generatedAt older than 24 hours                      │
│                                                             │
│  3. Check if quote changed:                                │
│     - Compare generatedAt from API vs cached               │
│     - Animate update if different                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Model

### InspirationalQuote

| Field | Type | Description |
|-------|------|-------------|
| `id` | `UUID` | Unique identifier |
| `userId` | `UUID` | Owner (one quote per user) |
| `quote` | `String` | The inspirational quote text |
| `tags` | `String[]` | Array of 4 thematic tags |
| `needsRegeneration` | `Boolean` | Flag for pending regeneration |
| `lastGeneratedAt` | `Instant` | When quote was last generated |
| `createdAt` | `Instant` | First creation timestamp |
| `updatedAt` | `Instant` | Last modification timestamp |

---

## Configuration

The quote generation job can be configured via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `INSPIRATIONAL_QUOTE_CRON` | `*/30 * * * * *` | Cron expression for job schedule |

The job uses the same OpenRouter API configuration as mood analysis. See `SECRETS.md` for API key configuration.
