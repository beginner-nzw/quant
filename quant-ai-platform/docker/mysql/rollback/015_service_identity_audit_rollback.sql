USE quant_ai;

DROP INDEX IF EXISTS idx_service_principal ON task_message_log;

ALTER TABLE task_message_log
    DROP COLUMN IF EXISTS service_principal,
    DROP COLUMN IF EXISTS role_source,
    DROP COLUMN IF EXISTS identity_source;

ALTER TABLE audit_record
    DROP COLUMN IF EXISTS delegated_actor_id,
    DROP COLUMN IF EXISTS original_actor_id,
    DROP COLUMN IF EXISTS service_principal,
    DROP COLUMN IF EXISTS role_source,
    DROP COLUMN IF EXISTS identity_source;
