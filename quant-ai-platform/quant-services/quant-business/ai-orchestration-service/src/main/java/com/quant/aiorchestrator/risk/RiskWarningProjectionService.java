package com.quant.aiorchestrator.risk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.approvedPayloadValue;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.approvedTextList;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.defaultValue;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.hasApprovedPayload;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.limit;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.mergeTextList;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.normalizeTextList;
import static com.quant.aiorchestrator.risk.RiskStrategyProjectionPayloadSupport.resolveFirstText;

@Service
@RequiredArgsConstructor
public class RiskWarningProjectionService {

    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;

    public void project(AiTaskResultMessage message) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        String taskId = message.getTaskId();
        String warningId = buildRiskWarningId(taskId);
        if (!hasApprovedPayload(payload)) {
            markDeleted(warningId);
            return;
        }
        List<String> riskWarnings = approvedTextList(payload, "riskWarnings");
        List<String> riskPoints = normalizeTextList(approvedPayloadValue(payload, "riskPoints"));
        List<String> reasons = mergeTextList(riskWarnings, riskPoints);
        boolean needHumanReview = Boolean.TRUE.equals(approvedPayloadValue(payload, "needHumanReview"));

        if (!needHumanReview && reasons.isEmpty()) {
            markDeleted(warningId);
            return;
        }

        RiskWarningDO entity = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getWarningId, warningId)
                        .last("limit 1")
        );
        boolean isNew = entity == null;
        if (entity == null) {
            entity = new RiskWarningDO();
            entity.setWarningId(warningId);
        }

        entity.setTaskId(taskId);
        entity.setWarningType(limit(org.springframework.util.StringUtils.hasText(payload.getSourceEventId()) ? "EVENT" : "REPORT", 64));
        entity.setWarningLevel(resolveRiskLevel(reasons.size(), needHumanReview).name());
        entity.setEntityType(limit(defaultValue(payload.getTargetType(), "STOCK"), 32));
        entity.setEntityCode(limit(defaultValue(payload.getTargetCode(), "UNKNOWN"), 64));
        entity.setEntityName(limit(payload.getTargetName(), 255));
        entity.setTriggerSource(limit(defaultValue(payload.getSourceDomain(), "AI_TASK_RESULT"), 64));
        entity.setTriggerEventId(limit(payload.getSourceEventId(), 64));
        entity.setWarningSummary(resolveFirstText(reasons, null));
        entity.setWarningReason(String.join("\n", reasons));
        entity.setSuggestAction(needHumanReview ? "NEED_HUMAN_REVIEW" : "TRACK_AND_REVIEW");
        entity.setConfidenceScore(toBigDecimal(toDouble(approvedPayloadValue(payload, "confidenceScore"))));
        entity.setStatus("ACTIVE");
        entity.setReviewStatus(ReportReviewStatusEnum.PENDING.name());
        entity.setTraceId(limit(message.getTraceId(), 128));
        entity.setTenantId(limit(defaultValue(message.getTenantId(), "default"), 64));
        entity.setDeleted(0);

        if (isNew) {
            riskWarningMapper.insert(entity);
        } else {
            riskWarningMapper.updateById(entity);
        }

        riskWarningDetailMapper.delete(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getWarningId, warningId)
        );
        for (String reason : reasons) {
            RiskWarningDetailDO detail = new RiskWarningDetailDO();
            detail.setDetailId(UUID.randomUUID().toString());
            detail.setWarningId(warningId);
            detail.setIndicatorCode(limit("AI_RISK_REASON", 64));
            detail.setIndicatorName(limit("AI risk reason", 128));
            detail.setIndicatorValue(limit(reason, 128));
            detail.setComparisonResult(limit("TRIGGERED", 64));
            detail.setDetailDesc(reason);
            detail.setDeleted(0);
            riskWarningDetailMapper.insert(detail);
        }
    }

    private void markDeleted(String warningId) {
        RiskWarningDO entity = riskWarningMapper.selectOne(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getWarningId, warningId)
                        .last("limit 1")
        );
        if (entity == null) {
            return;
        }
        entity.setDeleted(1);
        riskWarningMapper.updateById(entity);
        riskWarningDetailMapper.delete(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getWarningId, warningId)
        );
    }

    private RiskLevelEnum resolveRiskLevel(int riskCount, boolean needHumanReview) {
        if (needHumanReview || riskCount >= 3) {
            return RiskLevelEnum.HIGH;
        }
        if (riskCount > 0) {
            return RiskLevelEnum.MEDIUM;
        }
        return RiskLevelEnum.LOW;
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, value)));
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && org.springframework.util.StringUtils.hasText(text)) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String buildRiskWarningId(String taskId) {
        return "risk-" + taskId;
    }
}
