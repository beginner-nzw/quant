CREATE TABLE IF NOT EXISTS research_report_review_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    review_log_id VARCHAR(64) NOT NULL UNIQUE,
    report_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    reviewed_by VARCHAR(64) DEFAULT NULL,
    review_comment VARCHAR(1000) DEFAULT NULL,
    revised_summary TEXT,
    revised_highlights JSON DEFAULT NULL,
    revised_risk_points JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id),
    INDEX idx_report_id (report_id)
);