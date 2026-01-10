# Health Check API

> Base URL: `/api`

---

## Overview

The health check endpoint allows you to verify that the API is running and operational. This endpoint is **public** and does not require authentication.

---

## Endpoints

### Health Check

Check if the API is running.

```
GET /api/health
```

**Authentication:** Not required

**Response:** `200 OK`
```json
{
  "status": "UP",
  "version": "1.0.0",
  "timestamp": "2026-01-10T14:30:00Z"
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Service status (`UP` or `DOWN`) |
| `version` | String | API version number |
| `timestamp` | DateTime | Current server time (ISO 8601) |

---

## Usage

### Kubernetes / Docker Health Checks

Use this endpoint for container orchestration health probes:

**Docker Compose:**
```yaml
services:
  app:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

**Kubernetes:**
```yaml
livenessProbe:
  httpGet:
    path: /api/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /api/health
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

### Monitoring

This endpoint can be used with monitoring tools like:
- Prometheus (with HTTP probe)
- Datadog
- New Relic
- UptimeRobot
- Pingdom
