package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.AiTaskDeadLetterMessageManager;
import com.quant.aiorchestrator.manager.KafkaMessagePublisherManager;
import com.quant.aiorchestrator.service.AiTaskDeadLetterPublisherService;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.model.message.AiTaskDeadLetterMessage;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskDeadLetterPublisherServiceImpl implements AiTaskDeadLetterPublisherService {

    private final AiTaskDeadLetterMessageManager aiTaskDeadLetterMessageManager;
    private final KafkaMessagePublisherManager kafkaMessagePublisherManager;

    public void publishInvalidMessage(String sourceTopic,
                                      String consumerGroup,
                                      String consumerService,
                                      MessageEnvelope originalMessage,
                                      String rawMessage,
                                      String errorCode,
                                      String errorMessage) {
        AiTaskDeadLetterMessage deadLetterMessage = aiTaskDeadLetterMessageManager.buildDeadLetterMessage(
                sourceTopic,
                consumerGroup,
                consumerService,
                originalMessage,
                rawMessage,
                errorCode,
                errorMessage
        );
        try {
            kafkaMessagePublisherManager.publish(
                    KafkaTopicConstants.AI_TASK_DEADLETTER,
                    aiTaskDeadLetterMessageManager.resolveKey(deadLetterMessage),
                    deadLetterMessage
            );
        } catch (KafkaMessagePublisherManager.KafkaMessagePublishException e) {
            log.warn("publish ai task deadletter failed, sourceTopic={}, originalMessageId={}, taskId={}",
                    sourceTopic,
                    originalMessage == null ? null : originalMessage.getMessageId(),
                    originalMessage == null ? null : originalMessage.getTaskId(),
                    e);
        }
    }
}
