package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportQueryServiceImpl implements ReportQueryService {

    private final TaskQueryService taskQueryService;
    private final TaskReportService taskReportService;
    private final ReportVersionService reportVersionService;

    @Override
    public ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO) {
        return taskQueryService.pageReportCenter(queryDTO);
    }

    @Override
    public ReportCenterStatsVO getReportCenterStats() {
        return taskQueryService.getReportCenterStats();
    }

    @Override
    public TaskReportVO getTaskReportOnly(String taskId) {
        return taskQueryService.getTaskReportOnly(taskId);
    }

    @Override
    public ReportReviewStatsVO getReportReviewStats() {
        return taskQueryService.getReportReviewStats();
    }

    @Override
    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return taskReportService.listReviewLogs(taskId);
    }

    @Override
    public List<ReportVersionVO> listReportVersions(String taskId) {
        return reportVersionService.listVersions(taskId);
    }

    @Override
    public ReportVersionVO getReportVersion(String taskId, Integer versionNo) {
        return reportVersionService.getVersion(taskId, versionNo);
    }

    @Override
    public ReportVersionCompareVO compareReportVersions(String taskId, Integer fromVersionNo, Integer toVersionNo) {
        return reportVersionService.compareVersions(taskId, fromVersionNo, toVersionNo);
    }
}
