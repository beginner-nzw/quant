package com.quant.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import com.quant.task.mapper.TaskOutboxMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TaskOutboxMessageService {
        public TaskOutboxMessageDO enqueueAiTaskDispatch(ResearchTaskDO task);

        public int markSending(TaskOutboxMessageDO outbox);

        public void markSent(TaskOutboxMessageDO outbox);

        public void markFailed(TaskOutboxMessageDO outbox, Throwable throwable);

        public java.util.List<TaskOutboxMessageDO> selectReadyToPublish(int limit, LocalDateTime staleBefore);
}
