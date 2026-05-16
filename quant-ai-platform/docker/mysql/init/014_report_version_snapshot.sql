USE quant_ai;

CREATE TABLE IF NOT EXISTS research_report_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_id VARCHAR(64) NOT NULL UNIQUE,
    report_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    version_no INT NOT NULL,
    snapshot_source VARCHAR(32) NOT NULL,
    snapshot_payload JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_report_version (report_id, version_no),
    INDEX idx_task_id (task_id),
    INDEX idx_report_id (report_id)
);
