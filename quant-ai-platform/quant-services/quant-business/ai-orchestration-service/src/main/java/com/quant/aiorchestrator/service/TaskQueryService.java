package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.AuditCompliancePageQueryDTO;
import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.ReportCenterPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.RiskWarningPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.StrategySignalPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.*;

import java.util.List;

public interface TaskQueryService {
    TaskDetailVO getTaskDetail(String taskId);

    TaskStateVO getTaskState(String taskId);

    List<TaskStepVO> listTaskSteps(String taskId);

    WorkflowInstanceVO getWorkflowInstance(String taskId);

    List<AgentExecutionVO> listAgentExecutions(String taskId);

    List<AuditRecordVO> listAuditRecords(String taskId);

    TaskPageVO pageTasks(TaskPageQueryDTO queryDTO);

    List<TaskRetryLogVO> listRetryLogs(String taskId);

    TaskFullDetailVO getTaskFullDetail(String taskId);

    TaskStatsVO getTaskStats();

    RiskWarningPageVO pageRiskWarnings(RiskWarningPageQueryDTO queryDTO);

    RiskWarningStatsVO getRiskWarningStats();

    StrategySignalPageVO pageStrategySignals(StrategySignalPageQueryDTO queryDTO);

    StrategySignalStatsVO getStrategySignalStats();

    ReportCenterPageVO pageReportCenter(ReportCenterPageQueryDTO queryDTO);

    ReportCenterStatsVO getReportCenterStats();

    MarketIntelligencePageVO pageMarketIntelligence(MarketIntelligencePageQueryDTO queryDTO);

    MarketIntelligenceStatsVO getMarketIntelligenceStats();

    AuditCompliancePageVO pageAuditCompliance(AuditCompliancePageQueryDTO queryDTO);

    AuditComplianceStatsVO getAuditComplianceStats();

    ModelAgentConfigCenterVO getModelAgentConfigCenter();

    ResearchWorkbenchVO getResearchWorkbench(ResearchWorkbenchQueryDTO queryDTO);

    TaskReportVO getTaskReportOnly(String taskId);

    ReportReviewStatsVO getReportReviewStats();
}
