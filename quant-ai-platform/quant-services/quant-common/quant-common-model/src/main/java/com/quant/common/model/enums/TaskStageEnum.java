package com.quant.common.model.enums;

public enum TaskStageEnum {
    INIT(false),
    DISPATCHED(false),
    RETRY_DISPATCHED(false),
    RECEIVED(false),
    PLANNING(false),
    INTENT_UNDERSTANDING(false),
    EVIDENCE_COLLECTION(false),
    FINANCIAL_ANALYSIS(false),
    RISK_REVIEW(false),
    REPORT_GENERATION(false),
    FAILED(true),
    TIMEOUT(true),
    FINISHED(true),
    CANCELLED(true);

    private final boolean terminalStage;

    TaskStageEnum(boolean terminalStage) {
        this.terminalStage = terminalStage;
    }

    public boolean isTerminalStage() {
        return terminalStage;
    }

    public static TaskStageEnum from(String stage) {
        if (stage == null || stage.isBlank()) {
            return null;
        }
        for (TaskStageEnum value : values()) {
            if (value.name().equalsIgnoreCase(stage)) {
                return value;
            }
        }
        return null;
    }

    public static String normalize(String stage) {
        TaskStageEnum taskStage = from(stage);
        return taskStage == null ? stage : taskStage.name();
    }
}

