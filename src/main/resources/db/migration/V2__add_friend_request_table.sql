CREATE TABLE IF NOT EXISTS friend_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(128) NOT NULL,
    requester_account_id VARCHAR(64) NOT NULL,
    requester_display_name VARCHAR(128) NOT NULL,
    target_account_id VARCHAR(64) NOT NULL,
    target_display_name VARCHAR(128) NOT NULL,
    note VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    CONSTRAINT uk_friend_request_request_id UNIQUE (request_id)
);

CREATE INDEX idx_friend_request_target_status ON friend_request (target_account_id, status, created_at);
CREATE INDEX idx_friend_request_requester_status ON friend_request (requester_account_id, status, created_at);
