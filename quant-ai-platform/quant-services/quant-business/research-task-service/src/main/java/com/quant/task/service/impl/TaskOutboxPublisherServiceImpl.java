package com.quant.task.service.impl;

import com.quant.task.service.TaskOutboxPublisherService;
import com.quant.task.service.*;

import com.quant.task.domain.entity.TaskOutboxMessageDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOutboxPublisherServiceImpl implements TaskOutboxPublisherService {

    private static final int BATCH_SIZE = 50;
    private static final int SENDING_STALE_MINUTES = 5;

    private final TaskOutboxMessageService taskOutboxMessageService;
    private final TaskMessageLogService taskMessageLogService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${task.outbox.publisher.fixed-delay-ms:2000}")
    public void publishPending() {
        publishPendingOnce(BATCH_SIZE);
    }

    public int publishPendingOnce(int limit) {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(SENDING_STALE_MINUTES);
        List<TaskOutboxMessageDO> outboxes = taskOutboxMessageService.selectReadyToPublish(limit, staleBefore);
        int claimed = 0;
        for (TaskOutboxMessageDO outbox : outboxes) {
            if (taskOutboxMessageService.markSending(outbox) <= 0) {
                continue;
            }
            claimed++;
            publish(outbox);
        }
        return claimed;
    }

    private void publish(TaskOutboxMessageDO outbox) {
        try {
            kafkaTemplate.send(outbox.getTopicName(), outbox.getMessageKey(), outbox.getPayloadJson())
                    .whenComplete((result, throwable) -> {
                        if (throwable == null) {
                            taskOutboxMessageService.markSent(outbox);
                            taskMessageLogService.recordProduced(outbox);
                            log.info("publish task outbox success, outboxId={}, taskId={}, topic={}",
                                    outbox.getOutboxId(), outbox.getTaskId(), outbox.getTopicName());
                            return;
                        }
                        taskOutboxMessageService.markFailed(outbox, throwable);
                        taskMessageLogService.recordFailed(outbox, throwable.getMessage());
                        log.warn("publish task outbox failed, outboxId={}, taskId={}, topic={}",
                                outbox.getOutboxId(), outbox.getTaskId(), outbox.getTopicName(), throwable);
                    });
        } catch (Exception e) {
            taskOutboxMessageService.markFailed(outbox, e);
            taskMessageLogService.recordFailed(outbox, e.getMessage());
            log.warn("submit task outbox to kafka failed, outboxId={}, taskId={}, topic={}",
                    outbox.getOutboxId(), outbox.getTaskId(), outbox.getTopicName(), e);
        }
    }
}
