-- V5: Create inspirational quotes table
-- Stores AI-generated inspirational quotes based on user's journal entries

CREATE TABLE inspirational_quotes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    quote TEXT NOT NULL,
    tags TEXT NOT NULL DEFAULT '',
    needs_regeneration BOOLEAN NOT NULL DEFAULT FALSE,
    last_generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for efficient lookup by user
CREATE INDEX idx_inspirational_quotes_user_id ON inspirational_quotes(user_id);

-- Index for finding quotes that need regeneration
CREATE INDEX idx_inspirational_quotes_needs_regen ON inspirational_quotes(needs_regeneration) WHERE needs_regeneration = TRUE;

-- Index for finding quotes older than 24 hours (for daily regeneration check)
CREATE INDEX idx_inspirational_quotes_last_generated ON inspirational_quotes(last_generated_at);

-- Comments
COMMENT ON TABLE inspirational_quotes IS 'AI-generated inspirational quotes based on user journal entries';
COMMENT ON COLUMN inspirational_quotes.user_id IS 'User this quote belongs to (unique - one quote per user)';
COMMENT ON COLUMN inspirational_quotes.quote IS 'The inspirational quote text';
COMMENT ON COLUMN inspirational_quotes.tags IS 'Comma-separated list of 4 tags extracted from journal themes';
COMMENT ON COLUMN inspirational_quotes.needs_regeneration IS 'Flag set when user creates new journal entry';
COMMENT ON COLUMN inspirational_quotes.last_generated_at IS 'When the quote was last generated (for daily refresh)';
