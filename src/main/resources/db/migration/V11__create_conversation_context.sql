CREATE TABLE conversation_context (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chat_id VARCHAR(255) NOT NULL,
    context_type VARCHAR(255) NOT NULL,
    context_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_conversation_context_user_chat UNIQUE (user_id, chat_id)
);

CREATE INDEX idx_conversation_context_expires_at ON conversation_context(expires_at);
