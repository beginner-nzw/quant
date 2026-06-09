package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskListItemVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.common.model.enums.ReportReviewStatusEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class TaskQueryItemAssembler {

    private final ObjectMapper objectMapper;

    public TaskQueryItemAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TaskDetailVO toTaskDetailVO(ResearchTaskDO task) {
        if (task == null) {
            return null;
        }
        TaskDetailVO vo = new TaskDetailVO();
        BeanUtils.copyProperties(task, vo);
        if (!shouldDisplayTaskErrorMessage(task.getStatus())) {
            vo.setErrorMessage(null);
        }
        return vo;
    }

    public TaskListItemVO toTaskListItemVO(ResearchTaskDO task, TaskReportProjection report) {
        TaskListItemVO item = new TaskListItemVO();
        BeanUtils.copyProperties(task, item);
        if (!shouldDisplayTaskErrorMessage(task.getStatus())) {
            item.setErrorMessage(null);
        }

        if (report != null) {
            item.setReportId(report.getReportId());
            item.setReportType(resolveReportType(report, task));
            item.setReportReviewStatus(report.getReviewStatus() == null ? ReportReviewStatusEnum.PENDING.name() : report.getReviewStatus());
            item.setRevised(isReportRevised(report));
            item.setSummaryRevised(isSummaryRevised(report));
            item.setHighlightsRevised(isHighlightsRevised(report));
            item.setRiskPointsRevised(isRiskPointsRevised(report));
            item.setReportReviewedBy(report.getReviewedBy());
            item.setReportReviewedAt(report.getReviewedAt());
            item.setReportReviewComment(report.getReviewComment());
        }
        return item;
    }

    public TaskStepVO toTaskStepVO(ResearchTaskStepDO entity) {
        TaskStepVO vo = new TaskStepVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public TaskRetryLogVO toRetryLogVO(ResearchTaskRetryLogDO entity) {
        TaskRetryLogVO vo = new TaskRetryLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private boolean isReportRevised(TaskReportProjection report) {
        return isSummaryRevised(report) || isHighlightsRevised(report) || isRiskPointsRevised(report);
    }

    private boolean isSummaryRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        String revisedSummary = report.getRevisedSummary();
        return revisedSummary != null && !revisedSummary.isBlank() && !Objects.equals(
                normalizeText(report.getSummary()),
                normalizeText(revisedSummary)
        );
    }

    private boolean isHighlightsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getHighlights()).equals(
                readPreferredTextList(report.getRevisedHighlights(), report.getHighlights())
        );
    }

    private boolean isRiskPointsRevised(TaskReportProjection report) {
        if (report == null) {
            return false;
        }
        return !readTextList(report.getRiskPoints()).equals(
                readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints())
        );
    }

    private String resolveReportType(TaskReportProjection report, ResearchTaskDO task) {
        if (report != null && report.getReportType() != null && !report.getReportType().isBlank()) {
            return report.getReportType();
        }
        return task == null ? null : task.getTaskType();
    }

    private List<String> readTextList(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(item -> !item.isBlank())
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> readPreferredTextList(String preferredRawJson, String fallbackRawJson) {
        List<String> preferred = readTextList(preferredRawJson);
        return preferred.isEmpty() ? readTextList(fallbackRawJson) : preferred;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean shouldDisplayTaskErrorMessage(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return TaskStatusEnum.FAILED.name().equals(status) || TaskStatusEnum.CANCELLED.name().equals(status);
    }
}
