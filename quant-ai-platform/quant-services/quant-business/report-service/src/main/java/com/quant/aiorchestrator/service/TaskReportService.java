package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.report.ReportReviewCommand;

import java.util.List;

public interface TaskReportService extends ReportReviewCommand {

    List<TaskReportReviewLogVO> listReviewLogs(String taskId);
}
