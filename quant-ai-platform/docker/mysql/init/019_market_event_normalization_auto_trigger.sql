USE quant_ai;

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_missing $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_value, '` ADD COLUMN `', column_name_value, '` ', column_definition_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS add_index_if_missing $$
CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX `', index_name_value, '` ON `', table_name_value, '` ', index_definition_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DROP PROCEDURE IF EXISTS add_unique_index_if_missing $$
CREATE PROCEDURE add_unique_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN index_definition_value VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @ddl = CONCAT('CREATE UNIQUE INDEX `', index_name_value, '` ON `', table_name_value, '` ', index_definition_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$

DELIMITER ;

CALL add_column_if_missing('market_event', 'normalized_fingerprint', 'VARCHAR(128) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'provenance_type', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'provenance_ref', 'VARCHAR(512) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'provenance_detail', 'VARCHAR(1000) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'confidence_score', 'DECIMAL(5,4) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'auto_trigger_reason', 'VARCHAR(255) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'auto_trigger_source', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'auto_trigger_failure_code', 'VARCHAR(128) DEFAULT NULL');
CALL add_column_if_missing('market_event', 'auto_trigger_retry_count', 'INT NOT NULL DEFAULT 0');

CALL add_unique_index_if_missing('market_event', 'uk_market_event_fingerprint', '(normalized_fingerprint)');
CALL add_index_if_missing('market_event', 'idx_market_event_auto_trigger_ops', '(auto_trigger_status, auto_trigger_rule_code, auto_trigger_attempted_at)');
CALL add_index_if_missing('market_event', 'idx_market_event_provenance', '(provenance_type, source_channel)');

CREATE TABLE IF NOT EXISTS market_event_auto_trigger_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    attempt_id VARCHAR(64) NOT NULL UNIQUE,
    event_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(64) DEFAULT NULL,
    status VARCHAR(32) NOT NULL,
    task_id VARCHAR(64) DEFAULT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    source VARCHAR(64) DEFAULT NULL,
    failure_code VARCHAR(128) DEFAULT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    message VARCHAR(1000) DEFAULT NULL,
    trace_id VARCHAR(128) DEFAULT NULL,
    tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_event_attempted_at (event_id, attempted_at),
    INDEX idx_status_attempted_at (status, attempted_at),
    INDEX idx_failure_code (failure_code)
);

CALL add_unique_index_if_missing('research_task', 'uk_research_task_market_event_source', '(source_domain, source_event_id)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
DROP PROCEDURE IF EXISTS add_unique_index_if_missing;
