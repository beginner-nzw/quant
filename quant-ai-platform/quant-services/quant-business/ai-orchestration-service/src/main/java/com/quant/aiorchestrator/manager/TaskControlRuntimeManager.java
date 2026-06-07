package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quant.common.model.enums.TaskStageEnum;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyBuilder;
import com.quant.common.redis.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TaskControlRuntimeManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;

    public String workflowCheckpoint(String taskId) {
        return stringRedisTemplate.opsForValue().get(RedisKeyBuilder.taskWorkflowCheckpoint(taskId));
    }

    public String workflowNodeState(String taskId, String nodeName) {
        return stringRedisTemplate.opsForValue().get(RedisKeyBuilder.taskWorkflowNode(taskId, nodeName));
    }

    public void writeCancelRuntime(String taskId, String cancelReason) {
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskControl(taskId),
                buildCancelRuntimeSignal(cancelReason),
                Duration.ofHours(24)
        );
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskState(taskId),
                buildCancelledTaskState(),
                Duration.ofHours(24)
        );
    }

    public void writeWorkflowControlSignal(String taskId, String action, String reason, String operatorId, String nodeName) {
        ObjectNode signal = objectMapper.createObjectNode();
        signal.put("action", action);
        signal.put("reason", reason);
        signal.put("operatorId", operatorId);
        signal.put("nodeName", nodeName);
        signal.put("requestedAt", System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskControl(taskId),
                signal.toString(),
                Duration.ofHours(24)
        );
    }

    public void evictTaskCaches(String taskId) {
        stringRedisTemplate.delete(RedisKeyBuilder.taskFull(taskId));
        stringRedisTemplate.delete(RedisKeyConstants.TASK_STATS_GLOBAL);
        taskCacheVersionManager.bumpVersion();
    }

    private String buildCancelRuntimeSignal(String cancelReason) {
        ObjectNode signal = objectMapper.createObjectNode();
        signal.put("cancelled", true);
        signal.put("reason", cancelReason);
        return signal.toString();
    }

    private String buildCancelledTaskState() {
        ObjectNode state = objectMapper.createObjectNode();
        state.put("status", TaskStatusEnum.CANCELLED.name());
        state.put("currentStage", TaskStageEnum.CANCELLED.name());
        state.put("progress", 100);
        return state.toString();
    }
}
