CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    username VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_user_id (user_id),
    UNIQUE KEY uk_sys_user_username_deleted (username, deleted),
    KEY idx_sys_user_status_deleted (status, deleted)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role_user_role_deleted (user_id, role_code, deleted),
    KEY idx_sys_user_role_user_deleted (user_id, deleted)
);

CREATE TABLE IF NOT EXISTS risk_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    subscription_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_code VARCHAR(128) NOT NULL,
    subscription_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_subscription_id (subscription_id),
    KEY idx_risk_subscription_user_status (user_id, status, deleted),
    KEY idx_risk_subscription_target (target_type, target_code, subscription_type, status, deleted)
);

CREATE TABLE IF NOT EXISTS notification_dispatch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    dispatch_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_dispatch_id (notification_id),
    KEY idx_notification_dispatch_user_created (user_id, deleted, created_at),
    KEY idx_notification_dispatch_status (dispatch_status, deleted)
);
