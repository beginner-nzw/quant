package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import com.quant.aiorchestrator.service.AuditConfigDashboardQueryService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(MarketDataIngestStableContract.LEGACY_TASK_API_BASE)
@RequiredArgsConstructor
public class MarketEventSourceConfigController {

    private final AuditConfigDashboardQueryService auditConfigDashboardQueryService;

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_SOURCE_CONFIGS)
    public Result<List<EventSourceConfigItemVO>> listMarketEventSourceConfigs() {
        return Result.success(auditConfigDashboardQueryService.listMarketEventSourceConfigs());
    }
}
