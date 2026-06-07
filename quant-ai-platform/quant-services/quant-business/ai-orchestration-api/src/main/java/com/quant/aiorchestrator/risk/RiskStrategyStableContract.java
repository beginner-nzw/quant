package com.quant.aiorchestrator.risk;

import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;

public final class RiskStrategyStableContract {

    public static final String RISK_WARNING_TABLE = "risk_warning";
    public static final String RISK_WARNING_DETAIL_TABLE = "risk_warning_detail";
    public static final String STRATEGY_SIGNAL_TABLE = "strategy_signal";
    public static final String STRATEGY_SIGNAL_FACTOR_TABLE = "strategy_signal_factor";

    public static final String RISK_WARNING_GENERATED_TOPIC = KafkaTopicConstants.RISK_WARNING_GENERATED;
    public static final String STRATEGY_SIGNAL_GENERATED_TOPIC = KafkaTopicConstants.STRATEGY_SIGNAL_GENERATED;
    public static final String RISK_WARNING_GENERATED_MESSAGE_TYPE = MessageTypeConstants.RISK_WARNING_GENERATED;
    public static final String STRATEGY_SIGNAL_GENERATED_MESSAGE_TYPE = MessageTypeConstants.STRATEGY_SIGNAL_GENERATED;

    public static final String RISK_WARNING_LIST_PATH = "/api/tasks/risk-warnings";
    public static final String RISK_WARNING_STATS_PATH = "/api/tasks/risk-warning-stats";
    public static final String STRATEGY_SIGNAL_LIST_PATH = "/api/tasks/strategy-signals";
    public static final String STRATEGY_SIGNAL_STATS_PATH = "/api/tasks/strategy-signal-stats";
    public static final String STRATEGY_SIGNAL_FACTOR_PATH = "/api/tasks/strategy-signals/{signalId}/factors";
    public static final String STRATEGY_SIGNAL_COMMAND_PATH = "/api/tasks/strategy-signals";
    public static final String STRATEGY_SIGNAL_STATUS_PATH = "/api/tasks/strategy-signals/{signalId}/status";

    private RiskStrategyStableContract() {
    }
}
