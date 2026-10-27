CREATE TABLE external_dsa_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    profile_url VARCHAR(2048),
    status VARCHAR(50) NOT NULL,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_external_dsa_account_user_platform UNIQUE (user_id, platform)
);

CREATE INDEX idx_external_dsa_accounts_user_id ON external_dsa_accounts(user_id);
CREATE INDEX idx_external_dsa_accounts_username ON external_dsa_accounts(username);

CREATE TABLE leetcode_stats (
    id BIGSERIAL PRIMARY KEY,
    external_dsa_account_id BIGINT NOT NULL REFERENCES external_dsa_accounts(id) ON DELETE CASCADE,
    total_solved INT,
    easy_solved INT,
    medium_solved INT,
    hard_solved INT,
    ranking INT,
    last_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_leetcode_stats_account UNIQUE (external_dsa_account_id)
);
