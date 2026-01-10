-- V3: Create journal_entries table
-- Stores user journal entries for mood tracking

CREATE TABLE journal_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT DEFAULT '',
    mood_score INTEGER CHECK (mood_score >= 1 AND mood_score <= 10),
    entry_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for user's entries (most common query)
CREATE INDEX idx_journal_entries_user_id ON journal_entries(user_id);

-- Index for date-based queries
CREATE INDEX idx_journal_entries_entry_date ON journal_entries(entry_date);

-- Composite index for user + date queries (e.g., get entry for a specific date)
CREATE INDEX idx_journal_entries_user_date ON journal_entries(user_id, entry_date);

-- Add comments
COMMENT ON TABLE journal_entries IS 'User journal entries for mood tracking in JanusLeaf';
COMMENT ON COLUMN journal_entries.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN journal_entries.user_id IS 'Owner of the journal entry';
COMMENT ON COLUMN journal_entries.title IS 'Entry title, defaults to date if not provided';
COMMENT ON COLUMN journal_entries.body IS 'Full content of the journal entry';
COMMENT ON COLUMN journal_entries.mood_score IS 'AI-generated mood score from 1-10';
COMMENT ON COLUMN journal_entries.entry_date IS 'Date the entry is for (not creation date)';
COMMENT ON COLUMN journal_entries.version IS 'Optimistic locking version for concurrent edits';
COMMENT ON COLUMN journal_entries.created_at IS 'Entry creation timestamp';
COMMENT ON COLUMN journal_entries.updated_at IS 'Last modification timestamp';
