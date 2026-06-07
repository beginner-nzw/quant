package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.common.redis.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TaskStateProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskStateManager taskStateManager;
    private final TaskQueryReadManager taskQueryReadManager;

    public TaskStateProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskStateManager taskStateManager,
                                      TaskQueryReadManager taskQueryReadManager) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskStateManager = taskStateManager;
        this.taskQueryReadManager = taskQueryReadManager;
    }

    public TaskStateVO getTaskState(String taskId) {
        TaskStateVO vo = new TaskStateVO();
        vo.setTaskId(taskId);

        String cache = stringRedisTemplate.opsForValue().get(RedisKeyBuilder.taskState(taskId));
        ResearchTaskDO task = null;
        if (cache != null && !cache.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(cache);
                String cachedStatus = json.path("status").asText();
                if (!taskStateManager.isFinalState(cachedStatus)) {
                    task = taskQueryReadManager.selectTaskById(taskId);
                    if (task != null && taskStateManager.isFinalState(task.getStatus())) {
                        vo.setStatus(task.getStatus());
                        vo.setCurrentStage(task.getCurrentStage());
                        vo.setProgress(100);
                        vo.setSource("mysql");
                        refreshTaskStateCache(taskId, task.getStatus(), task.getCurrentStage(), 100);
                        return vo;
                    }
                }

                vo.setStatus(cachedStatus);
                vo.setCurrentStage(json.path("currentStage").asText());
                vo.setProgress(json.path("progress").asInt());
                vo.setSource("redis");
                return vo;
            } catch (Exception ignored) {
            }
        }

        if (task == null) {
            task = taskQueryReadManager.selectTaskById(taskId);
        }
        if (task != null) {
            vo.setStatus(task.getStatus());
            vo.setCurrentStage(task.getCurrentStage());
            vo.setProgress(taskStateManager.isFinalState(task.getStatus()) ? 100 : null);
            vo.setSource("mysql");
        }
        return vo;
    }

    private void refreshTaskStateCache(String taskId, String status, String currentStage, int progress) {
        String stateJson = """
                {"status":"%s","currentStage":"%s","progress":%d}
                """.formatted(status, currentStage, progress);
        stringRedisTemplate.opsForValue().set(
                RedisKeyBuilder.taskState(taskId),
                stateJson,
                Duration.ofHours(24)
        );
    }
}
