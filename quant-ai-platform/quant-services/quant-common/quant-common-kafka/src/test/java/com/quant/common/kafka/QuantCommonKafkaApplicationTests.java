package com.quant.common.kafka;

import com.quant.common.messaging.KafkaTopicConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantCommonKafkaApplicationTests {

    @Test
    void exposesKafkaTopicConstants() {
        assertEquals("ai.task.dispatch", KafkaTopicConstants.AI_TASK_DISPATCH);
        assertEquals("market.event.standardized", KafkaTopicConstants.MARKET_EVENT_STANDARDIZED);
        assertEquals("business.event.deadletter", KafkaTopicConstants.BUSINESS_EVENT_DEADLETTER);
    }

}
