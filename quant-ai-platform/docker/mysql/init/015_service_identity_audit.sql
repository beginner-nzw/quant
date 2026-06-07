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

DELIMITER ;

CALL add_column_if_missing('audit_record', 'identity_source', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('audit_record', 'role_source', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('audit_record', 'service_principal', 'VARCHAR(128) DEFAULT NULL');
CALL add_column_if_missing('audit_record', 'original_actor_id', 'VARCHAR(128) DEFAULT NULL');
CALL add_column_if_missing('audit_record', 'delegated_actor_id', 'VARCHAR(128) DEFAULT NULL');

CALL add_column_if_missing('task_message_log', 'identity_source', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('task_message_log', 'role_source', 'VARCHAR(64) DEFAULT NULL');
CALL add_column_if_missing('task_message_log', 'service_principal', 'VARCHAR(128) DEFAULT NULL');

CALL add_index_if_missing('task_message_log', 'idx_service_principal', '(service_principal)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
