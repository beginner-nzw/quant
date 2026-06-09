package com.quant.api.task;

import com.quant.aiorchestrator.domain.dto.ResearchWorkbenchQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.AuditRecordVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchDispositionSummaryVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchInsightVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchRecentTaskVO;
import com.quant.aiorchestrator.domain.vo.ResearchWorkbenchVO;
import com.quant.aiorchestrator.domain.vo.TaskDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskListItemVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.aiorchestrator.domain.vo.TaskRetryLogVO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.TaskStatsVO;
import com.quant.aiorchestrator.domain.vo.TaskStepVO;
import com.quant.aiorchestrator.domain.vo.TaskSummaryVO;
import com.quant.aiorchestrator.domain.vo.WorkflowCheckpointVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.service.ResearchWorkbenchQueryService;
import com.quant.aiorchestrator.service.TaskControlService;
import com.quant.aiorchestrator.service.TaskQueryService;
import com.quant.aiorchestrator.service.TaskRetryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskApiBoundaryTests {

    @Test
    void taskApiOwnsLegacyTaskAndWorkbenchRequestModels() {
        assertEquals("com.quant.aiorchestrator.domain.dto", TaskPageQueryDTO.class.getPackageName());
        assertEquals(TaskPageQueryDTO.class.getPackageName(), TaskRetryDTO.class.getPackageName());
        assertEquals(TaskPageQueryDTO.class.getPackageName(), TaskCancelDTO.class.getPackageName());
        assertEquals(TaskPageQueryDTO.class.getPackageName(), TaskWorkflowControlDTO.class.getPackageName());
        assertEquals(TaskPageQueryDTO.class.getPackageName(), ResearchWorkbenchQueryDTO.class.getPackageName());
    }

    @Test
    void taskApiOwnsLegacyTaskAndWorkbenchResponseModels() {
        assertEquals("com.quant.aiorchestrator.domain.vo", TaskDetailVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskStateVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskStepVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskRetryLogVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskListItemVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskPageVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskStatsVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskSummaryVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), TaskFullDetailVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), WorkflowInstanceVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), WorkflowCheckpointVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), AgentExecutionVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), AuditRecordVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), ResearchWorkbenchVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), ResearchWorkbenchRecentTaskVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), ResearchWorkbenchInsightVO.class.getPackageName());
        assertEquals(TaskDetailVO.class.getPackageName(), ResearchWorkbenchDispositionSummaryVO.class.getPackageName());
    }

    @Test
    void taskApiOwnsLegacyTaskAndWorkbenchServiceContracts() {
        assertEquals("com.quant.aiorchestrator.service", TaskQueryService.class.getPackageName());
        assertEquals(TaskQueryService.class.getPackageName(), TaskControlService.class.getPackageName());
        assertEquals(TaskQueryService.class.getPackageName(), TaskRetryService.class.getPackageName());
        assertEquals(TaskQueryService.class.getPackageName(), ResearchWorkbenchQueryService.class.getPackageName());
    }
}
