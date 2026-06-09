package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.AuditRecordVO;

import java.util.List;

public interface TaskAuditProjectionProvider {
    List<AuditRecordVO> listAuditRecords(String taskId);
}
