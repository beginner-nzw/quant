package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditTaskReadManager {

    private final AuditRecordMapper auditRecordMapper;

    public List<AuditRecordDO> listAuditRecords(String taskId) {
        return auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getTaskId, taskId)
                        .eq(AuditRecordDO::getDeleted, 0)
                        .orderByDesc(AuditRecordDO::getId)
        );
    }

    public Map<String, List<AuditRecordDO>> loadAuditRecordMapByTaskIds(Set<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Map.of();
        }
        return auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getDeleted, 0)
                        .in(AuditRecordDO::getTaskId, taskIds)
                        .orderByDesc(AuditRecordDO::getCreatedAt, AuditRecordDO::getId)
        ).stream().collect(Collectors.groupingBy(AuditRecordDO::getTaskId));
    }
}
