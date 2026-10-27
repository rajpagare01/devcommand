CREATE TABLE telegram_processed_updates (
    update_id BIGINT PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
