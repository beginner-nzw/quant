package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.TaskMessageLogDO;
import com.quant.aiorchestrator.mapper.TaskMessageLogMapper;
import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.UUID;

public interface TaskMessageLogService {
        public boolean beginConsume(String topicName, MessageEnvelope message, String consumerService);

        public void recordProduced(String topicName, MessageEnvelope message);

        public void recordFailed(String topicName, MessageEnvelope message, String errorMessage);

        public void recordConsumed(String topicName, MessageEnvelope message, String consumerService);

        public void recordSkipped(String topicName, MessageEnvelope message, String consumerService, String reason);

        public void recordFailed(String topicName, MessageEnvelope message, String consumerService, String errorMessage);
}
