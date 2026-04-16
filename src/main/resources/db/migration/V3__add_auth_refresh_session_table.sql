CREATE TABLE IF NOT EXISTS auth_refresh_session (
    session_id VARCHAR(128) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT uk_auth_refresh_session_token_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_auth_refresh_session_account_id ON auth_refresh_session (account_id);
CREATE INDEX idx_auth_refresh_session_expires_at ON auth_refresh_session (expires_at);
