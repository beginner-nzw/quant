package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.audit.HumanReviewRiskDecisionResult;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RiskHumanReviewDecisionManager {

    private final RiskWarningMapper riskWarningMapper;

    public HumanReviewRiskDecisionResult decideLatestRisk(String taskId,
                                                          String reviewerId,
                                                          String reviewComment,
                                                          ReportReviewStatusEnum decision) {
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

        Map<String, Object> beforeSnapshot = buildRiskSnapshot(risk);
        risk.setReviewStatus(decision.name());
        risk.setReviewerId(reviewerId);
        risk.setReviewTime(LocalDateTime.now());
        if (hasText(reviewComment)) {
            risk.setSuggestAction(reviewComment);
        }
        riskWarningMapper.updateById(risk);

        return new HumanReviewRiskDecisionResult(
                risk.getWarningId(),
                risk.getReviewerId(),
                beforeSnapshot,
                buildRiskSnapshot(risk),
                risk.getTraceId(),
                risk.getTenantId()
        );
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
