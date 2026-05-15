CREATE TABLE IF NOT EXISTS research_task_retry_log (
                                                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                       task_id VARCHAR(64) NOT NULL,
    retry_no INT NOT NULL,
    retry_reason VARCHAR(500) DEFAULT NULL,
    retry_source VARCHAR(64) NOT NULL,
    retry_status VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id)
    );