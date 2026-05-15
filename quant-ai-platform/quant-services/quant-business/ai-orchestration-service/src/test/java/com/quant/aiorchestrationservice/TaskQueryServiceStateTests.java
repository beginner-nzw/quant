package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.manager.TaskCacheVersionManager;
import com.quant.aiorchestrator.manager.TaskStateManager;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import com.quant.aiorchestrator.mapper.HumanReviewRecordMapper;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.mapper.StrategySignalFactorMapper;
import com.quant.aiorchestrator.mapper.StrategySignalMapper;
import com.quant.aiorchestrator.service.AgentConfigService;
import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.ModelStrategyConfigService;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.RoleAccessConfigService;
import com.quant.aiorchestrator.service.WorkflowConfigService;
import com.quant.aiorchestrator.service.impl.TaskQueryServiceImpl;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
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

        AiWorkflowInstanceMapper aiWorkflowInstanceMapper = mock(AiWorkflowInstanceMapper.class);
        AiWorkflowInstanceDO workflow = new AiWorkflowInstanceDO();
        workflow.setWorkflowInstanceId("wf-task-2");
        workflow.setTaskId("task-2");
        workflow.setCurrentNode("planner_agent");
        workflow.setStatus(TaskStatusEnum.RUNNING.name());
        when(aiWorkflowInstanceMapper.selectOne(any())).thenReturn(workflow);

        TaskQueryServiceImpl service = newService(researchTaskMapper, stringRedisTemplate, aiWorkflowInstanceMapper);

        WorkflowInstanceVO vo = service.getWorkflowInstance("task-2");

        assertNotNull(vo);
        assertEquals(TaskStatusEnum.SUCCESS.name(), vo.getStatus());
        assertEquals("FINISHED", vo.getCurrentNode());
        assertEquals(task.getFinishTime(), vo.getFinishTime());
    }

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
                                            StringRedisTemplate stringRedisTemplate) {
        return newService(researchTaskMapper, stringRedisTemplate, mock(AiWorkflowInstanceMapper.class));
    }

    private TaskQueryServiceImpl newService(ResearchTaskMapper researchTaskMapper,
                                            StringRedisTemplate stringRedisTemplate,
                                            AiWorkflowInstanceMapper aiWorkflowInstanceMapper) {
        return new TaskQueryServiceImpl(
                researchTaskMapper,
                mock(ResearchTaskStepMapper.class),
                aiWorkflowInstanceMapper,
                mock(AiAgentExecutionMapper.class),
                mock(AuditRecordMapper.class),
                stringRedisTemplate,
                new ObjectMapper(),
                mock(ResearchTaskRetryLogMapper.class),
                mock(TaskCacheVersionManager.class),
                mock(ResearchReportMapper.class),
                mock(AgentConfigService.class),
                mock(ConfigChangeAuditService.class),
                mock(EventAutoTriggerConfigService.class),
                mock(MarketEventIngestHistoryService.class),
                mock(EventSourceConfigService.class),
                mock(ModelStrategyConfigService.class),
                mock(PromptTemplateConfigService.class),
                mock(WorkflowConfigService.class),
                mock(RoleAccessConfigService.class),
                mock(RiskWarningMapper.class),
                mock(RiskWarningDetailMapper.class),
                mock(StrategySignalMapper.class),
                mock(StrategySignalFactorMapper.class),
                mock(ReportEvidenceRefMapper.class),
                mock(HumanReviewRecordMapper.class),
                mock(ResearchReportSectionMapper.class),
                new TaskStateManager()
        );
    }
}
