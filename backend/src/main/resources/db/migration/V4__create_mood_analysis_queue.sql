-- V4: Create mood analysis queue table
-- Database-backed queue for debounced AI mood analysis

CREATE TABLE mood_analysis_queue (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL UNIQUE REFERENCES journal_entries(id) ON DELETE CASCADE,
    body_snapshot TEXT NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for efficient polling of ready-to-process records
-- Query: SELECT * FROM mood_analysis_queue WHERE scheduled_for <= now() ORDER BY scheduled_for
CREATE INDEX idx_mood_queue_scheduled_for ON mood_analysis_queue(scheduled_for);

-- Comments
COMMENT ON TABLE mood_analysis_queue IS 'Queue for pending AI mood analysis with debouncing';
COMMENT ON COLUMN mood_analysis_queue.journal_entry_id IS 'Journal entry to analyze (unique - only one pending per entry)';
COMMENT ON COLUMN mood_analysis_queue.body_snapshot IS 'Body content to analyze (captured at queue time)';
COMMENT ON COLUMN mood_analysis_queue.scheduled_for IS 'When to process (now + debounce delay). Updated on each edit.';
