package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;

import java.util.List;

public interface TaskReportService {
    String reviewReport(String taskId, TaskReportReviewDTO dto);

    List<TaskReportReviewLogVO> listReviewLogs(String taskId);
}