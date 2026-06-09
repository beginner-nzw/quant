package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.RoleAccessConfigItemVO;

import java.util.List;

public interface AuditConfigDashboardQueryService {

    List<RoleAccessConfigItemVO> listRoleAccessConfigs();

    List<EventSourceConfigItemVO> listMarketEventSourceConfigs();
}
