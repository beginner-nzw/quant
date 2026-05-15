package com.quant.common.model.enums;

public enum RiskLevelEnum {
    HIGH,
    MEDIUM,
    LOW;

    public static RiskLevelEnum from(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        for (RiskLevelEnum value : values()) {
            if (value.name().equalsIgnoreCase(level)) {
                return value;
            }
        }
        return null;
    }
}
