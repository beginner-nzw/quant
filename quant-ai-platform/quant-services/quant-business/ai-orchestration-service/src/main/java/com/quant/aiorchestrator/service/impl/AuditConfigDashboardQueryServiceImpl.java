package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditConfigDashboardQueryServiceImpl implements AuditConfigDashboardQueryService {

    private final TaskQueryService taskQueryService;
    private final RoleAccessConfigService roleAccessConfigService;
    private final EventSourceConfigService eventSourceConfigService;

    @Override
    public AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO) {
        return taskQueryService.pageAuditCompliance(queryDTO);
    }

    @Override
    public AuditComplianceStatsVO getAuditComplianceStats() {
        return taskQueryService.getAuditComplianceStats();
    }

    @Override
    public ModelAgentConfigCenterVO getModelAgentConfigCenter() {
        return taskQueryService.getModelAgentConfigCenter();
    }

    @Override
    public List<RoleAccessConfigItemVO> listRoleAccessConfigs() {
        return roleAccessConfigService.loadRoles();
    }

    @Override
    public List<EventSourceConfigItemVO> listMarketEventSourceConfigs() {
        return eventSourceConfigService.loadSources();
    }

    @Override
    public ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO) {
        return taskQueryService.getResearchWorkbench(queryDTO);
    }
}
