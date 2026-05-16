package com.quant.common.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaTopicConstantsContractTests {

    @Test
    void aiTaskTopicNamesMatchEngineConfigurationContract() {
        assertEquals("ai.task.dispatch", KafkaTopicConstants.AI_TASK_DISPATCH);
        assertEquals("ai.task.status", KafkaTopicConstants.AI_TASK_STATUS);
        assertEquals("ai.task.result", KafkaTopicConstants.AI_TASK_RESULT);
        assertEquals("ai.task.audit", KafkaTopicConstants.AI_TASK_AUDIT);
    }
}
