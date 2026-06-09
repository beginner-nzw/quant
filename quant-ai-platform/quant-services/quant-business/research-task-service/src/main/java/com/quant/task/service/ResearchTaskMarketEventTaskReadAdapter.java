package com.quant.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.task.market.MarketEventTaskProjection;
import com.quant.task.market.MarketEventTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskMarketEventTaskReadAdapter implements MarketEventTaskReadPort {

    private final ResearchTaskMapper researchTaskMapper;

    @Override
    public long countDistinctSourceEvents(String sourceDomain) {
        if (sourceDomain == null || sourceDomain.isBlank()) {
            return 0L;
        }
        List<Object> values = researchTaskMapper.selectObjs(
                new QueryWrapper<ResearchTaskDO>()
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

    @Override
    public Map<String, MarketEventTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .in(ResearchTaskDO::getTaskId, taskIds)
        ).stream().collect(Collectors.toMap(
                ResearchTaskDO::getTaskId,
                this::toProjection,
                (left, right) -> left
        ));
    }

    @Override
    public MarketEventTaskProjection selectLatestTaskBySourceEvent(String sourceDomain, String sourceEventId) {
        if (sourceDomain == null || sourceDomain.isBlank() || sourceEventId == null || sourceEventId.isBlank()) {
            return null;
        }
        ResearchTaskDO task = researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, sourceDomain)
                        .eq(ResearchTaskDO::getSourceEventId, sourceEventId)
                        .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId)
                        .last("limit 1")
        );
        return task == null ? null : toProjection(task);
    }

    @Override
    public Map<String, List<MarketEventTaskProjection>> loadFollowUpTasksBySourceEvents(String sourceDomain,
                                                                                         List<String> sourceEventIds) {
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
                .collect(Collectors.groupingBy(
                        ResearchTaskDO::getSourceEventId,
                        Collectors.mapping(this::toProjection, Collectors.toList())
                ));
    }

    @Override
    public List<MarketEventTaskProjection> loadFollowUpTasks(String sourceDomain,
                                                             Set<String> sourceTaskIds,
                                                             Set<String> sourceReportIds) {
        boolean hasSourceTaskIds = sourceTaskIds != null && !sourceTaskIds.isEmpty();
        boolean hasSourceReportIds = sourceReportIds != null && !sourceReportIds.isEmpty();
        if (sourceDomain == null || sourceDomain.isBlank() || (!hasSourceTaskIds && !hasSourceReportIds)) {
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
        return researchTaskMapper.selectList(wrapper).stream().map(this::toProjection).toList();
    }

    private MarketEventTaskProjection toProjection(ResearchTaskDO task) {
        return new MarketEventTaskProjection(
                task.getId(),
                task.getTaskId(),
                task.getTaskType(),
                task.getTaskTitle(),
                task.getTargetCode(),
                task.getTargetName(),
                task.getPriority(),
                task.getStatus(),
                task.getCurrentStage(),
                task.getSourceTaskId(),
                task.getSourceReportId(),
                task.getSourceDomain(),
                task.getSourceEventId(),
                task.getCreatedAt()
        );
    }
}
