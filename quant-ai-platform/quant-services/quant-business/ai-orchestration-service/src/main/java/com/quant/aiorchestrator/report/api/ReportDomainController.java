package com.quant.aiorchestrator.report.api;

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
import com.quant.aiorchestrator.service.RoleAccessConfigService;
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
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportDomainController {

    private final ReportDomainService reportDomainService;
    private final RoleAccessConfigService roleAccessConfigService;

    @GetMapping("/center")
    public Result<ReportCenterPageVO> pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return Result.success(reportDomainService.pageReportCenter(queryDTO));
    }

    @GetMapping("/center/stats")
    public Result<ReportCenterStatsVO> getReportCenterStats() {
        return Result.success(reportDomainService.getReportCenterStats());
    }

    @GetMapping("/tasks/{taskId}")
    public Result<TaskReportVO> getTaskReport(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.getTaskReport(taskId));
    }

    @GetMapping("/tasks/{taskId}/versions")
    public Result<List<ReportVersionVO>> listReportVersions(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.listReportVersions(taskId));
    }

    @GetMapping("/tasks/{taskId}/versions/compare")
    public Result<ReportVersionCompareVO> compareReportVersions(@PathVariable("taskId") String taskId,
                                                                @RequestParam("fromVersionNo") Integer fromVersionNo,
                                                                @RequestParam("toVersionNo") Integer toVersionNo) {
        return Result.success(reportDomainService.compareReportVersions(taskId, fromVersionNo, toVersionNo));
    }

    @GetMapping("/tasks/{taskId}/versions/{versionNo}")
    public Result<ReportVersionVO> getReportVersion(@PathVariable("taskId") String taskId,
                                                    @PathVariable("versionNo") Integer versionNo) {
        return Result.success(reportDomainService.getReportVersion(taskId, versionNo));
    }

    @GetMapping("/tasks/{taskId}/review-logs")
    public Result<List<TaskReportReviewLogVO>> listReportReviewLogs(@PathVariable("taskId") String taskId) {
        return Result.success(reportDomainService.listReviewLogs(taskId));
    }

    @PostMapping("/tasks/{taskId}/review")
    public Result<String> reviewReport(@PathVariable("taskId") String taskId,
                                       @RequestBody TaskReportReviewDTO dto) {
        roleAccessConfigService.requirePermission(RoleAccessConfigService.PERMISSION_REPORT_REVIEW);
        return Result.success(reportDomainService.reviewReport(taskId, dto));
    }

    @GetMapping("/review/stats")
    public Result<ReportReviewStatsVO> getReportReviewStats() {
        return Result.success(reportDomainService.getReportReviewStats());
    }
}
