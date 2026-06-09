package com.quant.task.service;

import com.quant.aiorchestrator.audit.AuditComplianceTaskProjection;
import com.quant.aiorchestrator.audit.AuditComplianceTaskProvider;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.manager.TaskQueryReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ResearchTaskAuditComplianceTaskProvider implements AuditComplianceTaskProvider {

    private final TaskQueryReadManager taskQueryReadManager;

    @Override
    public Map<String, AuditComplianceTaskProjection> loadTaskMapByTaskIds(Set<String> taskIds) {
        return taskQueryReadManager.loadTaskMapByTaskIds(taskIds)
                .values()
                .stream()
                .collect(Collectors.toMap(
                        ResearchTaskDO::getTaskId,
                        this::toProjection,
                        (left, right) -> left
                ));
    }

    private AuditComplianceTaskProjection toProjection(ResearchTaskDO task) {
        return new AuditComplianceTaskProjection(
                task.getTaskId(),
                task.getTaskTitle(),
                task.getTaskType(),
                task.getTargetCode(),
                task.getTargetName(),
                task.getPriority(),
                task.getTraceId(),
                task.getCreatedAt()
        );
    }
}
