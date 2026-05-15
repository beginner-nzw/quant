package com.quant.task.service;

import com.quant.common.messaging.MessageConsumeStatusConstants;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.task.domain.entity.TaskMessageLogDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.TaskMessageLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

public interface TaskMessageLogService {
        public void recordProduced(String topicName, MessageEnvelope message);

        public void recordFailed(String topicName, MessageEnvelope message, String errorMessage);

        public void recordProduced(TaskOutboxMessageDO outbox);

        public void recordFailed(TaskOutboxMessageDO outbox, String errorMessage);
}
