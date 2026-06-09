package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.service.TaskMessageLogService;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMessagePublisherManager {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskMessageLogService taskMessageLogService;

    public void publish(String topicName, String key, MessageEnvelope message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topicName, key, messageJson);
            taskMessageLogService.recordProduced(topicName, message);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(topicName, message, e.getMessage());
            throw new KafkaMessagePublishException(e);
        }
    }

    public static class KafkaMessagePublishException extends RuntimeException {

        public KafkaMessagePublishException(Throwable cause) {
            super(cause);
        }
    }
}
