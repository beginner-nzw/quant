package com.quant.common.model.enums;

public enum MarketIntelligenceTypeEnum {
    RISK_ALERT,
    STRATEGY_SIGNAL,
    REPORT_INSIGHT;

    public static MarketIntelligenceTypeEnum from(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        for (MarketIntelligenceTypeEnum value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        return null;
    }
}