package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.vo.AuditCompliancePageVO;
import com.quant.aiorchestrator.domain.vo.AuditComplianceStatsVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.ModelAgentConfigCenterVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;

import java.util.List;

public interface AuditConfigDashboardQueryService {

    AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO);

    AuditComplianceStatsVO getAuditComplianceStats();

    ModelAgentConfigCenterVO getModelAgentConfigCenter();

    List<RoleAccessConfigItemVO> listRoleAccessConfigs();

    List<EventSourceConfigItemVO> listMarketEventSourceConfigs();

    ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO);
}
