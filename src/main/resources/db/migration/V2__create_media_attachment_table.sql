CREATE TABLE IF NOT EXISTS media_attachment (
    attachment_id VARCHAR(128) PRIMARY KEY,
    session_id VARCHAR(128) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64),
    attachment_type VARCHAR(32) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    content_length BIGINT NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    upload_url VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_media_attachment_session UNIQUE (session_id)
);
