package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.SimpleMessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface AiTaskInboundMessageSupportService {
        public <T extends MessageEnvelope> T parseOrNull(String rawMessage,
                                                         Class<T> messageClass,
                                                         String sourceTopic,
                                                         String consumerGroup,
                                                         String consumerService);

        public boolean rejectIfInvalidEnvelope(MessageEnvelope message,
                                               boolean payloadPresent,
                                               String rawMessage,
                                               String sourceTopic,
                                               String consumerGroup,
                                               String consumerService);

        public void rejectInvalidMessage(MessageEnvelope message,
                                         String rawMessage,
                                         String sourceTopic,
                                         String consumerGroup,
                                         String consumerService,
                                         String errorCode,
                                         String errorMessage);
}
