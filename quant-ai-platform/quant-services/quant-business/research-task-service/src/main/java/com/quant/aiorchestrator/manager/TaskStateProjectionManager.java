package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.vo.TaskStateVO;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.common.redis.RedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TaskStateProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskStateManager taskStateManager;
    private final ResearchTaskMapper researchTaskMapper;

    public TaskStateProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskStateManager taskStateManager,
                                      ResearchTaskMapper researchTaskMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskStateManager = taskStateManager;
        this.researchTaskMapper = researchTaskMapper;
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
                    task = selectTaskById(taskId);
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
            task = selectTaskById(taskId);
        }
        if (task != null) {
            vo.setStatus(task.getStatus());
            vo.setCurrentStage(task.getCurrentStage());
            vo.setProgress(taskStateManager.isFinalState(task.getStatus()) ? 100 : null);
            vo.setSource("mysql");
        }
        return vo;
    }

    private ResearchTaskDO selectTaskById(String taskId) {
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
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
