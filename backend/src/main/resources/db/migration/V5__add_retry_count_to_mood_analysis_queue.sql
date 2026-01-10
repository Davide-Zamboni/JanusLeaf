-- Add retry_count column for exponential backoff on rate limit errors
ALTER TABLE mood_analysis_queue ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
