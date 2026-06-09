package com.quant.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.task.risk.RiskWarningTaskProjection;
import com.quant.task.risk.RiskWarningTaskReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskRiskWarningTaskReadAdapter implements RiskWarningTaskReadPort {

    private final ResearchTaskMapper researchTaskMapper;

    @Override
    public Map<String, RiskWarningTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
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
    public List<RiskWarningTaskProjection> loadRiskWarningFollowUpTasks() {
        return loadFollowUpTasksBySourceDomain("RISK_WARNING");
    }

    @Override
    public List<RiskWarningTaskProjection> loadFollowUpTasksBySourceDomain(String sourceDomain) {
        if (sourceDomain == null || sourceDomain.isBlank()) {
            return List.of();
        }
        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, sourceDomain)
        ).stream().map(this::toProjection).toList();
    }

    private RiskWarningTaskProjection toProjection(ResearchTaskDO task) {
        return new RiskWarningTaskProjection(
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
                task.getCreatedAt()
        );
    }
}
