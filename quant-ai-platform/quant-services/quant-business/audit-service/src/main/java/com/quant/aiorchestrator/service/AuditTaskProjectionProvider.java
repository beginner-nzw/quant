package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.manager.AuditTaskReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditTaskProjectionProvider implements TaskAuditProjectionProvider {

    private final AuditTaskReadManager auditTaskReadManager;

    public List<AuditRecordVO> listAuditRecords(String taskId) {
        return auditTaskReadManager.listAuditRecords(taskId).stream()
                .map(this::toAuditRecordVO)
                .toList();
    }

    private AuditRecordVO toAuditRecordVO(AuditRecordDO entity) {
        AuditRecordVO vo = new AuditRecordVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
