CREATE TABLE IF NOT EXISTS ai_workflow_instance (
                                                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                    workflow_instance_id VARCHAR(64) NOT NULL UNIQUE,
    task_id VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(64) NOT NULL,
    workflow_version VARCHAR(32) NOT NULL,
    entry_agent VARCHAR(64) NOT NULL,
    current_node VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    graph_snapshot JSON DEFAULT NULL,
    start_time DATETIME DEFAULT NULL,
    finish_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id)
    );

CREATE TABLE IF NOT EXISTS ai_agent_execution (
                                                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                  execution_id VARCHAR(64) NOT NULL UNIQUE,
    workflow_instance_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    agent_name VARCHAR(128) NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    input_ref VARCHAR(512) DEFAULT NULL,
    output_ref VARCHAR(512) DEFAULT NULL,
    decision_ref VARCHAR(512) DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    confidence_score DECIMAL(5,4) DEFAULT NULL,
    need_human_review TINYINT NOT NULL DEFAULT 0,
    start_time DATETIME DEFAULT NULL,
    finish_time DATETIME DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id),
    INDEX idx_workflow_instance_id (workflow_instance_id)
    );

CREATE TABLE IF NOT EXISTS audit_record (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            audit_id VARCHAR(64) NOT NULL UNIQUE,
    task_id VARCHAR(64) NOT NULL,
    audit_type VARCHAR(64) NOT NULL,
    audit_stage VARCHAR(64) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) DEFAULT NULL,
    action_code VARCHAR(64) NOT NULL,
    action_desc VARCHAR(500) DEFAULT NULL,
    result_status VARCHAR(32) NOT NULL,
    remark LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_task_id (task_id)
    );