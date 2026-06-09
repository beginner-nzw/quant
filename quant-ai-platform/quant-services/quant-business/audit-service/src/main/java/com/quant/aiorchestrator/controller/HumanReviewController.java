package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.HumanReviewDecisionDTO;
import com.quant.aiorchestrator.domain.dto.HumanReviewQueueQueryDTO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueuePageVO;
import com.quant.aiorchestrator.domain.vo.HumanReviewQueueStatsVO;
import com.quant.aiorchestrator.service.HumanReviewService;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/human-reviews")
@RequiredArgsConstructor
public class HumanReviewController {

    private final HumanReviewService humanReviewService;
    private final RoleAccessPermissionPort roleAccessPermissionPort;

    @GetMapping
    public Result<HumanReviewQueuePageVO> pageQueue(HumanReviewQueueQueryDTO queryDTO) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.REPORT_REVIEW);
        return Result.success(humanReviewService.pageQueue(queryDTO));
    }

    @GetMapping("/stats")
    public Result<HumanReviewQueueStatsVO> getStats() {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.REPORT_REVIEW);
        return Result.success(humanReviewService.getStats());
    }

    @PostMapping("/{queueId}/decision")
    public Result<String> decide(@PathVariable("queueId") String queueId,
                                 @RequestBody HumanReviewDecisionDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.REPORT_REVIEW);
        return Result.success(humanReviewService.decide(queueId, dto));
    }
}
