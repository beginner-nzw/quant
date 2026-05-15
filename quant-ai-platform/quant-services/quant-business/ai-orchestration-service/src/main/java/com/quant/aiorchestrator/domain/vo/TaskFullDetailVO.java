package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskFullDetailVO {
    private TaskDetailVO taskDetail;
    private TaskStateVO taskState;
    private TaskSummaryVO summary;
    private TaskReportVO report;
    private List<TaskStepVO> steps;
    private WorkflowInstanceVO workflow;
    private List<AgentExecutionVO> agents;
    private List<AuditRecordVO> audits;
    private List<TaskRetryLogVO> retries;
}