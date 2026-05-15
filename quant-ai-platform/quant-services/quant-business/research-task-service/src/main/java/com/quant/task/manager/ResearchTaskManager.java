package com.quant.task.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.web.TraceContext;
import com.quant.task.domain.dto.CreateResearchTaskDTO;
import com.quant.task.domain.entity.ResearchTaskDO;
import com.quant.task.domain.entity.ResearchTaskStepDO;
import com.quant.task.mapper.ResearchTaskMapper;
import com.quant.task.mapper.ResearchTaskStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResearchTaskManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskStepMapper researchTaskStepMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public ResearchTaskDO createTask(CreateResearchTaskDTO dto) {
        String taskId = UUID.randomUUID().toString();
        String traceId = TraceContext.resolveTraceId(TraceContext.currentTraceId());

        ResearchTaskDO task = new ResearchTaskDO();
        task.setTaskId(taskId);
        task.setTaskType(dto.getTaskType());
        task.setTaskTitle(dto.getTaskTitle());
        task.setTenantId("default");
        task.setTargetType(dto.getTargetType());
        task.setTargetCode(dto.getTargetCode());
        task.setTargetName(dto.getTargetName());
        task.setPriority(dto.getPriority());
        task.setStatus(TaskStatusEnum.INIT.name());
        task.setCurrentStage(TaskStageEnum.INIT.name());
        task.setSourceChannel(StringUtils.hasText(dto.getSourceChannel()) ? dto.getSourceChannel().trim() : "WEB");
        task.setTraceId(traceId);
        task.setSourceTaskId(dto.getSourceTaskId());
        task.setSourceReportId(dto.getSourceReportId());
        task.setSourceEventId(dto.getSourceEventId());
        task.setSourceDomain(dto.getSourceDomain());
        task.setSourceReviewStatus(dto.getSourceReviewStatus());
        task.setAnalysisScope(dto.getAnalysisScope());
        task.setRetryCount(0);
        task.setDeleted(0);
        task.setStartTime(LocalDateTime.now());

        try {
            task.setRequestPayload(objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("serialize request payload failed", e);
        }

        researchTaskMapper.insert(task);
        insertInitStep(task);

        writeTaskMetaCache(task);
        writeTaskStateCache(taskId, TaskStatusEnum.INIT.name(), TaskStageEnum.INIT.name(), 0);

        return task;
    }

    private void insertInitStep(ResearchTaskDO task) {
        ResearchTaskStepDO step = new ResearchTaskStepDO();
        step.setTaskId(task.getTaskId());
        step.setStepCode(TaskStageEnum.INIT.name());
        step.setStepName("任务初始化");
        step.setAgentCode("system");
        step.setExecutionOrder(0);
        step.setStatus(TaskStatusEnum.SUCCESS.name());
        step.setStartTime(LocalDateTime.now());
        step.setFinishTime(LocalDateTime.now());
        step.setDurationMs(0L);
        step.setDeleted(0);
        researchTaskStepMapper.insert(step);
    }

    public void writeTaskMetaCache(ResearchTaskDO task) {
        try {
            stringRedisTemplate.opsForValue().set(
                    RedisKeyBuilder.taskMeta(task.getTaskId()),
                    objectMapper.writeValueAsString(task),
                    Duration.ofHours(24)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("write task meta cache failed", e);
        }
    }

    public void writeTaskStateCache(String taskId, String status, String stage, int progress) {
        String json = """
                {"status":"%s","currentStage":"%s","progress":%d}
                """.formatted(status, stage, progress);
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskState(taskId),
                json,
                Duration.ofHours(24)
        );
    }
}
