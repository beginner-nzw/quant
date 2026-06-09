package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.risk.RiskWarningTaskProjection;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RiskWarningFollowUpSummaryManager {

    public FollowUpSummary resolveSummary(RiskWarningTaskProjection sourceTask,
                                          TaskReportProjection sourceReport,
                                          Map<String, List<RiskWarningTaskProjection>> followUpTaskMapBySourceTaskId,
                                          Map<String, List<RiskWarningTaskProjection>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultSummary();
        }

        LinkedHashMap<String, RiskWarningTaskProjection> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.taskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.taskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.taskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.taskId(), item));
        }

        List<RiskWarningTaskProjection> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(RiskWarningTaskProjection::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RiskWarningTaskProjection::id, Comparator.nullsLast(Comparator.reverseOrder())));

        RiskWarningTaskProjection latestTask = followUpTasks.get(0);
        return new FollowUpSummary(
                resolveStatus(followUpTasks),
                followUpTasks.size(),
                latestTask.taskId(),
                latestTask.taskTitle(),
                latestTask.status(),
                latestTask.createdAt()
        );
    }

    private FollowUpSummary defaultSummary() {
        return new FollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveStatus(List<RiskWarningTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(RiskWarningTaskProjection::status)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(RiskWarningTaskProjection::status)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(RiskWarningTaskProjection::status)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.FAILED || status == TaskStatusEnum.CANCELLED);
        if (hasFailedTask) {
            return "FAILED";
        }

        return "TRACKING";
    }

    public record FollowUpSummary(
            String followUpStatus,
            Integer followUpTaskCount,
            String latestFollowUpTaskId,
            String latestFollowUpTaskTitle,
            String latestFollowUpTaskStatus,
            LocalDateTime latestFollowUpCreatedAt
    ) {
    }
}
