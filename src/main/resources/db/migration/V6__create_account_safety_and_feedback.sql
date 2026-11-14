CREATE TABLE verification_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    expires_at DATETIME(3) NOT NULL,
    used_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_verification_email_purpose (email, purpose, created_at),
    KEY idx_verification_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_consents (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    document_version VARCHAR(40) NOT NULL,
    accepted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_consents_version (user_id, document_type, document_version),
    CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE data_deletion_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    scope VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    last_error VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_deletion_requests_status (status, requested_at, id),
    KEY idx_deletion_requests_user (user_id, requested_at),
    CONSTRAINT fk_deletion_requests_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE safety_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    message_id BIGINT UNSIGNED NULL,
    risk_type VARCHAR(40) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    action_taken VARCHAR(40) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_safety_user_time (user_id, created_at),
    CONSTRAINT fk_safety_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_safety_events_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_safety_events_message FOREIGN KEY (message_id) REFERENCES messages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    message_id BIGINT UNSIGNED NOT NULL,
    rating VARCHAR(20) NOT NULL,
    reason_code VARCHAR(40) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_feedback_message (user_id, message_id),
    CONSTRAINT fk_user_feedback_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_feedback_message FOREIGN KEY (message_id) REFERENCES messages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
