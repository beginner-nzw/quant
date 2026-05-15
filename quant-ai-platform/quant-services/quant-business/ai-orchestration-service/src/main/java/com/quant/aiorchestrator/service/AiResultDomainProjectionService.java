package com.quant.aiorchestrator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalDO;
import com.quant.aiorchestrator.domain.entity.StrategySignalFactorDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.RiskLevelEnum;
import com.quant.common.model.enums.SignalDirectionEnum;
import com.quant.common.model.enums.SignalStrengthEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import com.quant.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResultDomainProjectionService {

    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final StrategySignalMapper strategySignalMapper;
    private final StrategySignalFactorMapper strategySignalFactorMapper;
    private final ReportEvidenceRefMapper reportEvidenceRefMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void project(AiTaskResultMessage message, ResearchReportDO report) {
        if (message == null || message.getPayload() == null || report == null) {
            return;
        }
        if (!TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return;
        }

        saveRiskWarning(message);
        saveStrategySignal(message, report);
        saveReportEvidenceRefs(message, report);
        saveReportSections(message, report);
    }

    private void saveRiskWarning(AiTaskResultMessage message) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        String taskId = message.getTaskId();
        String warningId = buildRiskWarningId(taskId);
        List<String> riskWarnings = normalizeTextList(payload.getRiskWarnings());
        List<String> riskPoints = normalizeTextList(reportMetaValue(payload, "riskPoints"));
        List<String> reasons = mergeTextList(riskWarnings, riskPoints);
        boolean needHumanReview = Boolean.TRUE.equals(payload.getNeedHumanReview());

        if (!needHumanReview && reasons.isEmpty()) {
            markRiskWarningDeleted(warningId);
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
        entity.setWarningType(limit(StringUtils.hasText(payload.getSourceEventId()) ? "EVENT" : "REPORT", 64));
        entity.setWarningLevel(resolveRiskLevel(reasons.size(), needHumanReview).name());
        entity.setEntityType(limit(defaultValue(payload.getTargetType(), "STOCK"), 32));
        entity.setEntityCode(limit(defaultValue(payload.getTargetCode(), "UNKNOWN"), 64));
        entity.setEntityName(limit(payload.getTargetName(), 255));
        entity.setTriggerSource(limit(defaultValue(payload.getSourceDomain(), "AI_TASK_RESULT"), 64));
        entity.setTriggerEventId(limit(payload.getSourceEventId(), 64));
        entity.setWarningSummary(resolveFirstText(reasons, payload.getSummary()));
        entity.setWarningReason(String.join("\n", reasons));
        entity.setSuggestAction(needHumanReview ? "NEED_HUMAN_REVIEW" : "TRACK_AND_REVIEW");
        entity.setConfidenceScore(toBigDecimal(payload.getConfidenceScore()));
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

    private void saveStrategySignal(AiTaskResultMessage message, ResearchReportDO report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        String taskId = message.getTaskId();
        String signalId = buildStrategySignalId(taskId);
        String summary = normalizeText(payload.getSummary());
        Double confidenceScore = payload.getConfidenceScore();
        List<String> reasons = mergeTextList(
                normalizeTextList(payload.getRiskWarnings()),
                normalizeTextList(reportMetaValue(payload, "riskPoints"))
        );
        boolean needHumanReview = Boolean.TRUE.equals(payload.getNeedHumanReview());

        if (!StringUtils.hasText(summary) && confidenceScore == null && reasons.isEmpty()) {
            markStrategySignalDeleted(signalId);
            return;
        }

        StrategySignalDO entity = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId)
                        .last("limit 1")
        );
        boolean isNew = entity == null;
        if (entity == null) {
            entity = new StrategySignalDO();
            entity.setSignalId(signalId);
        }

        int signalScore = calculateSignalScore(confidenceScore, reasons.size(), needHumanReview);
        SignalDirectionEnum direction = resolveSignalDirection(confidenceScore, reasons.size(), needHumanReview);

        entity.setTaskId(taskId);
        entity.setSignalType(limit(resolveReportType(report, payload), 64));
        entity.setEntityCode(limit(defaultValue(payload.getTargetCode(), "UNKNOWN"), 64));
        entity.setEntityName(limit(payload.getTargetName(), 255));
        entity.setSignalDate(LocalDate.now());
        entity.setSignalScore(signalScore);
        entity.setSignalLevel(resolveSignalStrength(signalScore).name());
        entity.setSignalDirection(direction.name());
        entity.setReasonSummary(summary);
        entity.setConfidenceScore(toBigDecimal(confidenceScore));
        entity.setSourceEventId(limit(payload.getSourceEventId(), 64));
        entity.setStatus(limit("ACTIVE", 32));
        entity.setTraceId(limit(message.getTraceId(), 128));
        entity.setTenantId(limit(defaultValue(message.getTenantId(), "default"), 64));
        entity.setDeleted(0);

        if (isNew) {
            strategySignalMapper.insert(entity);
        } else {
            strategySignalMapper.updateById(entity);
        }
        refreshStrategySignalCache(entity);

        strategySignalFactorMapper.delete(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
        );
        insertSignalFactor(signalId, "CONFIDENCE", "Confidence score", confidenceScore == null ? null : String.valueOf(confidenceScore), "Model confidence projection", 0.5);
        insertSignalFactor(signalId, "RISK_COUNT", "Risk count", String.valueOf(reasons.size()), "Risk pressure deducted from signal score", 0.3);
        if (needHumanReview) {
            insertSignalFactor(signalId, "HUMAN_REVIEW", "Human review", "true", "Manual review requirement deducted from signal score", 0.2);
        }
    }

    private void saveReportEvidenceRefs(AiTaskResultMessage message, ResearchReportDO report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        reportEvidenceRefMapper.delete(
                new LambdaQueryWrapper<ReportEvidenceRefDO>()
                        .eq(ReportEvidenceRefDO::getReportId, report.getReportId())
        );

        Object evidenceItems = reportMetaValue(payload, "evidenceItems");
        if (evidenceItems instanceof List<?> items && !items.isEmpty()) {
            for (Object item : items) {
                if (!(item instanceof Map<?, ?> evidenceItem)) {
                    continue;
                }
                ReportEvidenceRefDO entity = new ReportEvidenceRefDO();
                entity.setEvidenceId(UUID.randomUUID().toString());
                entity.setReportId(report.getReportId());
                entity.setTaskId(message.getTaskId());
                entity.setSourceType(limit(defaultValue(normalizeText(evidenceItem.get("evidenceType")), "REPORT_META"), 64));
                entity.setSourceRefId(limit(firstNonBlank(
                        normalizeText(evidenceItem.get("referenceId")),
                        normalizeText(evidenceItem.get("evidenceId"))
                ), 128));
                entity.setEvidenceSummary(firstNonBlank(
                        normalizeText(evidenceItem.get("summary")),
                        normalizeText(evidenceItem.get("title"))
                ));
                entity.setEvidenceUrl(limit(normalizeText(evidenceItem.get("url")), 512));
                entity.setDeleted(0);
                reportEvidenceRefMapper.insert(entity);
            }
            return;
        }

        for (String ref : normalizeTextList(reportMetaValue(payload, "evidenceRefs"))) {
            ReportEvidenceRefDO entity = new ReportEvidenceRefDO();
            entity.setEvidenceId(UUID.randomUUID().toString());
            entity.setReportId(report.getReportId());
            entity.setTaskId(message.getTaskId());
            entity.setSourceType(limit("EVIDENCE_REF", 64));
            entity.setSourceRefId(limit(ref, 128));
            entity.setEvidenceSummary(ref);
            entity.setDeleted(0);
            reportEvidenceRefMapper.insert(entity);
        }
    }

    private void saveReportSections(AiTaskResultMessage message, ResearchReportDO report) {
        AiTaskResultMessage.ResultPayload payload = message.getPayload();
        researchReportSectionMapper.delete(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, report.getReportId())
        );

        List<String> highlights = normalizeTextList(reportMetaValue(payload, "highlights"));
        List<String> riskPoints = normalizeTextList(reportMetaValue(payload, "riskPoints"));
        List<String> riskWarnings = normalizeTextList(payload.getRiskWarnings());
        List<String> evidenceRefs = normalizeTextList(reportMetaValue(payload, "evidenceRefs"));

        int order = 10;
        order = insertReportSection(
                message,
                report,
                "SUMMARY",
                "投资结论摘要",
                order,
                normalizeText(payload.getSummary()),
                List.of()
        );
        order = insertReportSection(
                message,
                report,
                "HIGHLIGHTS",
                "关键亮点",
                order,
                null,
                highlights
        );
        order = insertReportSection(
                message,
                report,
                "RISK_POINTS",
                "风险点",
                order,
                null,
                mergeTextList(riskWarnings, riskPoints)
        );
        insertReportSection(
                message,
                report,
                "EVIDENCE",
                "证据引用",
                order,
                null,
                evidenceRefs
        );
    }

    private int insertReportSection(AiTaskResultMessage message,
                                    ResearchReportDO report,
                                    String sectionCode,
                                    String sectionTitle,
                                    int sectionOrder,
                                    String content,
                                    List<String> items) {
        List<String> safeItems = items == null ? List.of() : items;
        if (!StringUtils.hasText(content) && safeItems.isEmpty()) {
            return sectionOrder;
        }

        ResearchReportSectionDO section = new ResearchReportSectionDO();
        section.setSectionId(UUID.randomUUID().toString());
        section.setReportId(report.getReportId());
        section.setTaskId(message.getTaskId());
        section.setVersionNo(defaultVersionNo(report.getVersionNo()));
        section.setSectionCode(limit(sectionCode, 64));
        section.setSectionTitle(limit(sectionTitle, 128));
        section.setSectionOrder(sectionOrder);
        section.setSectionContent(content);
        section.setSectionItems(toJson(safeItems));
        section.setReviewStatus(ReportReviewStatusEnum.PENDING.name());
        section.setConfidenceScore(toBigDecimal(message.getPayload().getConfidenceScore()));
        section.setTraceId(limit(message.getTraceId(), 128));
        section.setTenantId(limit(defaultValue(message.getTenantId(), "default"), 64));
        section.setDeleted(0);
        researchReportSectionMapper.insert(section);
        return sectionOrder + 10;
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private String toJson(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void markRiskWarningDeleted(String warningId) {
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

    private void markStrategySignalDeleted(String signalId) {
        StrategySignalDO entity = strategySignalMapper.selectOne(
                new LambdaQueryWrapper<StrategySignalDO>()
                        .eq(StrategySignalDO::getSignalId, signalId)
                        .last("limit 1")
        );
        if (entity == null) {
            return;
        }
        entity.setDeleted(1);
        strategySignalMapper.updateById(entity);
        strategySignalFactorMapper.delete(
                new LambdaQueryWrapper<StrategySignalFactorDO>()
                        .eq(StrategySignalFactorDO::getSignalId, signalId)
        );
        evictStrategySignalCache(entity);
    }

    private void refreshStrategySignalCache(StrategySignalDO signal) {
        if (signal == null || !StringUtils.hasText(signal.getSignalId())) {
            return;
        }
        try {
            if (StringUtils.hasText(signal.getEntityCode())) {
                stringRedisTemplate.opsForValue().set(
                        RedisKeyBuilder.signalLatest(signal.getEntityCode()),
                        serializeStrategySignal(signal)
                );
            }
            if (signal.getSignalDate() != null && signal.getSignalScore() != null) {
                stringRedisTemplate.opsForZSet().add(
                        RedisKeyBuilder.signalRanking(signal.getSignalDate().toString()),
                        signal.getSignalId(),
                        signal.getSignalScore().doubleValue()
                );
            }
        } catch (Exception e) {
            log.warn("refresh strategy signal redis cache failed, signalId={}, taskId={}",
                    signal.getSignalId(),
                    signal.getTaskId(),
                    e);
        }
    }

    private void evictStrategySignalCache(StrategySignalDO signal) {
        if (signal == null || !StringUtils.hasText(signal.getSignalId())) {
            return;
        }
        try {
            if (StringUtils.hasText(signal.getEntityCode())) {
                stringRedisTemplate.delete(RedisKeyBuilder.signalLatest(signal.getEntityCode()));
            }
            if (signal.getSignalDate() != null) {
                stringRedisTemplate.opsForZSet().remove(
                        RedisKeyBuilder.signalRanking(signal.getSignalDate().toString()),
                        signal.getSignalId()
                );
            }
        } catch (Exception e) {
            log.warn("evict strategy signal redis cache failed, signalId={}, taskId={}",
                    signal.getSignalId(),
                    signal.getTaskId(),
                    e);
        }
    }

    private String serializeStrategySignal(StrategySignalDO signal) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("signalId", signal.getSignalId());
        payload.put("taskId", signal.getTaskId());
        payload.put("signalType", signal.getSignalType());
        payload.put("entityCode", signal.getEntityCode());
        payload.put("entityName", signal.getEntityName());
        payload.put("signalDate", signal.getSignalDate() == null ? null : signal.getSignalDate().toString());
        payload.put("signalScore", signal.getSignalScore());
        payload.put("signalLevel", signal.getSignalLevel());
        payload.put("signalDirection", signal.getSignalDirection());
        payload.put("reasonSummary", signal.getReasonSummary());
        payload.put("confidenceScore", signal.getConfidenceScore());
        payload.put("sourceEventId", signal.getSourceEventId());
        payload.put("status", signal.getStatus());
        payload.put("traceId", signal.getTraceId());
        payload.put("tenantId", signal.getTenantId());
        return objectMapper.writeValueAsString(payload);
    }

    private void insertSignalFactor(String signalId,
                                    String factorCode,
                                    String factorName,
                                    String factorValue,
                                    String factorConclusion,
                                    double factorWeight) {
        StrategySignalFactorDO factor = new StrategySignalFactorDO();
        factor.setFactorId(UUID.randomUUID().toString());
        factor.setSignalId(signalId);
        factor.setFactorCode(limit(factorCode, 64));
        factor.setFactorName(limit(factorName, 128));
        factor.setFactorValue(limit(factorValue, 128));
        factor.setFactorWeight(BigDecimal.valueOf(factorWeight));
        factor.setFactorConclusion(factorConclusion);
        factor.setDeleted(0);
        strategySignalFactorMapper.insert(factor);
    }

    private Object reportMetaValue(AiTaskResultMessage.ResultPayload payload, String key) {
        Map<String, Object> reportMeta = payload.getReportMeta();
        if (reportMeta == null) {
            return null;
        }
        return reportMeta.get(key);
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

    private SignalDirectionEnum resolveSignalDirection(Double confidenceScore, int riskCount, boolean needHumanReview) {
        if (needHumanReview || riskCount >= 3) {
            return SignalDirectionEnum.NEGATIVE;
        }
        if (confidenceScore != null && confidenceScore >= 0.85 && riskCount == 0) {
            return SignalDirectionEnum.POSITIVE;
        }
        return SignalDirectionEnum.NEUTRAL;
    }

    private int calculateSignalScore(Double confidenceScore, int riskCount, boolean needHumanReview) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0.0, Math.min(1.0, confidenceScore)) * 100);
        score -= Math.min(40, riskCount * 8);
        if (needHumanReview) {
            score -= 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    private SignalStrengthEnum resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return SignalStrengthEnum.STRONG;
        }
        if (signalScore >= 60) {
            return SignalStrengthEnum.MEDIUM;
        }
        return SignalStrengthEnum.WEAK;
    }

    private String resolveReportType(ResearchReportDO report, AiTaskResultMessage.ResultPayload payload) {
        if (StringUtils.hasText(report.getReportType())) {
            return report.getReportType();
        }
        if (StringUtils.hasText(payload.getTaskType())) {
            return payload.getTaskType();
        }
        return "AI_RESEARCH_SIGNAL";
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, value)));
    }

    private String buildRiskWarningId(String taskId) {
        return "risk-" + taskId;
    }

    private String buildStrategySignalId(String taskId) {
        return "signal-" + taskId;
    }

    private List<String> mergeTextList(List<String> left, List<String> right) {
        List<String> result = new ArrayList<>();
        result.addAll(left);
        for (String item : right) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> normalizeTextList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : values) {
            String text = normalizeText(item);
            if (StringUtils.hasText(text) && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private String resolveFirstText(List<String> values, String fallback) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return fallback;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return null;
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || maxLength <= 0) {
            return value;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
