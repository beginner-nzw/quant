package com.quant.task.service;

import com.quant.task.service.impl.TaskOutboxPublisherServiceImpl;

import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.task.domain.entity.TaskOutboxMessageDO;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class TaskOutboxPublisherServiceTests {

    @Test
    void publishPendingOnceShouldSendClaimedOutboxAndMarkSent() {
        TaskOutboxMessageService outboxService = mock(TaskOutboxMessageService.class);
        TaskMessageLogService messageLogService = mock(TaskMessageLogService.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        TaskOutboxPublisherService publisher =
                new TaskOutboxPublisherServiceImpl(outboxService, messageLogService, kafkaTemplate);
        TaskOutboxMessageDO outbox = outbox();

        when(outboxService.selectReadyToPublish(anyInt(), any(LocalDateTime.class))).thenReturn(List.of(outbox));
        when(outboxService.markSending(outbox)).thenReturn(1);
        when(kafkaTemplate.send(eq(KafkaTopicConstants.AI_TASK_DISPATCH), eq("task-1"), eq("{\"ok\":true}")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingOnce(10);

        verify(kafkaTemplate).send(KafkaTopicConstants.AI_TASK_DISPATCH, "task-1", "{\"ok\":true}");
        verify(outboxService).markSent(outbox);
        verify(messageLogService).recordProduced(outbox);
    }

    @Test
    void publishPendingOnceShouldSkipUnclaimedOutbox() {
        TaskOutboxMessageService outboxService = mock(TaskOutboxMessageService.class);
        TaskMessageLogService messageLogService = mock(TaskMessageLogService.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        TaskOutboxPublisherService publisher =
                new TaskOutboxPublisherServiceImpl(outboxService, messageLogService, kafkaTemplate);
        TaskOutboxMessageDO outbox = outbox();

        when(outboxService.selectReadyToPublish(anyInt(), any(LocalDateTime.class))).thenReturn(List.of(outbox));
        when(outboxService.markSending(outbox)).thenReturn(0);

        publisher.publishPendingOnce(10);

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(outboxService, never()).markSent(outbox);
        verify(messageLogService, never()).recordProduced(outbox);
    }

    @Test
    void publishPendingOnceShouldMarkFailedWhenKafkaSendFails() {
        TaskOutboxMessageService outboxService = mock(TaskOutboxMessageService.class);
        TaskMessageLogService messageLogService = mock(TaskMessageLogService.class);
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        TaskOutboxPublisherService publisher =
                new TaskOutboxPublisherServiceImpl(outboxService, messageLogService, kafkaTemplate);
        TaskOutboxMessageDO outbox = outbox();
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));

        when(outboxService.selectReadyToPublish(anyInt(), any(LocalDateTime.class))).thenReturn(List.of(outbox));
        when(outboxService.markSending(outbox)).thenReturn(1);
        when(kafkaTemplate.send(eq(KafkaTopicConstants.AI_TASK_DISPATCH), eq("task-1"), eq("{\"ok\":true}")))
                .thenReturn(failedFuture);

        publisher.publishPendingOnce(10);

        verify(outboxService).markFailed(eq(outbox), any(Throwable.class));
        verify(messageLogService).recordFailed(eq(outbox), eq("kafka down"));
    }

    private TaskOutboxMessageDO outbox() {
        TaskOutboxMessageDO outbox = new TaskOutboxMessageDO();
        outbox.setOutboxId("outbox-1");
        outbox.setMessageId("message-1");
        outbox.setTaskId("task-1");
        outbox.setTopicName(KafkaTopicConstants.AI_TASK_DISPATCH);
        outbox.setMessageKey("task-1");
        outbox.setPayloadJson("{\"ok\":true}");
        outbox.setRetryCount(0);
        return outbox;
    }
}
