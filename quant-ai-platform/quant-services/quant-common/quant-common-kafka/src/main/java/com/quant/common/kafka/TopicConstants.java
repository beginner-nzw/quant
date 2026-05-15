package com.quant.common.kafka;

import com.quant.common.messaging.KafkaTopicConstants;

@Deprecated
public final class TopicConstants {

    private TopicConstants() {
    }

    public static final String MARKET_NEWS_RAW = KafkaTopicConstants.MARKET_NEWS_RAW;
    public static final String MARKET_ANNOUNCEMENT_RAW = KafkaTopicConstants.MARKET_ANNOUNCEMENT_RAW;
    public static final String MARKET_FINANCIAL_REPORT_RAW = KafkaTopicConstants.MARKET_FINANCIAL_REPORT_RAW;
    public static final String MARKET_POLICY_RAW = KafkaTopicConstants.MARKET_POLICY_RAW;
    public static final String MARKET_EVENT_STANDARDIZED = KafkaTopicConstants.MARKET_EVENT_STANDARDIZED;

    public static final String AI_TASK_DISPATCH = KafkaTopicConstants.AI_TASK_DISPATCH;
    public static final String AI_TASK_STATUS = KafkaTopicConstants.AI_TASK_STATUS;
    public static final String AI_TASK_RESULT = KafkaTopicConstants.AI_TASK_RESULT;
    public static final String AI_TASK_AUDIT = KafkaTopicConstants.AI_TASK_AUDIT;
    public static final String AI_TASK_DEADLETTER = KafkaTopicConstants.AI_TASK_DEADLETTER;

    public static final String RISK_WARNING_GENERATED = KafkaTopicConstants.RISK_WARNING_GENERATED;
    public static final String STRATEGY_SIGNAL_GENERATED = KafkaTopicConstants.STRATEGY_SIGNAL_GENERATED;
    public static final String REPORT_GENERATED = KafkaTopicConstants.REPORT_GENERATED;
    public static final String NOTIFICATION_DISPATCH = KafkaTopicConstants.NOTIFICATION_DISPATCH;

    public static final String MARKET_EVENT_DEADLETTER = KafkaTopicConstants.MARKET_EVENT_DEADLETTER;
    public static final String BUSINESS_EVENT_DEADLETTER = KafkaTopicConstants.BUSINESS_EVENT_DEADLETTER;
}
