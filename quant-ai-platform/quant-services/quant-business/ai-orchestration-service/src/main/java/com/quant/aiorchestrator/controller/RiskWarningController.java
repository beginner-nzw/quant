package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.RiskWarningPageVO;
import com.quant.aiorchestrator.domain.vo.RiskWarningStatsVO;
import com.quant.aiorchestrator.service.RiskQueryService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class RiskWarningController {

    private final RiskQueryService riskQueryService;

    @GetMapping("/risk-warnings")
    public Result<RiskWarningPageVO> pageRiskWarnings(RiskWarningPageQueryDTO queryDTO) {
        return Result.success(riskQueryService.pageRiskWarnings(queryDTO));
    }

    @GetMapping("/risk-warning-stats")
    public Result<RiskWarningStatsVO> getRiskWarningStats() {
        return Result.success(riskQueryService.getRiskWarningStats());
    }
}
