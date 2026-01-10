# Authentication API

> Base URL: `/api/auth`

---

## Overview

JanusLeaf uses a **hybrid JWT authentication** system with short-lived access tokens and server-side refresh tokens for enhanced security.

### Authentication Flow

```
1. Register/Login → Returns access token + refresh token
2. Use API       → Include access token: Authorization: Bearer <token>
3. Token expires → Use refresh token to get new access token
4. Logout        → Revoke refresh token server-side
```

### Token Details

| Token Type | Expiration | Storage | Purpose |
|------------|------------|---------|---------|
| **Access Token** | 15 minutes | Client only | API authentication |
| **Refresh Token** | 7 days | Client + Server (PostgreSQL) | Get new access tokens |

### Security Features

- ✅ **Short-lived access tokens** - Minimizes exposure window if token is stolen
- ✅ **Server-side refresh tokens** - Can be revoked instantly on logout
- ✅ **Password change** - Automatically revokes all active sessions
- ✅ **Logout from all devices** - Revoke all refresh tokens at once

---

## Endpoints

### 1. Register New User

Create a new user account and receive tokens.

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
- `password`: Minimum 8 characters

**Response:** `201 Created`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "John",
    "createdAt": "2026-01-10T09:00:00Z",
    "updatedAt": "2026-01-10T09:00:00Z"
  }
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Email already registered

---

### 2. Login

Authenticate and receive JWT tokens.

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
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "username": "John",
    "createdAt": "2026-01-10T09:00:00Z",
    "updatedAt": "2026-01-10T09:00:00Z"
  }
}
```

> **Note:** `expiresIn` is in seconds (900 = 15 minutes)

**Error Responses:**
- `401 Unauthorized` - Invalid credentials

---

### 3. Refresh Token

Get a new access token using a valid refresh token.

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
  "expiresIn": 900
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid, expired, or revoked refresh token

---

### 4. Logout

Revoke a specific refresh token (logout from current device).

```
POST /api/auth/logout
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
  "message": "Logged out successfully"
}
```

> **Note:** This endpoint is public and will succeed even with an invalid token (graceful logout).

---

### 5. Logout from All Devices

Revoke all refresh tokens for the authenticated user.

```
POST /api/auth/logout-all
```

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** `200 OK`
```json
{
  "message": "Logged out from 3 device(s)"
}
```

> **Use case:** User suspects their account was compromised, or wants to sign out everywhere.

---

### 6. Get Current User

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

### 7. Update Profile

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

### 8. Change Password

Change the authenticated user's password. **This revokes all active sessions.**

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
  "message": "Password changed successfully. Please login again on all devices."
}
```

> ⚠️ **Important:** All refresh tokens are revoked after password change. Users must re-authenticate on all devices.

**Error Responses:**
- `401 Unauthorized` - Current password is incorrect
- `400 Bad Request` - New password doesn't meet requirements

---

## 📱 Mobile App Integration

### Token Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    TOKEN MANAGEMENT                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Login/Register → Store both tokens securely             │
│                                                             │
│  2. API Call → Use access token                             │
│     ├─ 200 OK → Continue                                    │
│     └─ 401/403 → Try refresh token                          │
│         ├─ 200 OK → Update access token, retry              │
│         └─ 401 → Clear tokens, redirect to login            │
│                                                             │
│  3. Logout → Call /api/auth/logout with refresh token       │
│             Then clear local storage                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Security Best Practices

1. **Store tokens securely:**
   - iOS: Keychain
   - Android: EncryptedSharedPreferences or Keystore

2. **Never store tokens in:**
   - Plain SharedPreferences
   - UserDefaults without encryption
   - Local storage (web)

3. **Handle token refresh proactively:**
   - Check expiration before API calls
   - Refresh when < 2 minutes remaining

4. **On logout:**
   - Call `/api/auth/logout` to revoke server-side
   - Clear all local token storage

5. **On security concerns:**
   - Call `/api/auth/logout-all` to sign out everywhere
   - Prompt user to change password
