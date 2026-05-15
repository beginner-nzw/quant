ALTER TABLE market_event
    ADD COLUMN auto_trigger_rule_code VARCHAR(64) DEFAULT NULL,
    ADD COLUMN auto_trigger_status VARCHAR(32) DEFAULT NULL,
    ADD COLUMN auto_trigger_task_id VARCHAR(64) DEFAULT NULL,
    ADD COLUMN auto_trigger_message VARCHAR(255) DEFAULT NULL,
    ADD COLUMN auto_trigger_attempted_at DATETIME DEFAULT NULL;
