package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.TaskReportReviewDTO;
import com.quant.aiorchestrator.domain.vo.TaskReportReviewLogVO;
import com.quant.aiorchestrator.manager.TaskReportReviewLogManager;
import com.quant.aiorchestrator.manager.TaskReportReviewManager;
import com.quant.aiorchestrator.service.TaskReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskReportServiceImpl implements TaskReportService {

    private final TaskReportReviewManager taskReportReviewManager;
    private final TaskReportReviewLogManager taskReportReviewLogManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String reviewReport(String taskId, TaskReportReviewDTO dto) {
        return taskReportReviewManager.reviewReport(taskId, dto);
    }

    @Override
    public List<TaskReportReviewLogVO> listReviewLogs(String taskId) {
        return taskReportReviewLogManager.listReviewLogs(taskId);
    }
}
