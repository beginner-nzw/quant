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

import java.util.List;

public interface ReportDomainService {

    ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO);

    ReportCenterStatsVO getReportCenterStats();

    ReportReviewStatsVO getReportReviewStats();

    TaskReportVO getTaskReport(String taskId);

    List<ReportVersionVO> listReportVersions(String taskId);

    ReportVersionVO getReportVersion(String taskId, Integer versionNo);

    ReportVersionCompareVO compareReportVersions(String taskId, Integer fromVersionNo, Integer toVersionNo);

    List<TaskReportReviewLogVO> listReviewLogs(String taskId);

    String reviewReport(String taskId, TaskReportReviewDTO dto);
}
