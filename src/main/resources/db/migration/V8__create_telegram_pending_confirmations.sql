CREATE TABLE telegram_pending_confirmations (
    token VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    task_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_telegram_pending_confirmations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
