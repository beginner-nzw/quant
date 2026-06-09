package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskCrossDomainReadManager {

    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;

    public AiWorkflowInstanceDO selectLatestWorkflowInstance(String taskId) {
        return aiWorkflowInstanceMapper.selectOne(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getTaskId, taskId)
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
                        .last("limit 1")
        );
    }

    public List<AiAgentExecutionDO> listAgentExecutions(String taskId) {
        return aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getTaskId, taskId)
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .orderByAsc(AiAgentExecutionDO::getId)
        );
    }

    public Map<String, AiWorkflowInstanceDO> loadLatestWorkflowInstanceMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return aiWorkflowInstanceMapper.selectList(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .in(AiWorkflowInstanceDO::getTaskId, taskIds)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
        ).stream().collect(Collectors.toMap(
                AiWorkflowInstanceDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    public Map<String, List<AiAgentExecutionDO>> loadAgentExecutionMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .in(AiAgentExecutionDO::getTaskId, taskIds)
                        .orderByDesc(AiAgentExecutionDO::getCreatedAt, AiAgentExecutionDO::getId)
        ).stream().collect(Collectors.groupingBy(AiAgentExecutionDO::getTaskId));
    }
}
