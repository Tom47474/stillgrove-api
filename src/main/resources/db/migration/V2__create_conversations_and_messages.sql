CREATE TABLE conversations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(120) NOT NULL DEFAULT '新的对话',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    archived_at DATETIME(3) NULL,
    deleted_at DATETIME(3) NULL,
    next_sequence INT UNSIGNED NOT NULL DEFAULT 1,
    last_message_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_conversations_user_list (user_id, status, last_message_at, id),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sequence_no INT UNSIGNED NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    content_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    error_code VARCHAR(100) NULL,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_messages_conversation_sequence (conversation_id, sequence_no),
    KEY idx_messages_user_conversation (user_id, conversation_id),
    CONSTRAINT fk_messages_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
