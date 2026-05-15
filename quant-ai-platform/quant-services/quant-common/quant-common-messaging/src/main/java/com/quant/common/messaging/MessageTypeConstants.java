package com.quant.common.messaging;

public final class MessageTypeConstants {

    public static final String AI_TASK_DISPATCH = "AI_TASK_DISPATCH";
    public static final String AI_TASK_STATUS = "AI_TASK_STATUS";
    public static final String AI_TASK_RESULT = "AI_TASK_RESULT";
    public static final String AI_TASK_AUDIT = "AI_TASK_AUDIT";
    public static final String AI_TASK_DEADLETTER = "AI_TASK_DEADLETTER";
    public static final String MARKET_EVENT_STANDARDIZED = "MARKET_EVENT_STANDARDIZED";
    public static final String RISK_WARNING_GENERATED = "RISK_WARNING_GENERATED";
    public static final String STRATEGY_SIGNAL_GENERATED = "STRATEGY_SIGNAL_GENERATED";
    public static final String REPORT_GENERATED = "REPORT_GENERATED";

    private MessageTypeConstants() {
    }
}
