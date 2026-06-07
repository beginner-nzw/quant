package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.entity.*;
import com.quant.aiorchestrator.domain.vo.*;
import com.quant.aiorchestrator.mapper.*;
import com.quant.aiorchestrator.service.ReportQueryService;
import com.quant.common.model.enums.TaskStatusEnum;
import com.quant.common.redis.RedisKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskQueryProjectionManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final TaskCacheVersionManager taskCacheVersionManager;
    private final TaskStateManager taskStateManager;
    private final TaskQueryReadManager taskQueryReadManager;
    private final TaskFullDetailProjectionManager taskFullDetailProjectionManager;
    private final TaskQueryItemAssembler itemAssembler;
    private final TaskStateProjectionManager taskStateProjectionManager;
    private final TaskPageProjectionManager taskPageProjectionManager;

    @Autowired
    public TaskQueryProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskCacheVersionManager taskCacheVersionManager,
                                      TaskStateManager taskStateManager,
                                      ReportQueryService reportQueryService,
                                      TaskQueryReadManager taskQueryReadManager,
                                      TaskFullDetailProjectionManager taskFullDetailProjectionManager,
                                      TaskStateProjectionManager taskStateProjectionManager,
                                      TaskPageProjectionManager taskPageProjectionManager) {
        this(
                stringRedisTemplate,
                objectMapper,
                taskCacheVersionManager,
                taskStateManager,
                reportQueryService,
                taskQueryReadManager,
                taskFullDetailProjectionManager,
                new TaskQueryItemAssembler(objectMapper),
                taskStateProjectionManager,
                taskPageProjectionManager
        );
    }

    public TaskQueryProjectionManager(StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      TaskCacheVersionManager taskCacheVersionManager,
                                      TaskStateManager taskStateManager,
                                      ReportQueryService reportQueryService,
                                      TaskQueryReadManager taskQueryReadManager,
                                      TaskFullDetailProjectionManager taskFullDetailProjectionManager,
                                      TaskQueryItemAssembler itemAssembler,
                                      TaskStateProjectionManager taskStateProjectionManager,
                                      TaskPageProjectionManager taskPageProjectionManager) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskCacheVersionManager = taskCacheVersionManager;
        this.taskStateManager = taskStateManager;
        this.taskQueryReadManager = taskQueryReadManager;
        this.taskFullDetailProjectionManager = taskFullDetailProjectionManager;
        this.itemAssembler = itemAssembler;
        this.taskStateProjectionManager = taskStateProjectionManager;
        this.taskPageProjectionManager = taskPageProjectionManager;
    }

    public TaskQueryProjectionManager(ResearchTaskMapper researchTaskMapper,
                                      ResearchTaskStepMapper researchTaskStepMapper,
                                      AiWorkflowInstanceMapper aiWorkflowInstanceMapper,
                                      AiAgentExecutionMapper aiAgentExecutionMapper,
                                      AuditRecordMapper auditRecordMapper,
                                      StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper,
                                      ResearchTaskRetryLogMapper researchTaskRetryLogMapper,
                                      TaskCacheVersionManager taskCacheVersionManager,
                                      ResearchReportMapper researchReportMapper,
                                      TaskStateManager taskStateManager,
                                      ReportQueryService reportQueryService) {
        TaskQueryReadManager readManager = new TaskQueryReadManager(
                researchTaskMapper,
                researchTaskStepMapper,
                aiWorkflowInstanceMapper,
                aiAgentExecutionMapper,
                auditRecordMapper,
                researchTaskRetryLogMapper,
                researchReportMapper
        );
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.taskCacheVersionManager = taskCacheVersionManager;
        this.taskStateManager = taskStateManager;
        this.taskQueryReadManager = readManager;
        this.taskFullDetailProjectionManager = new TaskFullDetailProjectionManager(
                stringRedisTemplate,
                objectMapper,
                reportQueryService
        );
        this.itemAssembler = new TaskQueryItemAssembler(objectMapper);
        this.taskStateProjectionManager = new TaskStateProjectionManager(stringRedisTemplate, objectMapper, taskStateManager, readManager);
        this.taskPageProjectionManager = new TaskPageProjectionManager(
                stringRedisTemplate,
                objectMapper,
                taskCacheVersionManager,
                readManager,
                itemAssembler
        );
    }

    public TaskDetailVO getTaskDetail(String taskId) {
        ResearchTaskDO task = selectTaskById(taskId);
        return itemAssembler.toTaskDetailVO(task);
    }
    public TaskStateVO getTaskState(String taskId) {
        return taskStateProjectionManager.getTaskState(taskId);
    }
    public List<TaskStepVO> listTaskSteps(String taskId) {
        return taskQueryReadManager.listTaskSteps(taskId).stream().map(itemAssembler::toTaskStepVO).toList();
    }
    public WorkflowInstanceVO getWorkflowInstance(String taskId) {
        AiWorkflowInstanceDO workflow = taskQueryReadManager.selectLatestWorkflowInstance(taskId);
        if (workflow == null) {
            return null;
        }
        WorkflowInstanceVO vo = itemAssembler.toWorkflowInstanceVO(workflow);
        ResearchTaskDO task = selectTaskById(taskId);
        if (task != null
                && taskStateManager.isFinalState(task.getStatus())
                && !taskStateManager.isFinalState(workflow.getStatus())) {
            vo.setStatus(task.getStatus());
            vo.setCurrentNode(task.getCurrentStage());
            vo.setFinishTime(task.getFinishTime());
        }
        return vo;
    }

    private ResearchTaskDO selectTaskById(String taskId) {
        return taskQueryReadManager.selectTaskById(taskId);
    }

    public List<AgentExecutionVO> listAgentExecutions(String taskId) {
        return taskQueryReadManager.listAgentExecutions(taskId).stream().map(itemAssembler::toAgentExecutionVO).toList();
    }
    public List<AuditRecordVO> listAuditRecords(String taskId) {
        return taskQueryReadManager.listAuditRecords(taskId).stream().map(itemAssembler::toAuditRecordVO).toList();
    }
    public TaskPageVO pageTasks(TaskPageQueryDTO queryDTO) {
        return taskPageProjectionManager.pageTasks(queryDTO);
    }
    public List<TaskRetryLogVO> listRetryLogs(String taskId) {
        return taskQueryReadManager.listRetryLogs(taskId).stream().map(itemAssembler::toRetryLogVO).toList();
    }
    public TaskFullDetailVO getTaskFullDetail(String taskId) {
        return taskFullDetailProjectionManager.getTaskFullDetail(taskId, this);
    }
    public TaskStatsVO getTaskStats() {
        String cacheKey = RedisKeyConstants.TASK_STATS_GLOBAL;
        String cache = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cache != null && !cache.isBlank()) {
            try {
                return objectMapper.readValue(cache, TaskStatsVO.class);
            } catch (Exception ignored) {
            }
        }

        TaskStatsVO vo = new TaskStatsVO();

        vo.setTotalCount(taskQueryReadManager.countTasks(
                new LambdaQueryWrapper<ResearchTaskDO>().eq(ResearchTaskDO::getDeleted, 0)
        ));

        vo.setRunningCount(taskQueryReadManager.countTasks(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.RUNNING.name())
        ));

        vo.setSuccessCount(taskQueryReadManager.countTasks(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.SUCCESS.name())
        ));

        vo.setFailedCount(taskQueryReadManager.countTasks(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getStatus, TaskStatusEnum.FAILED.name())
        ));

        vo.setRetriedCount(taskQueryReadManager.countTasks(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .gt(ResearchTaskDO::getRetryCount, 0)
        ));

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(vo),
                    java.time.Duration.ofSeconds(15)
            );
        } catch (Exception ignored) {
        }

        return vo;
    }
}
