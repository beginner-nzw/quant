package com.quant.common.model.enums;

public enum SignalDirectionEnum {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    public static SignalDirectionEnum from(String direction) {
        if (direction == null || direction.isBlank()) {
            return null;
        }
        for (SignalDirectionEnum value : values()) {
            if (value.name().equalsIgnoreCase(direction)) {
                return value;
            }
        }
        return null;
    }
}
