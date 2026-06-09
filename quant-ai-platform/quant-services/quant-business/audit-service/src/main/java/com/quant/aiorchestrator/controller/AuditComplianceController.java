package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.service.AuditComplianceQueryService;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class AuditComplianceController {

    private final AuditComplianceQueryService auditComplianceQueryService;
    private final RoleAccessPermissionPort roleAccessPermissionPort;

    @GetMapping("/audit-compliance")
    public Result<AuditCompliancePageVO> pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.AUDIT_COMPLIANCE_VIEW);
        return Result.success(auditComplianceQueryService.pageAuditCompliance(queryDTO));
    }

    @GetMapping("/audit-compliance-stats")
    public Result<AuditComplianceStatsVO> getAuditComplianceStats() {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.AUDIT_COMPLIANCE_VIEW);
        return Result.success(auditComplianceQueryService.getAuditComplianceStats());
    }
}
