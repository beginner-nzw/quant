package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.report.TaskReportProjection;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.task.market.MarketEventTaskProjection;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MarketEventFollowUpTaskSummaryManager {

    public FollowUpSummary resolveSummary(MarketEventTaskProjection sourceTask,
                                          TaskReportProjection sourceReport,
                                          Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceTaskId,
                                          Map<String, List<MarketEventTaskProjection>> followUpTaskMapBySourceReportId) {
        if (sourceTask == null && sourceReport == null) {
            return defaultSummary();
        }

        LinkedHashMap<String, MarketEventTaskProjection> followUpTaskMap = new LinkedHashMap<>();
        if (sourceTask != null && sourceTask.taskId() != null) {
            followUpTaskMapBySourceTaskId.getOrDefault(sourceTask.taskId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.taskId(), item));
        }
        if (sourceReport != null && sourceReport.reportId() != null) {
            followUpTaskMapBySourceReportId.getOrDefault(sourceReport.reportId(), List.of())
                    .forEach(item -> followUpTaskMap.put(item.taskId(), item));
        }

        List<MarketEventTaskProjection> followUpTasks = new ArrayList<>(followUpTaskMap.values());
        if (followUpTasks.isEmpty()) {
            return defaultSummary();
        }

        followUpTasks.sort(Comparator
                .comparing(MarketEventTaskProjection::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MarketEventTaskProjection::id, Comparator.nullsLast(Comparator.reverseOrder())));

        MarketEventTaskProjection latestTask = followUpTasks.get(0);
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

    private String resolveStatus(List<MarketEventTaskProjection> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return "NOT_TRACKED";
        }

        boolean hasActiveTask = followUpTasks.stream()
                .map(MarketEventTaskProjection::status)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.INIT
                        || status == TaskStatusEnum.DISPATCHED
                        || status == TaskStatusEnum.RUNNING);
        if (hasActiveTask) {
            return "TRACKING";
        }

        boolean hasSuccessTask = followUpTasks.stream()
                .map(MarketEventTaskProjection::status)
                .map(TaskStatusEnum::from)
                .anyMatch(status -> status == TaskStatusEnum.SUCCESS);
        if (hasSuccessTask) {
            return "COMPLETED";
        }

        boolean hasFailedTask = followUpTasks.stream()
                .map(MarketEventTaskProjection::status)
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
