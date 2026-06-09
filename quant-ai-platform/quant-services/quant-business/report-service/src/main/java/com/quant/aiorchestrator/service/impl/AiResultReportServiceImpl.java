package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.ResearchReportSnapshot;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.service.AiResultReportService;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.model.message.AiTaskResultMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiResultReportServiceImpl implements AiResultReportService {

    private final ObjectMapper objectMapper;
    private final ResearchReportMapper researchReportMapper;

    @Override
    public ResearchReportSnapshot saveReport(AiTaskResultMessage message) {
        if (message == null || message.getPayload() == null
                || !TaskStatusEnum.SUCCESS.name().equals(message.getPayload().getFinalStatus())) {
            return null;
        }

        ResearchReportDO report = researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, message.getTaskId())
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );

        boolean isNew = false;
        if (report == null) {
            report = new ResearchReportDO();
            report.setReportId(UUID.randomUUID().toString());
            report.setTaskId(message.getTaskId());
            report.setVersionNo(1);
            report.setDeleted(0);
            isNew = true;
        } else {
            report.setVersionNo(resolveNextVersionNo(report.getVersionNo()));
        }

        report.setTaskType(message.getPayload().getTaskType());
        report.setFinalStatus(message.getPayload().getFinalStatus());
        report.setSummary(message.getPayload().getSummary());
        report.setConfidenceScore(message.getPayload().getConfidenceScore() == null
                ? null
                : BigDecimal.valueOf(message.getPayload().getConfidenceScore()));
        report.setNeedHumanReview(Boolean.TRUE.equals(message.getPayload().getNeedHumanReview()) ? 1 : 0);
        report.setResultRef(message.getPayload().getResultRef());

        report.setReviewStatus(null);
        report.setReviewedBy(null);
        report.setReviewedAt(null);
        report.setRevisedSummary(null);
        report.setRevisedHighlights(null);
        report.setRevisedRiskPoints(null);
        report.setReviewComment(null);

        Map<String, Object> reportMeta = message.getPayload().getReportMeta();
        Object reportType = reportMeta == null ? null : reportMeta.get("reportType");
        report.setReportType(reportType == null ? null : String.valueOf(reportType));

        Object highlights = reportMeta == null ? null : reportMeta.get("highlights");
        report.setHighlights(writeJsonOrNull(highlights));

        Object riskPoints = reportMeta == null ? null : reportMeta.get("riskPoints");
        report.setRiskPoints(writeJsonOrNull(riskPoints));

        if (message.getPayload().getRiskWarnings() != null) {
            report.setRiskWarnings(writeJsonOrNull(message.getPayload().getRiskWarnings()));
        } else {
            report.setRiskWarnings("[]");
        }

        report.setRawPayload(writeJsonOrNull(message.getPayload()));

        if (isNew) {
            researchReportMapper.insert(report);
        } else {
            researchReportMapper.updateById(report);
        }
        return toSnapshot(report);
    }

    private ResearchReportSnapshot toSnapshot(ResearchReportDO report) {
        ResearchReportSnapshot snapshot = new ResearchReportSnapshot();
        snapshot.setReportId(report.getReportId());
        snapshot.setTaskId(report.getTaskId());
        snapshot.setVersionNo(report.getVersionNo());
        snapshot.setTaskType(report.getTaskType());
        snapshot.setFinalStatus(report.getFinalStatus());
        snapshot.setSummary(report.getSummary());
        snapshot.setConfidenceScore(report.getConfidenceScore());
        snapshot.setNeedHumanReview(report.getNeedHumanReview());
        snapshot.setReportType(report.getReportType());
        snapshot.setHighlights(report.getHighlights());
        snapshot.setRiskPoints(report.getRiskPoints());
        snapshot.setRiskWarnings(report.getRiskWarnings());
        snapshot.setResultRef(report.getResultRef());
        snapshot.setRawPayload(report.getRawPayload());
        snapshot.setReviewStatus(report.getReviewStatus());
        snapshot.setReviewedBy(report.getReviewedBy());
        snapshot.setReviewedAt(report.getReviewedAt() == null ? null : report.getReviewedAt().toString());
        snapshot.setRevisedSummary(report.getRevisedSummary());
        snapshot.setRevisedHighlights(report.getRevisedHighlights());
        snapshot.setRevisedRiskPoints(report.getRevisedRiskPoints());
        snapshot.setReviewComment(report.getReviewComment());
        return snapshot;
    }

    private String writeJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("serialize ai result report field failed", e);
        }
    }

    private int resolveNextVersionNo(Integer currentVersionNo) {
        if (currentVersionNo == null || currentVersionNo < 1) {
            return 2;
        }
        return currentVersionNo + 1;
    }
}
