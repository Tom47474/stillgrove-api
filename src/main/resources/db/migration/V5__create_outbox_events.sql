CREATE TABLE outbox_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT UNSIGNED NOT NULL,
    aggregate_version INT UNSIGNED NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    max_attempts INT UNSIGNED NOT NULL DEFAULT 10,
    next_retry_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_error VARCHAR(500) NULL,
    sent_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_outbox_ready (status, next_retry_at, id),
    KEY idx_outbox_aggregate (aggregate_type, aggregate_id, aggregate_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE consumed_events (
    consumer_name VARCHAR(80) NOT NULL,
    event_id BIGINT UNSIGNED NOT NULL,
    processed_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (consumer_name, event_id),
    KEY idx_consumed_events_processed (processed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
