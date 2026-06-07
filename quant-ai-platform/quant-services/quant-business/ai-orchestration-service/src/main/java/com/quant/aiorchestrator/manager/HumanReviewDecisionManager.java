package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HumanReviewDecisionManager {

    private final RiskWarningMapper riskWarningMapper;
    private final HumanReviewRecordMapper humanReviewRecordMapper;
    private final TaskReportService taskReportService;
    private final TaskControlService taskControlService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void decideReport(String taskId, HumanReviewDecisionDTO dto, ReportReviewStatusEnum decision) {
        TaskReportReviewDTO reviewDTO = new TaskReportReviewDTO();
        reviewDTO.setReviewStatus(decision.name());
        reviewDTO.setReviewedBy(firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer"));
        reviewDTO.setReviewComment(dto.getReviewComment());
        reviewDTO.setRevisedSummary(dto.getRevisedSummary());
        reviewDTO.setRevisedHighlights(dto.getRevisedHighlights());
        reviewDTO.setRevisedRiskPoints(dto.getRevisedRiskPoints());
        taskReportService.reviewReport(taskId, reviewDTO);
    }

    public void decideRisk(String taskId, HumanReviewDecisionDTO dto, ReportReviewStatusEnum decision) {
        RiskWarningDO risk = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getTaskId, taskId)
                        .eq(RiskWarningDO::getDeleted, 0)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
                        .last("limit 1")
        );
        if (risk == null) {
            throw new BizException("RISK_WARNING_NOT_FOUND", "risk warning not found");
        }

        String beforeSnapshot = toJson(buildRiskSnapshot(risk));
        risk.setReviewStatus(decision.name());
        risk.setReviewerId(firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer"));
        risk.setReviewTime(LocalDateTime.now());
        if (hasText(dto.getReviewComment())) {
            risk.setSuggestAction(dto.getReviewComment());
        }
        riskWarningMapper.updateById(risk);

        insertReviewRecord(
                taskId,
                "RISK_WARNING",
                risk.getWarningId(),
                risk.getReviewerId(),
                decision.name(),
                dto.getReviewComment(),
                beforeSnapshot,
                toJson(buildRiskSnapshot(risk)),
                risk.getTraceId(),
                risk.getTenantId()
        );
        evictTaskCaches(taskId);
    }

    public void rerunWorkflow(String taskId, HumanReviewDecisionDTO dto) {
        TaskWorkflowControlDTO controlDTO = new TaskWorkflowControlDTO();
        controlDTO.setOperatorId(firstText(dto.getReviewedBy(), SecurityUtils.currentUserId(), "human-reviewer"));
        controlDTO.setReason(firstText(dto.getReviewComment(), "human review requested rerun"));
        controlDTO.setNodeName(firstText(dto.getRerunNodeName(), "report_generation_agent"));
        taskControlService.rerunNode(taskId, controlDTO);
    }

    private void insertReviewRecord(String taskId,
                                    String objectType,
                                    String objectId,
                                    String reviewerId,
                                    String decision,
                                    String comment,
                                    String beforeSnapshot,
                                    String afterSnapshot,
                                    String traceId,
                                    String tenantId) {
        HumanReviewRecordDO record = new HumanReviewRecordDO();
        record.setReviewId(UUID.randomUUID().toString());
        record.setTaskId(taskId);
        record.setRelatedObjectType(objectType);
        record.setRelatedObjectId(objectId);
        record.setReviewerId(reviewerId);
        record.setReviewerRole(SecurityUtils.currentUserRole());
        record.setReviewResult(decision);
        record.setReviewComment(comment);
        record.setBeforeSnapshot(beforeSnapshot);
        record.setAfterSnapshot(afterSnapshot);
        record.setTraceId(traceId);
        record.setTenantId(firstText(tenantId, "default"));
        record.setDeleted(0);
        humanReviewRecordMapper.insert(record);
    }

    private void evictTaskCaches(String taskId) {
        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyBuilder.taskResult(taskId));
    }

    private Map<String, Object> buildRiskSnapshot(RiskWarningDO risk) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("warningId", risk.getWarningId());
        snapshot.put("taskId", risk.getTaskId());
        snapshot.put("reviewStatus", risk.getReviewStatus());
        snapshot.put("reviewerId", risk.getReviewerId());
        snapshot.put("reviewTime", risk.getReviewTime() == null ? null : risk.getReviewTime().toString());
        snapshot.put("warningLevel", risk.getWarningLevel());
        snapshot.put("warningSummary", risk.getWarningSummary());
        snapshot.put("warningReason", risk.getWarningReason());
        snapshot.put("suggestAction", risk.getSuggestAction());
        return snapshot;
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
}
