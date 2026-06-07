package com.quant.aiorchestrator.report;

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
import com.quant.aiorchestrator.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportDomainServiceImpl implements ReportDomainService {

    private final ReportQueryService reportQueryService;
    private final TaskReportService taskReportService;

    @Override
    public ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return reportQueryService.pageReportCenter(queryDTO);
    }

    @Override
    public ReportCenterStatsVO getReportCenterStats() {
        return reportQueryService.getReportCenterStats();
    }

    @Override
    public ReportReviewStatsVO getReportReviewStats() {
        return reportQueryService.getReportReviewStats();
    }

    @Override
    public TaskReportVO getTaskReport(String taskId) {
        return reportQueryService.getTaskReportOnly(taskId);
    }

    @Override
    public List<ReportVersionVO> listReportVersions(String taskId) {
        return reportQueryService.listReportVersions(taskId);
    }

    @Override
    public ReportVersionVO getReportVersion(String taskId, Integer versionNo) {
        return reportQueryService.getReportVersion(taskId, versionNo);
    }

    @Override
    public ReportVersionCompareVO compareReportVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        return reportQueryService.compareReportVersions(taskId, fromVersionNo, toVersionNo);
    }

    @Override
    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return reportQueryService.listReviewLogs(taskId);
    }

    @Override
    public String reviewReport(String taskId, TaskReportReviewDTO dto) {
        return taskReportService.reviewReport(taskId, dto);
    }
}
