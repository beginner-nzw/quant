package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.ReportCenterPageVO;
import com.quant.aiorchestrator.domain.vo.ReportCenterStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportReviewStatsVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
import com.quant.aiorchestrator.domain.vo.ReportVersionVO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.domain.vo.TaskReportVO;

import java.util.List;

public interface ReportQueryService {
    ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO);

    ReportCenterStatsVO getReportCenterStats();

    TaskReportVO getTaskReportOnly(String taskId);

    ReportReviewStatsVO getReportReviewStats();

    List<TaskReportReviewLogVO> listReviewLogs(String taskId);

    List<ReportVersionVO> listReportVersions(String taskId);

    ReportVersionVO getReportVersion(String taskId, Integer versionNo);

    ReportVersionCompareVO compareReportVersions(String taskId, Integer fromVersionNo, Integer toVersionNo);
}
