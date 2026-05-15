CREATE TABLE IF NOT EXISTS market_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    target_type VARCHAR(32) NOT NULL DEFAULT 'STOCK',
    target_code VARCHAR(32) NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    event_summary TEXT NOT NULL,
    source_channel VARCHAR(64) DEFAULT NULL,
    source_url VARCHAR(255) DEFAULT NULL,
    impact_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    event_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    occurred_at DATETIME NOT NULL,
    created_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_target_code (target_code),
    INDEX idx_event_type (event_type),
    INDEX idx_impact_level (impact_level),
    INDEX idx_event_status (event_status),
    INDEX idx_occurred_at (occurred_at)
);

 ALTER TABLE research_task
      ADD COLUMN source_event_id VARCHAR(64) DEFAULT NULL;
