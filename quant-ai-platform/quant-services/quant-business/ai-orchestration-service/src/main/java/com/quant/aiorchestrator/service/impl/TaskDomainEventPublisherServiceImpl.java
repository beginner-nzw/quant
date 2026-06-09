package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.KafkaMessagePublisherManager;
import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.manager.RiskWarningGeneratedDomainEventPort;
import com.quant.aiorchestrator.manager.StrategySignalGeneratedDomainEventPort;
import com.quant.aiorchestrator.service.ReportGeneratedDomainEventPort;
import com.quant.aiorchestrator.service.TaskDomainEventPublisherService;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;
import com.quant.common.model.message.MessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDomainEventPublisherServiceImpl implements TaskDomainEventPublisherService {

    private final RiskWarningGeneratedDomainEventPort riskWarningGeneratedDomainEventManager;
    private final StrategySignalGeneratedDomainEventPort strategySignalGeneratedDomainEventManager;
    private final ReportGeneratedDomainEventPort reportGeneratedDomainEventManager;
    private final KafkaMessagePublisherManager kafkaMessagePublisherManager;

    public void publishGeneratedEvents(AiTaskResultMessage message, ResearchReportSnapshot report) {
        if (message == null || message.getPayload() == null || report == null) {
            return;
        }
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return;
        }

        Stream.concat(
                        Stream.concat(
                                riskWarningGeneratedDomainEventManager.buildRiskWarningGeneratedEvent(message).stream(),
                                strategySignalGeneratedDomainEventManager.buildStrategySignalGeneratedEvent(message).stream()
                        ),
                        Stream.of(reportGeneratedDomainEventManager.buildReportGeneratedEvent(message, report))
                )
                .forEach(event -> send(event.topicName(), event.key(), event.message()));
    }

    private void send(String topicName, String key, MessageEnvelope message) {
        try {
            kafkaMessagePublisherManager.publish(topicName, key, message);
        } catch (KafkaMessagePublisherManager.KafkaMessagePublishException e) {
            log.warn("publish domain event failed, topic={}, taskId={}, messageId={}",
                    topicName,
                    message == null ? null : message.getTaskId(),
                    message == null ? null : message.getMessageId(),
                    e);
        }
    }
}
