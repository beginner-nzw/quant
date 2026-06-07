package com.quant.aiorchestrator.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.entity.AuditRecordDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskRetryLogDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskStepDO;
import com.quant.aiorchestrator.mapper.AiAgentExecutionMapper;
import com.quant.aiorchestrator.mapper.AiWorkflowInstanceMapper;
import com.quant.aiorchestrator.mapper.AuditRecordMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskRetryLogMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskQueryReadManager {

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchTaskStepMapper researchTaskStepMapper;
    private final AiWorkflowInstanceMapper aiWorkflowInstanceMapper;
    private final AiAgentExecutionMapper aiAgentExecutionMapper;
    private final AuditRecordMapper auditRecordMapper;
    private final ResearchTaskRetryLogMapper researchTaskRetryLogMapper;
    private final ResearchReportMapper researchReportMapper;

    public ResearchTaskDO selectTaskById(String taskId) {
        return researchTaskMapper.selectOne(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getTaskId, taskId)
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .last("limit 1")
        );
    }

    public List<ResearchTaskStepDO> listTaskSteps(String taskId) {
        return researchTaskStepMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskStepDO>()
                        .eq(ResearchTaskStepDO::getTaskId, taskId)
                        .eq(ResearchTaskStepDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskStepDO::getExecutionOrder, ResearchTaskStepDO::getId)
        );
    }

    public AiWorkflowInstanceDO selectLatestWorkflowInstance(String taskId) {
        return aiWorkflowInstanceMapper.selectOne(
                new LambdaQueryWrapper<AiWorkflowInstanceDO>()
                        .eq(AiWorkflowInstanceDO::getTaskId, taskId)
                        .eq(AiWorkflowInstanceDO::getDeleted, 0)
                        .orderByDesc(AiWorkflowInstanceDO::getCreatedAt, AiWorkflowInstanceDO::getId)
                        .last("limit 1")
        );
    }

    public List<AiAgentExecutionDO> listAgentExecutions(String taskId) {
        return aiAgentExecutionMapper.selectList(
                new LambdaQueryWrapper<AiAgentExecutionDO>()
                        .eq(AiAgentExecutionDO::getTaskId, taskId)
                        .eq(AiAgentExecutionDO::getDeleted, 0)
                        .orderByAsc(AiAgentExecutionDO::getId)
        );
    }

    public List<AuditRecordDO> listAuditRecords(String taskId) {
        return auditRecordMapper.selectList(
                new LambdaQueryWrapper<AuditRecordDO>()
                        .eq(AuditRecordDO::getTaskId, taskId)
                        .eq(AuditRecordDO::getDeleted, 0)
                        .orderByDesc(AuditRecordDO::getId)
        );
    }

    public List<ResearchReportDO> listReports(LambdaQueryWrapper<ResearchReportDO> wrapper) {
        return researchReportMapper.selectList(wrapper);
    }

    public Page<ResearchTaskDO> pageTasks(Page<ResearchTaskDO> page, LambdaQueryWrapper<ResearchTaskDO> wrapper) {
        return researchTaskMapper.selectPage(page, wrapper);
    }

    public List<ResearchTaskRetryLogDO> listRetryLogs(String taskId) {
        return researchTaskRetryLogMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskRetryLogDO>()
                        .eq(ResearchTaskRetryLogDO::getTaskId, taskId)
                        .eq(ResearchTaskRetryLogDO::getDeleted, 0)
                        .orderByAsc(ResearchTaskRetryLogDO::getRetryNo)
        );
    }

    public Long countTasks(LambdaQueryWrapper<ResearchTaskDO> wrapper) {
        return researchTaskMapper.selectCount(wrapper);
    }
}
