package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.AiTaskDeadLetterMessage;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.UUID;

public interface AiTaskDeadLetterPublisherService {
        public void publishInvalidMessage(String sourceTopic,
                                          String consumerGroup,
                                          String consumerService,
                                          MessageEnvelope originalMessage,
                                          String rawMessage,
                                          String errorCode,
                                          String errorMessage);
}
