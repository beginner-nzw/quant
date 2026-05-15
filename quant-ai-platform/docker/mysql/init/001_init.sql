CREATE DATABASE IF NOT EXISTS quant_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quant_ai;

CREATE TABLE IF NOT EXISTS research_task (
                                             id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                             task_id VARCHAR(64) NOT NULL UNIQUE,
    task_type VARCHAR(64) NOT NULL,
    task_title VARCHAR(255) NOT NULL,
    initiator_user_id BIGINT DEFAULT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    target_type VARCHAR(64) NOT NULL,
    target_code VARCHAR(64) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL,
    current_stage VARCHAR(64) NOT NULL,
    source_channel VARCHAR(64) NOT NULL DEFAULT 'WEB',
    trace_id VARCHAR(128) NOT NULL,
    request_payload JSON DEFAULT NULL,
    result_ref VARCHAR(512) DEFAULT NULL,
    error_message VARCHAR(1000) DEFAULT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    start_time DATETIME DEFAULT NULL,
    finish_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_target_code_status (target_code, status),
    INDEX idx_trace_id (trace_id)
    );

CREATE TABLE IF NOT EXISTS research_task_step (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  task_id VARCHAR(64) NOT NULL,
    step_code VARCHAR(64) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    execution_order INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_snapshot JSON DEFAULT NULL,
    output_snapshot JSON DEFAULT NULL,
    error_message VARCHAR(1000) DEFAULT NULL,
    start_time DATETIME DEFAULT NULL,
    finish_time DATETIME DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id)
    );