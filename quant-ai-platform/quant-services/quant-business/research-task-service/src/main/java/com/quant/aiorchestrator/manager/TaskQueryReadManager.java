package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskQueryReadManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskStepMapper researchTaskStepMapper;
    private final ResearchTaskRetryLogMapper researchTaskRetryLogMapper;

    public ResearchTaskDO selectTaskById(String taskId) {
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    public List<ResearchTaskStepDO> listTaskSteps(String taskId) {
        return researchTaskStepMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskStepDO>()
                        .eq(ResearchTaskStepDO::getTaskId, taskId)
                        .eq(ResearchTaskStepDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskStepDO::getExecutionOrder, ResearchTaskStepDO::getId)
        );
    }

    public Page<ResearchTaskDO> pageTasks(Page<ResearchTaskDO> page, LambdaQueryWrapper<ResearchTaskDO> wrapper) {
        return researchTaskMapper.selectPage(page, wrapper);
    }

    public long countDistinctSourceEvents(String sourceDomain) {
        if (sourceDomain == null || sourceDomain.isBlank()) {
            return 0L;
        }
        List<Object> values = researchTaskMapper.selectObjs(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ResearchTaskDO>()
                        .select("COUNT(DISTINCT source_event_id)")
                        .eq("deleted", 0)
                        .eq("source_domain", sourceDomain)
                        .isNotNull("source_event_id")
        );
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return 0L;
        }
        Object value = values.get(0);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public List<ResearchTaskDO> listWorkbenchTasks(String targetCode, String targetName) {
        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId);
        if (targetCode != null && !targetCode.isBlank()) {
            wrapper.eq(ResearchTaskDO::getTargetCode, targetCode.trim());
        }
        if (targetName != null && !targetName.isBlank()) {
            wrapper.like(ResearchTaskDO::getTargetName, targetName.trim());
        }
        return researchTaskMapper.selectList(wrapper);
    }

    public Map<String, List<ResearchTaskDO>> loadFollowUpTasksBySourceEvents(String sourceDomain, List<String> sourceEventIds) {
        if (sourceDomain == null || sourceDomain.isBlank() || sourceEventIds == null || sourceEventIds.isEmpty()) {
            return Map.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, sourceDomain)
                        .in(ResearchTaskDO::getSourceEventId, sourceEventIds)
                        .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId)
        ).stream()
                .filter(item -> item.getSourceEventId() != null && !item.getSourceEventId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceEventId));
    }

    public Map<String, ResearchTaskDO> loadTaskMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                item -> item,
                (left, right) -> left
        ));
    }

    public List<ResearchTaskDO> loadFollowUpTasks(String sourceDomain,
                                                  Set<String> sourceTaskIds,
                                                  Set<String> sourceReportIds) {
        boolean hasSourceTaskIds = sourceTaskIds != null && !sourceTaskIds.isEmpty();
        boolean hasSourceReportIds = sourceReportIds != null && !sourceReportIds.isEmpty();
        if (!hasSourceTaskIds && !hasSourceReportIds) {
            return List.of();
        }

        LambdaQueryWrapper<ResearchTaskDO> wrapper = new LambdaQueryWrapper<ResearchTaskDO>()
                .eq(ResearchTaskDO::getDeleted, 0)
                .eq(ResearchTaskDO::getSourceDomain, sourceDomain);
        if (hasSourceTaskIds && hasSourceReportIds) {
            wrapper.and(nested -> nested
                    .in(ResearchTaskDO::getSourceTaskId, sourceTaskIds)
                    .or()
                    .in(ResearchTaskDO::getSourceReportId, sourceReportIds));
        } else if (hasSourceTaskIds) {
            wrapper.in(ResearchTaskDO::getSourceTaskId, sourceTaskIds);
        } else {
            wrapper.in(ResearchTaskDO::getSourceReportId, sourceReportIds);
        }
        return researchTaskMapper.selectList(wrapper);
    }

    public ResearchTaskDO selectLatestTaskBySourceEvent(String sourceDomain, String sourceEventId) {
        if (sourceDomain == null || sourceDomain.isBlank() || sourceEventId == null || sourceEventId.isBlank()) {
            return null;
        }
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, sourceDomain)
                        .eq(ResearchTaskDO::getSourceEventId, sourceEventId)
                        .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId)
                        .last("limit 1")
        );
    }

    public Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceTaskId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Map.of();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceTaskId() != null && !item.getSourceTaskId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceTaskId));
    }

    public Map<String, List<ResearchTaskDO>> groupFollowUpTasksBySourceReportId(List<ResearchTaskDO> followUpTasks) {
        if (followUpTasks == null || followUpTasks.isEmpty()) {
            return Map.of();
        }
        return followUpTasks.stream()
                .filter(item -> item.getSourceReportId() != null && !item.getSourceReportId().isBlank())
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceReportId));
    }

    public List<ResearchTaskRetryLogDO> listRetryLogs(String taskId) {
        return researchTaskRetryLogMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskRetryLogDO>()
                        .eq(ResearchTaskRetryLogDO::getTaskId, taskId)
                        .eq(ResearchTaskRetryLogDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskRetryLogDO::getRetryNo)
        );
    }
}
