-- V10: Rate limit buckets table for DB-backed sliding window rate limiting.
-- Each row tracks the request count for a (bucket_key, window_start) pair.
-- bucket_key encodes the limitable resource: e.g. "auth:login:127.0.0.1"
-- window_start is truncated to the window duration (e.g. per-minute).
-- INSERT ... ON CONFLICT allows atomic upsert without row-level locking overhead.
CREATE TABLE IF NOT EXISTS rate_limit_buckets (
    id             BIGSERIAL PRIMARY KEY,
    bucket_key     VARCHAR(512)             NOT NULL,
    window_start   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    request_count  INTEGER                  NOT NULL DEFAULT 1,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rate_limit_bucket UNIQUE (bucket_key, window_start)
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_bucket_key_window
    ON rate_limit_buckets (bucket_key, window_start);

-- Auto-cleanup: rows older than 1 hour have no further use.
-- A scheduled job or application-level cleanup will handle this.
-- No hard delete trigger here to keep migrations clean.
