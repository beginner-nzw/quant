package com.quant.aiorchestrator.controller;

import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.report.ReportDomainService;
import com.quant.config.api.RoleAccessPermissions;
import com.quant.config.port.RoleAccessPermissionPort;
import com.quant.common.core.model.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Deprecated(since = "phase-014", forRemoval = false)
public class ReportController {

    private final ReportDomainService reportDomainService;
    private final RoleAccessPermissionPort roleAccessPermissionPort;

    @GetMapping("/report-center")
    public Result<ReportCenterPageVO> pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return Result.success(reportDomainService.pageReportCenter(queryDTO));
    }

    @GetMapping("/report-center-stats")
    public Result<ReportCenterStatsVO> getReportCenterStats() {
        return Result.success(reportDomainService.getReportCenterStats());
    }

    @GetMapping("/{taskId}/report")
    public Result<TaskReportVO> getTaskReport(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.getTaskReport(taskId));
    }

    @GetMapping("/{taskId}/report/versions")
    public Result<List<ReportVersionVO>> listReportVersions(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.listReportVersions(taskId));
    }

    @GetMapping("/{taskId}/report/versions/compare")
    public Result<ReportVersionCompareVO> compareReportVersions(@PathVariable("taskId") String taskId,
                                                                @RequestParam("fromVersionNo") Integer fromVersionNo,
                                                                @RequestParam("toVersionNo") Integer toVersionNo) {
        return Result.success(reportDomainService.compareReportVersions(taskId, fromVersionNo, toVersionNo));
    }

    @GetMapping("/{taskId}/report/versions/{versionNo}")
    public Result<ReportVersionVO> getReportVersion(@PathVariable("taskId") String taskId,
                                                    @PathVariable("versionNo") Integer versionNo) {
        return Result.success(reportDomainService.getReportVersion(taskId, versionNo));
    }

    @GetMapping("/{taskId}/report/review-logs")
    public Result<List<TaskReportReviewLogVO>> listReportReviewLogs(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.listReviewLogs(taskId));
    }

    @PostMapping("/{taskId}/report/review")
    public Result<String> reviewReport(@PathVariable("taskId") String taskId,
                                       @RequestBody TaskReportReviewDTO dto) {
        roleAccessPermissionPort.requirePermission(RoleAccessPermissions.REPORT_REVIEW);
        return Result.success(reportDomainService.reviewReport(taskId, dto));
    }

    @GetMapping("/report-review-stats")
    public Result<ReportReviewStatsVO> getReportReviewStats() {
        return Result.success(reportDomainService.getReportReviewStats());
    }
}
