package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;

public interface AuditComplianceQueryService {
    AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO);

    AuditComplianceStatsVO getAuditComplianceStats();
}
