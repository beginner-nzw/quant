package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.StrategySignalCreateDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalStatusUpdateDTO;
import com.quant.aiorchestrator.domain.vo.StrategySignalFactorItemVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalPageVO;
import com.quant.aiorchestrator.domain.vo.StrategySignalStatsVO;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.StrategyQueryService;
import com.quant.aiorchestrator.service.StrategySignalService;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class StrategySignalController {

    private final StrategyQueryService strategyQueryService;
    private final StrategySignalService strategySignalService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/strategy-signals")
    public Result<StrategySignalPageVO> pageStrategySignals(StrategySignalPageQueryDTO queryDTO) {
        return Result.success(strategyQueryService.pageStrategySignals(queryDTO));
    }

    @GetMapping("/strategy-signal-stats")
    public Result<StrategySignalStatsVO> getStrategySignalStats() {
        return Result.success(strategyQueryService.getStrategySignalStats());
    }

    @PostMapping("/strategy-signals")
    public Result<String> createStrategySignal(@RequestBody StrategySignalCreateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(strategySignalService.createOrUpdate(dto));
    }

    @GetMapping("/strategy-signals/{signalId}/factors")
    public Result<List<StrategySignalFactorItemVO>> listStrategySignalFactors(@PathVariable("signalId") String signalId) {
        return Result.success(strategyQueryService.listStrategySignalFactors(signalId));
    }

    @PostMapping("/strategy-signals/{signalId}/status")
    public Result<String> updateStrategySignalStatus(@PathVariable("signalId") String signalId,
                                                     @RequestBody StrategySignalStatusUpdateDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(strategySignalService.updateStatus(signalId, dto));
    }
}
