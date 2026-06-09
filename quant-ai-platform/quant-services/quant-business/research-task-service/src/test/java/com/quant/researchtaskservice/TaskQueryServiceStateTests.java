package com.quant.researchtaskservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.manager.TaskFullDetailProjectionManager;
import com.quant.aiorchestrator.manager.TaskPageItemAssembler;
import com.quant.aiorchestrator.manager.TaskPageProjectionManager;
import com.quant.aiorchestrator.manager.TaskQueryItemAssembler;
import com.quant.aiorchestrator.manager.TaskQueryProjectionManager;
import com.quant.aiorchestrator.manager.TaskQueryReadManager;
import com.quant.aiorchestrator.manager.TaskStateProjectionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.report.TaskReportReadPort;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.aiorchestrator.service.TaskAuditProjectionProvider;
import com.quant.aiorchestrator.service.TaskWorkflowProjectionProvider;
import com.quant.aiorchestrator.service.impl.TaskQueryServiceImpl;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.task.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStatsManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskQueryServiceStateTests {

    @SuppressWarnings("unchecked")
    @Test
    void returnsMysqlFinalStateWhenRedisStateIsStale() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeyBuilder.taskState("task-1")))
                .thenReturn("{\"status\":\"RUNNING\",\"currentStage\":\"REPORT_GENERATION\",\"progress\":95}");

        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-1");
        task.setStatus(TaskStatusEnum.SUCCESS.name());
        task.setCurrentStage("FINISHED");
        when(researchTaskMapper.selectOne(any())).thenReturn(task);

        TaskQueryServiceImpl service = newService(researchTaskMapper, stringRedisTemplate);

        TaskStateVO vo = service.getTaskState("task-1");

        assertEquals(TaskStatusEnum.SUCCESS.name(), vo.getStatus());
        assertEquals("FINISHED", vo.getCurrentStage());
        assertEquals(100, vo.getProgress());
        assertEquals("mysql", vo.getSource());
        verify(valueOperations).set(
                eq(RedisKeyBuilder.taskState("task-1")),
                contains("\"status\":\"SUCCESS\""),
                eq(Duration.ofHours(24))
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsFinalWorkflowViewWhenTaskAlreadyFinished() {
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId("task-2");
        task.setStatus(TaskStatusEnum.SUCCESS.name());
        task.setCurrentStage("FINISHED");
        task.setFinishTime(LocalDateTime.of(2026, 5, 7, 11, 30, 0));
        when(researchTaskMapper.selectOne(any())).thenReturn(task);

        TaskWorkflowProjectionProvider workflowProjectionProvider = mock(TaskWorkflowProjectionProvider.class);
        WorkflowInstanceVO workflow = new WorkflowInstanceVO();
        workflow.setWorkflowInstanceId("wf-task-2");
        workflow.setTaskId("task-2");
        workflow.setCurrentNode("planner_agent");
        workflow.setStatus(TaskStatusEnum.RUNNING.name());
        when(workflowProjectionProvider.getWorkflowInstance("task-2")).thenReturn(workflow);

        TaskQueryServiceImpl service = newService(researchTaskMapper, stringRedisTemplate, workflowProjectionProvider);

        WorkflowInstanceVO vo = service.getWorkflowInstance("task-2");

        assertNotNull(vo);
        assertEquals(TaskStatusEnum.SUCCESS.name(), vo.getStatus());
        assertEquals("FINISHED", vo.getCurrentNode());
        assertEquals(task.getFinishTime(), vo.getFinishTime());
    }

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
                                            StringRedisTemplate stringRedisTemplate) {
        return newService(researchTaskMapper, stringRedisTemplate, mock(TaskWorkflowProjectionProvider.class));
    }

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
                                            StringRedisTemplate stringRedisTemplate,
                                            TaskWorkflowProjectionProvider workflowProjectionProvider) {
        ObjectMapper objectMapper = new ObjectMapper();
        TaskQueryReadManager readManager = new TaskQueryReadManager(
                researchTaskMapper,
                mock(ResearchTaskStepMapper.class),
                mock(ResearchTaskRetryLogMapper.class)
        );
        TaskStateManager taskStateManager = new TaskStateManager();
        TaskQueryProjectionManager taskQueryProjectionManager = new TaskQueryProjectionManager(
                stringRedisTemplate,
                objectMapper,
                mock(TaskCacheVersionManager.class),
                taskStateManager,
                mock(ReportQueryService.class),
                readManager,
                workflowProjectionProvider,
                mock(TaskAuditProjectionProvider.class),
                new TaskFullDetailProjectionManager(stringRedisTemplate, objectMapper, mock(ReportQueryService.class)),
                new TaskQueryItemAssembler(objectMapper),
                new TaskStateProjectionManager(stringRedisTemplate, objectMapper, taskStateManager, researchTaskMapper),
                new TaskPageProjectionManager(
                        stringRedisTemplate,
                        objectMapper,
                        mock(TaskCacheVersionManager.class),
                        readManager,
                        mock(TaskReportReadPort.class),
                        new TaskPageItemAssembler(objectMapper)
                ),
                new TaskStatsManager(researchTaskMapper, stringRedisTemplate, objectMapper)
        );
        return new TaskQueryServiceImpl(taskQueryProjectionManager);
    }
}
