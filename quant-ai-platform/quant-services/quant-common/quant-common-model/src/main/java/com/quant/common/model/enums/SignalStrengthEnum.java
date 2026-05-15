package com.quant.common.model.enums;

public enum SignalStrengthEnum {
    STRONG,
    MEDIUM,
    WEAK;

    public static SignalStrengthEnum from(String strength) {
        if (strength == null || strength.isBlank()) {
            return null;
        }
        for (SignalStrengthEnum value : values()) {
            if (value.name().equalsIgnoreCase(strength)) {
                return value;
            }
        }
        return null;
    }
}
