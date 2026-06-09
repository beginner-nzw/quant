package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.audit.HumanReviewReportDecisionPort;
import com.quant.aiorchestrator.audit.HumanReviewRiskDecisionPort;
import com.quant.aiorchestrator.audit.HumanReviewRiskDecisionResult;
import com.quant.aiorchestrator.audit.HumanReviewWorkflowRerunPort;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.service.HumanReviewDecisionProvider;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HumanReviewCommandManager implements HumanReviewDecisionProvider {

    private final HumanReviewReportDecisionPort reportDecisionPort;
    private final HumanReviewRiskDecisionPort riskDecisionPort;
    private final HumanReviewWorkflowRerunPort workflowRerunPort;
    private final HumanReviewRecordWriteManager humanReviewRecordWriteManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String decide(String queueId, HumanReviewDecisionDTO dto) {
        QueueRef ref = parseQueueId(queueId);
        HumanReviewDecisionDTO safeDto = dto == null ? new HumanReviewDecisionDTO() : dto;
        ReportReviewStatusEnum decision = resolveDecision(safeDto.getDecision());
        if (HumanReviewQueueManager.DOMAIN_RISK.equals(ref.domain())) {
            decideRisk(ref.taskId(), safeDto, decision);
        } else {
            reportDecisionPort.decideReport(ref.taskId(), safeDto, decision);
        }
        if (Boolean.TRUE.equals(safeDto.getRerunWorkflow())) {
            workflowRerunPort.rerunWorkflow(ref.taskId(), safeDto);
        }
        return ref.taskId();
    }

    private void decideRisk(String taskId, HumanReviewDecisionDTO dto, ReportReviewStatusEnum decision) {
        String reviewerId = firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer");
        HumanReviewRiskDecisionResult result = riskDecisionPort.decideRisk(
                taskId,
                reviewerId,
                dto.getReviewComment(),
                decision
        );
        humanReviewRecordWriteManager.insertReviewRecord(
                taskId,
                "RISK_WARNING",
                result.warningId(),
                result.reviewerId(),
                SecurityUtils.currentUserRole(),
                decision.name(),
                dto.getReviewComment(),
                toJson(result.beforeSnapshot()),
                toJson(result.afterSnapshot()),
                result.traceId(),
                result.tenantId()
        );
        evictTaskCaches(taskId);
    }

    private QueueRef parseQueueId(String queueId) {
        if (!hasText(queueId) || !queueId.contains(":")) {
            throw new BizException("HUMAN_REVIEW_QUEUE_ID_INVALID", "human review queue id invalid");
        }
        String[] parts = queueId.split(":", 2);
        String domain = parts[0].trim().toUpperCase();
        String taskId = parts[1].trim();
        if (!List.of(HumanReviewQueueManager.DOMAIN_REPORT,
                HumanReviewQueueManager.DOMAIN_RISK,
                HumanReviewQueueManager.DOMAIN_COMPLIANCE).contains(domain) || !hasText(taskId)) {
            throw new BizException("HUMAN_REVIEW_QUEUE_ID_INVALID", "human review queue id invalid");
        }
        return new QueueRef(domain, taskId);
    }

    private ReportReviewStatusEnum resolveDecision(String decision) {
        ReportReviewStatusEnum resolved = ReportReviewStatusEnum.from(decision);
        if (resolved == null) {
            throw new BizException("HUMAN_REVIEW_DECISION_INVALID", "human review decision invalid");
        }
        return resolved;
    }

    private void evictTaskCaches(String taskId) {
        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyBuilder.taskResult(taskId));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record QueueRef(String domain, String taskId) {
    }
}
