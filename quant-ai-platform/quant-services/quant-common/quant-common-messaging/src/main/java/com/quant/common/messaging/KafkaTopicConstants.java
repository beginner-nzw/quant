package com.quant.common.messaging;

public final class KafkaTopicConstants {

    public static final String MARKET_NEWS_RAW = "market.news.raw";
    public static final String MARKET_ANNOUNCEMENT_RAW = "market.announcement.raw";
    public static final String MARKET_FINANCIAL_REPORT_RAW = "market.financial-report.raw";
    public static final String MARKET_POLICY_RAW = "market.policy.raw";
    public static final String MARKET_EVENT_STANDARDIZED = "market.event.standardized";

    public static final String AI_TASK_DISPATCH = "ai.task.dispatch";
    public static final String AI_TASK_STATUS = "ai.task.status";
    public static final String AI_TASK_RESULT = "ai.task.result";
    public static final String AI_TASK_AUDIT = "ai.task.audit";
    public static final String AI_TASK_DEADLETTER = "ai.task.deadletter";

    public static final String RISK_WARNING_GENERATED = "risk.warning.generated";
    public static final String STRATEGY_SIGNAL_GENERATED = "strategy.signal.generated";
    public static final String REPORT_GENERATED = "report.generated";
    public static final String NOTIFICATION_DISPATCH = "notification.dispatch";

    public static final String MARKET_EVENT_DEADLETTER = "market.event.deadletter";
    public static final String BUSINESS_EVENT_DEADLETTER = "business.event.deadletter";

    private KafkaTopicConstants() {
    }
}
