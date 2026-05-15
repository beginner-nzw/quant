CREATE TABLE IF NOT EXISTS research_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_id VARCHAR(64) NOT NULL UNIQUE,
    task_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    final_status VARCHAR(32) NOT NULL,
    summary TEXT,
    confidence_score DECIMAL(5,4) DEFAULT NULL,
    need_human_review TINYINT NOT NULL DEFAULT 0,
    report_type VARCHAR(64) DEFAULT NULL,
    highlights JSON DEFAULT NULL,
    risk_points JSON DEFAULT NULL,
    risk_warnings JSON DEFAULT NULL,
    result_ref VARCHAR(512) DEFAULT NULL,
    raw_payload JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_task_id (task_id),
    INDEX idx_final_status (final_status)
);

ALTER TABLE research_report
ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
ADD COLUMN reviewed_by VARCHAR(64) DEFAULT NULL,
ADD COLUMN reviewed_at DATETIME DEFAULT NULL,
ADD COLUMN revised_summary TEXT,
ADD COLUMN revised_highlights JSON DEFAULT NULL,
ADD COLUMN revised_risk_points JSON DEFAULT NULL,
ADD COLUMN review_comment VARCHAR(1000) DEFAULT NULL;