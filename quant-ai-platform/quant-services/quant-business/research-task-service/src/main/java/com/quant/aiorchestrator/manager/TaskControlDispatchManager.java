package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import com.quant.common.messaging.MessageTypeConstants;
import com.quant.common.model.message.AiTaskActorProvenanceSupport;
import com.quant.common.model.message.AiTaskDispatchMessage;
import com.quant.task.service.TaskMessageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskControlDispatchManager {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TaskMessageLogService taskMessageLogService;

    public void dispatchWorkflowControl(ResearchTaskDO task, TaskWorkflowControlDTO dto) {
        AiTaskDispatchMessage message = buildDispatchMessage(task, dto);
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopicConstants.AI_TASK_DISPATCH, task.getTaskId(), messageJson);
            taskMessageLogService.recordProduced(KafkaTopicConstants.AI_TASK_DISPATCH, message);
        } catch (Exception e) {
            taskMessageLogService.recordFailed(KafkaTopicConstants.AI_TASK_DISPATCH, message, e.getMessage());
            throw new BizException("WORKFLOW_CONTROL_DISPATCH_FAILED", "工作流控制派发失败: " + e.getMessage());
        }
    }

    private AiTaskDispatchMessage buildDispatchMessage(ResearchTaskDO task, TaskWorkflowControlDTO dto) {
        AiTaskDispatchMessage.AiTaskDispatchPayload payload = new AiTaskDispatchMessage.AiTaskDispatchPayload();
        payload.setTaskType(task.getTaskType());
        payload.setTaskTitle(task.getTaskTitle());
        payload.setTargetType(task.getTargetType());
        payload.setTargetCode(task.getTargetCode());
        payload.setTargetName(task.getTargetName());
        payload.setPriority(task.getPriority());
        payload.setSourceTaskId(task.getSourceTaskId());
        payload.setSourceReportId(task.getSourceReportId());
        payload.setSourceEventId(task.getSourceEventId());
        payload.setSourceDomain(task.getSourceDomain());
        payload.setSourceReviewStatus(task.getSourceReviewStatus());
        payload.setAnalysisScope(task.getAnalysisScope());
        payload.setActorProvenance(AiTaskActorProvenanceSupport.userInitiated(
                "research-task-service",
                dto == null ? null : dto.getOperatorId(),
                null
        ));

        AiTaskDispatchMessage message = new AiTaskDispatchMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTraceId(task.getTraceId());
        message.setTaskId(task.getTaskId());
        message.setEventId(task.getSourceEventId());
        message.setMessageType(MessageTypeConstants.AI_TASK_DISPATCH);
        message.setSourceService("research-task-service");
        message.setTargetService("python-ai-engine");
        message.setTenantId(task.getTenantId());
        message.setBizKey(task.getTargetType() + ":" + task.getTargetCode());
        message.setTimestamp(System.currentTimeMillis());
        message.setVersion("1.0");
        message.setRetryCount(task.getRetryCount() == null ? 0 : task.getRetryCount());
        message.setPayload(payload);
        return message;
    }
}
