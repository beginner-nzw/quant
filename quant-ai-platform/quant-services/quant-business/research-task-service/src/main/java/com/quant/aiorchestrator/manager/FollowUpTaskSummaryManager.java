package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.common.model.enums.TaskStatusEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FollowUpTaskSummaryManager {

    public FollowUpSummary resolveSummary(ResearchTaskDO sourceTask,
                                          TaskReportProjection sourceReport,
                                          Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceTaskId,
                                          Map<String, List<ResearchTaskDO>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultSummary();
        }

        LinkedHashMap<String, ResearchTaskDO> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.getTaskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.getTaskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }
        if (sourceReport != null && sourceReport.getReportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.getReportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.getTaskId(), item));
        }

        List<ResearchTaskDO> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        ResearchTaskDO latestTask = followUpTasks.get(0);
        return new FollowUpSummary(
                resolveStatus(followUpTasks),
                followUpTasks.size(),
                latestTask.getTaskId(),
                latestTask.getTaskTitle(),
                latestTask.getStatus(),
                latestTask.getCreatedAt()
        );
    }

    private FollowUpSummary defaultSummary() {
        return new FollowUpSummary("NOT_TRACKED", 0, null, null, null, null);
    }

    private String resolveStatus(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(ResearchTaskDO::getStatus)
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
