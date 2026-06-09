package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditConfigDashboardQueryServiceImpl implements AuditConfigDashboardQueryService {

    private final RoleAccessConfigService roleAccessConfigService;
    private final EventSourceConfigService eventSourceConfigService;

    @Override
    public List<RoleAccessConfigItemVO> listRoleAccessConfigs() {
        return roleAccessConfigService.loadRoles();
    }

    @Override
    public List<EventSourceConfigItemVO> listMarketEventSourceConfigs() {
        return eventSourceConfigService.loadSources();
    }
}
