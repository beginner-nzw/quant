package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.entity.HumanReviewRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportReviewLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportReviewLogMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.TaskReportService;
import com.quant.common.core.exception.BizException;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskReportServiceImpl implements TaskReportService {

    private final ResearchReportMapper researchReportMapper;
    private final ObjectMapper objectMapper;
    private final ResearchReportReviewLogMapper researchReportReviewLogMapper;
    private final HumanReviewRecordMapper humanReviewRecordMapper;
    private final ResearchReportSectionMapper researchReportSectionMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final ReportVersionService reportVersionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reviewReport(String taskId, TaskReportReviewDTO dto) {
        ResearchReportDO report = researchReportMapper.selectOne(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getTaskId, taskId)
                        .eq(ResearchReportDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (report == null) {
            throw new BizException("REPORT_NOT_FOUND", "报告不存在");
        }

        ReportReviewStatusEnum reviewStatus = ReportReviewStatusEnum.from(dto.getReviewStatus());
        if (reviewStatus == null) {
            throw new BizException("REPORT_REVIEW_STATUS_INVALID", "报告审核状态无效");
        }

        String beforeSnapshotJson;
        try {
            beforeSnapshotJson = objectMapper.writeValueAsString(buildReportSnapshot(report));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String revisedHighlightsJson = null;
        try {
            revisedHighlightsJson = dto.getRevisedHighlights() == null
                    ? null
                    : objectMapper.writeValueAsString(dto.getRevisedHighlights());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String revisedRiskPointsJson = null;
        try {
            revisedRiskPointsJson = dto.getRevisedRiskPoints() == null
                    ? null
                    : objectMapper.writeValueAsString(dto.getRevisedRiskPoints());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        report.setVersionNo(resolveNextVersionNo(report.getVersionNo()));
        report.setReviewStatus(reviewStatus.name());
        report.setReviewedBy(dto.getReviewedBy());
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewComment(dto.getReviewComment());
        report.setRevisedSummary(dto.getRevisedSummary());
        report.setRevisedHighlights(revisedHighlightsJson);
        report.setRevisedRiskPoints(revisedRiskPointsJson);

        researchReportMapper.updateById(report);

        ResearchReportReviewLogDO log = new ResearchReportReviewLogDO();
        log.setReviewLogId(java.util.UUID.randomUUID().toString());
        log.setReportId(report.getReportId());
        log.setTaskId(taskId);
        log.setVersionNo(defaultVersionNo(report.getVersionNo()));
        log.setReviewStatus(reviewStatus.name());
        log.setReviewedBy(dto.getReviewedBy());
        log.setReviewComment(dto.getReviewComment());
        log.setRevisedSummary(dto.getRevisedSummary());
        log.setRevisedHighlights(revisedHighlightsJson);
        log.setRevisedRiskPoints(revisedRiskPointsJson);
        log.setDeleted(0);

        researchReportReviewLogMapper.insert(log);
        updateReportSectionsReview(report, dto, reviewStatus, report.getReviewedAt(), revisedHighlightsJson, revisedRiskPointsJson);
        insertHumanReviewRecord(report, dto, reviewStatus, beforeSnapshotJson, revisedHighlightsJson, revisedRiskPointsJson);
        reportVersionService.createSnapshot(report, "REPORT_REVIEW");

        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyBuilder.taskResult(taskId));
        taskCacheVersionManager.bumpVersion();

        return taskId;
    }

    private void updateReportSectionsReview(ResearchReportDO report,
                                            TaskReportReviewDTO dto,
                                            ReportReviewStatusEnum reviewStatus,
                                            LocalDateTime reviewedAt,
                                            String revisedHighlightsJson,
                                            String revisedRiskPointsJson) {
        List<ResearchReportSectionDO> sections = researchReportSectionMapper.selectList(
                new LambdaQueryWrapper<ResearchReportSectionDO>()
                        .eq(ResearchReportSectionDO::getReportId, report.getReportId())
                        .eq(ResearchReportSectionDO::getDeleted, 0)
        );
        if (sections == null || sections.isEmpty()) {
            return;
        }

        for (ResearchReportSectionDO section : sections) {
            section.setReviewStatus(reviewStatus.name());
            section.setVersionNo(defaultVersionNo(report.getVersionNo()));
            section.setReviewedBy(dto.getReviewedBy());
            section.setReviewedAt(reviewedAt);
            section.setReviewComment(dto.getReviewComment());
            if ("SUMMARY".equals(section.getSectionCode())) {
                section.setRevisedContent(normalizeText(dto.getRevisedSummary()));
            } else if ("HIGHLIGHTS".equals(section.getSectionCode())) {
                section.setRevisedItems(revisedHighlightsJson);
            } else if ("RISK_POINTS".equals(section.getSectionCode())) {
                section.setRevisedItems(revisedRiskPointsJson);
            }
            researchReportSectionMapper.updateById(section);
        }
    }

    @Override
    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return researchReportReviewLogMapper.selectList(
                new LambdaQueryWrapper<ResearchReportReviewLogDO>()
                        .eq(ResearchReportReviewLogDO::getTaskId, taskId)
                        .eq(ResearchReportReviewLogDO::getDeleted, 0)
                        .orderByDesc(ResearchReportReviewLogDO::getId)
        ).stream().map(item -> {
            TaskReportReviewLogVO vo = new TaskReportReviewLogVO();
            vo.setReviewLogId(item.getReviewLogId());
            vo.setReportId(item.getReportId());
            vo.setTaskId(item.getTaskId());
            vo.setVersionNo(defaultVersionNo(item.getVersionNo()));
            vo.setReviewStatus(item.getReviewStatus());
            vo.setReviewedBy(item.getReviewedBy());
            vo.setReviewComment(item.getReviewComment());
            vo.setRevisedSummary(item.getRevisedSummary());
            vo.setCreatedAt(item.getCreatedAt() == null ? null : item.getCreatedAt().toString());

            try {
                if (item.getRevisedHighlights() != null && !item.getRevisedHighlights().isBlank()) {
                    vo.setRevisedHighlights(objectMapper.readValue(
                            item.getRevisedHighlights(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {}
                    ));
                }
            } catch (Exception ignored) {
            }

            try {
                if (item.getRevisedRiskPoints() != null && !item.getRevisedRiskPoints().isBlank()) {
                    vo.setRevisedRiskPoints(objectMapper.readValue(
                            item.getRevisedRiskPoints(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {}
                    ));
                }
            } catch (Exception ignored) {
            }

            return vo;
        }).toList();
    }

    private void insertHumanReviewRecord(ResearchReportDO report,
                                         TaskReportReviewDTO dto,
                                         ReportReviewStatusEnum reviewStatus,
                                         String beforeSnapshotJson,
                                         String revisedHighlightsJson,
                                         String revisedRiskPointsJson) {
        HumanReviewRecordDO record = new HumanReviewRecordDO();
        record.setReviewId(java.util.UUID.randomUUID().toString());
        record.setTaskId(report.getTaskId());
        record.setRelatedObjectType("REPORT");
        record.setRelatedObjectId(report.getReportId());
        record.setReviewerId(dto.getReviewedBy());
        record.setReviewerRole(SecurityUtils.currentUserRole());
        record.setReviewResult(reviewStatus.name());
        record.setReviewComment(dto.getReviewComment());
        record.setBeforeSnapshot(beforeSnapshotJson);
        record.setAfterSnapshot(buildAfterSnapshotJson(report, revisedHighlightsJson, revisedRiskPointsJson));
        record.setTenantId("default");
        record.setDeleted(0);
        humanReviewRecordMapper.insert(record);
    }

    private String buildAfterSnapshotJson(ResearchReportDO report,
                                          String revisedHighlightsJson,
                                          String revisedRiskPointsJson) {
        try {
            return objectMapper.writeValueAsString(buildReportSnapshot(report));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> buildReportSnapshot(ResearchReportDO report) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("reportId", report.getReportId());
        snapshot.put("taskId", report.getTaskId());
        snapshot.put("versionNo", defaultVersionNo(report.getVersionNo()));
        snapshot.put("reviewStatus", report.getReviewStatus());
        snapshot.put("reviewedBy", report.getReviewedBy());
        snapshot.put("reviewedAt", report.getReviewedAt());
        snapshot.put("reviewComment", report.getReviewComment());
        snapshot.put("revisedSummary", report.getRevisedSummary());
        snapshot.put("revisedHighlights", parseJsonArray(report.getRevisedHighlights()));
        snapshot.put("revisedRiskPoints", parseJsonArray(report.getRevisedRiskPoints()));
        return snapshot;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private int defaultVersionNo(Integer versionNo) {
        return versionNo == null || versionNo < 1 ? 1 : versionNo;
    }

    private int resolveNextVersionNo(Integer currentVersionNo) {
        if (currentVersionNo == null || currentVersionNo < 1) {
            return 2;
        }
        return currentVersionNo + 1;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            return List.of();
        }
    }
}
