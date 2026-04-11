CREATE TABLE IF NOT EXISTS account (
    account_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(128) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auth_session (
    access_token VARCHAR(128) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS friend_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_account_id VARCHAR(64) NOT NULL,
    friend_account_id VARCHAR(64) NOT NULL,
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_friend_relation UNIQUE (owner_account_id, friend_account_id)
);

CREATE TABLE IF NOT EXISTS conversation_thread (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_account_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    subtitle VARCHAR(255) NOT NULL,
    unread_count INT NOT NULL DEFAULT 0,
    sync_cursor VARCHAR(128) NOT NULL DEFAULT '',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conversation_thread UNIQUE (owner_account_id, conversation_id)
);

CREATE TABLE IF NOT EXISTS conversation_message (
    id VARCHAR(128) PRIMARY KEY,
    owner_account_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    sender_account_id VARCHAR(64) NOT NULL,
    sender_display_name VARCHAR(128) NOT NULL,
    body TEXT NOT NULL,
    timestamp_label VARCHAR(64) NOT NULL,
    from_current_user BOOLEAN NOT NULL,
    delivery_status VARCHAR(32) NOT NULL,
    status_updated_at VARCHAR(64) NOT NULL,
    sequence_no BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_conversation_message_sequence UNIQUE (owner_account_id, conversation_id, sequence_no)
);
