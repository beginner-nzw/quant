package com.quant.aiorchestrator.report;

import com.quant.aiorchestrator.domain.vo.TaskReportVO;

public interface TaskReportProjectionProvider {
    TaskReportVO getTaskReportOnly(String taskId);
}
