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
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.TaskReportService;
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
public class ReportController {

    private final ReportQueryService reportQueryService;
    private final TaskReportService taskReportService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/report-center")
    public Result<ReportCenterPageVO> pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return Result.success(reportQueryService.pageReportCenter(queryDTO));
    }

    @GetMapping("/report-center-stats")
    public Result<ReportCenterStatsVO> getReportCenterStats() {
        return Result.success(reportQueryService.getReportCenterStats());
    }

    @GetMapping("/{taskId}/report")
    public Result<TaskReportVO> getTaskReport(@PathVariable("taskId") String taskId) {
        return Result.success(reportQueryService.getTaskReportOnly(taskId));
    }

    @GetMapping("/{taskId}/report/versions")
    public Result<List<ReportVersionVO>> listReportVersions(@PathVariable("taskId") String taskId) {
        return Result.success(reportQueryService.listReportVersions(taskId));
    }

    @GetMapping("/{taskId}/report/versions/compare")
    public Result<ReportVersionCompareVO> compareReportVersions(@PathVariable("taskId") String taskId,
                                                                @RequestParam("fromVersionNo") Integer fromVersionNo,
                                                                @RequestParam("toVersionNo") Integer toVersionNo) {
        return Result.success(reportQueryService.compareReportVersions(taskId, fromVersionNo, toVersionNo));
    }

    @GetMapping("/{taskId}/report/versions/{versionNo}")
    public Result<ReportVersionVO> getReportVersion(@PathVariable("taskId") String taskId,
                                                    @PathVariable("versionNo") Integer versionNo) {
        return Result.success(reportQueryService.getReportVersion(taskId, versionNo));
    }

    @GetMapping("/{taskId}/report/review-logs")
    public Result<List<TaskReportReviewLogVO>> listReportReviewLogs(@PathVariable("taskId") String taskId) {
        return Result.success(reportQueryService.listReviewLogs(taskId));
    }

    @PostMapping("/{taskId}/report/review")
    public Result<String> reviewReport(@PathVariable("taskId") String taskId,
                                       @RequestBody TaskReportReviewDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(taskReportService.reviewReport(taskId, dto));
    }

    @GetMapping("/report-review-stats")
    public Result<ReportReviewStatsVO> getReportReviewStats() {
        return Result.success(reportQueryService.getReportReviewStats());
    }
}
