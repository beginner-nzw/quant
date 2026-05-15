USE quant_ai;

CREATE TABLE IF NOT EXISTS research_report_section (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    section_id VARCHAR(64) NOT NULL UNIQUE,
    report_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    section_code VARCHAR(64) NOT NULL,
    section_title VARCHAR(128) NOT NULL,
    section_order INT NOT NULL DEFAULT 0,
    section_content TEXT,
    section_items JSON DEFAULT NULL,
    revised_content TEXT,
    revised_items JSON DEFAULT NULL,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(64) DEFAULT NULL,
    reviewed_at DATETIME DEFAULT NULL,
    review_comment VARCHAR(1000) DEFAULT NULL,
    confidence_score DECIMAL(5,4) DEFAULT NULL,
    trace_id VARCHAR(128) DEFAULT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_report_id (report_id),
    INDEX idx_task_id (task_id),
    INDEX idx_report_order (report_id, section_order),
    INDEX idx_section_code (section_code)
);
