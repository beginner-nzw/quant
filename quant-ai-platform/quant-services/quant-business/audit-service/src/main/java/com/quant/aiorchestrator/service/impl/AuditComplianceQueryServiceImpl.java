package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.service.AuditComplianceProjectionProvider;
import com.quant.aiorchestrator.service.AuditComplianceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditComplianceQueryServiceImpl implements AuditComplianceQueryService {

    private final AuditComplianceProjectionProvider auditComplianceProjectionProvider;

    @Override
    public AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        return auditComplianceProjectionProvider.pageAuditCompliance(queryDTO);
    }

    @Override
    public AuditComplianceStatsVO getAuditComplianceStats() {
        return auditComplianceProjectionProvider.getAuditComplianceStats();
    }
}
