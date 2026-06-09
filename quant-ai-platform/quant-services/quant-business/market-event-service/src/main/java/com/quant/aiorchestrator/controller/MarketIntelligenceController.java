package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.service.MarketIntelligenceQueryService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class MarketIntelligenceController {

    private final MarketIntelligenceQueryService marketIntelligenceQueryService;

    @GetMapping("/market-intelligence")
    public Result<MarketIntelligencePageVO> pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO) {
        return Result.success(marketIntelligenceQueryService.pageMarketIntelligence(queryDTO));
    }

    @GetMapping("/market-intelligence-stats")
    public Result<MarketIntelligenceStatsVO> getMarketIntelligenceStats() {
        return Result.success(marketIntelligenceQueryService.getMarketIntelligenceStats());
    }
}
