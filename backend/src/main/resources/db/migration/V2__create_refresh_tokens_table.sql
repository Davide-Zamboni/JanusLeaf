-- V2: Create refresh_tokens table
-- Stores JWT refresh tokens for session management

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP WITH TIME ZONE
);

-- Index for token lookups (used in refresh flow)
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Index for user's tokens (used in logout-all)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for cleanup of expired/revoked tokens
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Add comments
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens for session management';
COMMENT ON COLUMN refresh_tokens.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token';
COMMENT ON COLUMN refresh_tokens.user_id IS 'Owner of the token';
COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp';
COMMENT ON COLUMN refresh_tokens.created_at IS 'Token creation timestamp';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'When token was revoked (null if active)';
