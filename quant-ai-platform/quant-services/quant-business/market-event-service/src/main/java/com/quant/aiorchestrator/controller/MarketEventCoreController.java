package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.manager.MarketEventCommandManager;
import com.quant.aiorchestrator.manager.MarketEventQueryManager;
import com.quant.aiorchestrator.manager.MarketEventStatsManager;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import com.quant.common.core.model.Result;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MarketDataIngestStableContract.LEGACY_TASK_API_BASE)
@RequiredArgsConstructor
public class MarketEventCoreController {

    private final MarketEventQueryManager marketEventQueryManager;
    private final MarketEventStatsManager marketEventStatsManager;
    private final MarketEventCommandManager marketEventCommandManager;
    private final RoleAccessPermissionPort roleAccessPermissionPort;

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENTS)
    public Result<MarketEventPageVO> pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        return Result.success(marketEventQueryManager.pageMarketEvents(queryDTO));
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_STATS)
    public Result<MarketEventStatsVO> getMarketEventStats() {
        return Result.success(marketEventStatsManager.getMarketEventStats());
    }

    @GetMapping(MarketDataIngestStableContract.MARKET_EVENT_DETAIL)
    public Result<MarketEventListItemVO> getMarketEvent(@PathVariable("eventId") String eventId) {
        return Result.success(marketEventQueryManager.getMarketEvent(eventId));
    }

    @PostMapping(MarketDataIngestStableContract.MARKET_EVENTS)
    public Result<MarketEventCreateResultVO> createMarketEvent(@RequestBody MarketEventCreateDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.TASK_CREATE);
        return Result.success(marketEventCommandManager.createMarketEvent(dto, true));
    }
}
