package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.report.StableReportContract;
import com.quant.aiorchestrator.service.ReportGeneratedDomainEventPort;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.model.message.GeneratedDomainEvent;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.ReportGeneratedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportGeneratedDomainEventManager implements ReportGeneratedDomainEventPort {

    private final ReportEvidenceRefMapper reportEvidenceRefMapper;

    @Override
    public GeneratedDomainEvent buildReportGeneratedEvent(AiTaskResultMessage message, ResearchReportSnapshot report) {
        long evidenceCount = reportEvidenceRefMapper.selectCount(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
        );

        ReportGeneratedMessage outbound = new ReportGeneratedMessage();
        copyEnvelope(
                message,
                outbound,
                StableReportContract.REPORT_GENERATED_TOPIC,
                StableReportContract.REPORT_GENERATED_MESSAGE_TYPE,
                report.getReportId(),
                StableReportContract.REPORT_GENERATED_BIZ_KEY_PREFIX + report.getReportId()
        );

        ReportGeneratedMessage.Payload payload = new ReportGeneratedMessage.Payload();
        payload.setReportId(report.getReportId());
        payload.setReportType(report.getReportType());
        payload.setFinalStatus(report.getFinalStatus());
        payload.setReviewStatus(resolveReviewStatus(report.getReviewStatus()));
        payload.setNeedHumanReview(report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1);
        payload.setEvidenceCount((int) evidenceCount);
        payload.setConfidenceScore(report.getConfidenceScore());
        payload.setResultRef(report.getResultRef());
        payload.setSourceTaskId(message.getPayload().getSourceTaskId());
        payload.setSourceReportId(message.getPayload().getSourceReportId());
        payload.setSourceEventId(message.getPayload().getSourceEventId());
        payload.setSourceDomain(message.getPayload().getSourceDomain());
        outbound.setPayload(payload);
        return new GeneratedDomainEvent(StableReportContract.REPORT_GENERATED_TOPIC, report.getReportId(), outbound);
    }

    private void copyEnvelope(AiTaskResultMessage source,
                              MessageEnvelope target,
                              String topicName,
                              String messageType,
                              String objectId,
                              String bizKey) {
        target.setMessageId(stableMessageId(topicName, source.getTaskId(), objectId, source.getVersion()));
        target.setTraceId(source.getTraceId());
        target.setTaskId(source.getTaskId());
        target.setEventId(source.getPayload().getSourceEventId());
        target.setMessageType(messageType);
        target.setSourceService(StableReportContract.REPORT_GENERATED_SOURCE_SERVICE);
        target.setTargetService(StableReportContract.REPORT_GENERATED_TARGET_SERVICE);
        target.setTenantId(defaultValue(source.getTenantId(), "default"));
        target.setBizKey(bizKey);
        target.setTimestamp(source.getTimestamp() == null ? 0L : source.getTimestamp());
        target.setVersion(defaultValue(source.getVersion(), StableReportContract.REPORT_GENERATED_VERSION));
        target.setRetryCount(source.getRetryCount() == null ? 0 : source.getRetryCount());
    }

    private String stableMessageId(String topicName, String taskId, String objectId, String version) {
        String seed = defaultValue(topicName, "UNKNOWN_TOPIC")
                + "|" + defaultValue(taskId, "UNKNOWN_TASK")
                + "|" + defaultValue(objectId, "UNKNOWN_OBJECT")
                + "|" + defaultValue(version, StableReportContract.REPORT_GENERATED_VERSION);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String resolveReviewStatus(String reviewStatus) {
        return StringUtils.hasText(reviewStatus) ? reviewStatus : ReportReviewStatusEnum.PENDING.name();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
