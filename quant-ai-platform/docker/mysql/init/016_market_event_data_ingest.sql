USE quant_ai;

CREATE TABLE IF NOT EXISTS market_event_ingest_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ingest_run_id VARCHAR(64) NOT NULL,
    source_code VARCHAR(128) NOT NULL,
    source_name VARCHAR(255) DEFAULT NULL,
    source_category VARCHAR(64) DEFAULT NULL,
    source_channel VARCHAR(64) DEFAULT NULL,
    ingest_mode VARCHAR(64) DEFAULT NULL,
    request_target VARCHAR(128) DEFAULT NULL,
    fetch_status VARCHAR(32) NOT NULL,
    raw_payload_ref VARCHAR(1024) DEFAULT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 0,
    deadlettered TINYINT NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    duplicate_count INT NOT NULL DEFAULT 0,
    auto_triggered_count INT NOT NULL DEFAULT 0,
    error_code VARCHAR(128) DEFAULT NULL,
    error_message VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_ingest_run_id (ingest_run_id),
    INDEX idx_source_status_time (source_code, fetch_status, created_at),
    INDEX idx_source_deadletter_time (source_code, deadlettered, created_at)
);
