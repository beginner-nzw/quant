package com.quant.aiorchestrator.report;

import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;

public interface ReportReviewCommand {

    String reviewReport(String taskId, TaskReportReviewDTO dto);
}
