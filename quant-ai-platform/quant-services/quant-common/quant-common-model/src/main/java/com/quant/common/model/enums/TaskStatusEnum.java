package com.quant.common.model.enums;

public enum TaskStatusEnum {
    INIT(false),
    DISPATCHED(false),
    RUNNING(false),
    SUCCESS(true),
    FAILED(true),
    CANCELLED(true);

    private final boolean finalState;

    TaskStatusEnum(boolean finalState) {
        this.finalState = finalState;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public static TaskStatusEnum from(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (TaskStatusEnum value : values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }
}
