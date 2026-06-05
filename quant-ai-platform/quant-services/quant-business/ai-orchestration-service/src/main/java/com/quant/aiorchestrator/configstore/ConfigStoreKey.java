package com.quant.aiorchestrator.configstore;

public enum ConfigStoreKey {
    AGENT("agent-configs", "agent-configs.json", "AGENT_CONFIG"),
    WORKFLOW("workflow-configs", "workflow-configs.json", "WORKFLOW_CONFIG"),
    MODEL_STRATEGY("model-strategies", "model-strategies.json", "MODEL_STRATEGY"),
    EVENT_SOURCE("event-source-configs", "event-source-configs.json", "EVENT_SOURCE_CONFIG"),
    EVENT_AUTO_TRIGGER("event-auto-trigger-configs", "event-auto-trigger-configs.json", "EVENT_AUTO_TRIGGER_RULE"),
    ROLE_ACCESS("role-access-configs", "role-access-configs.json", "ROLE_ACCESS_CONFIG"),
    PROMPT_TEMPLATE("prompt-templates", "prompt-templates.json", "PROMPT_TEMPLATE");

    private final String storeCode;
    private final String legacyFileName;
    private final String auditType;

    ConfigStoreKey(String storeCode, String legacyFileName, String auditType) {
        this.storeCode = storeCode;
        this.legacyFileName = legacyFileName;
        this.auditType = auditType;
    }

    public String storeCode() {
        return storeCode;
    }

    public String legacyFileName() {
        return legacyFileName;
    }

    public String auditType() {
        return auditType;
    }

    public static ConfigStoreKey fromStoreCode(String storeCode) {
        if (storeCode == null || storeCode.isBlank()) {
            throw new IllegalArgumentException("storeCode cannot be empty");
        }
        for (ConfigStoreKey key : values()) {
            if (key.storeCode.equalsIgnoreCase(storeCode.trim()) || key.name().equalsIgnoreCase(storeCode.trim())) {
                return key;
            }
        }
        throw new IllegalArgumentException("unsupported config store: " + storeCode);
    }
}
