package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.entity.AiAgentExecutionDO;
import com.quant.aiorchestrator.domain.entity.AiWorkflowInstanceDO;
import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;
import com.quant.aiorchestrator.manager.TaskCrossDomainReadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiTaskWorkflowProjectionProvider implements TaskWorkflowProjectionProvider {

    private final TaskCrossDomainReadManager taskCrossDomainReadManager;

    public WorkflowInstanceVO getWorkflowInstance(String taskId) {
        AiWorkflowInstanceDO workflow = taskCrossDomainReadManager.selectLatestWorkflowInstance(taskId);
        if (workflow == null) {
            return null;
        }
        WorkflowInstanceVO vo = new WorkflowInstanceVO();
        BeanUtils.copyProperties(workflow, vo);
        return vo;
    }

    public List<AgentExecutionVO> listAgentExecutions(String taskId) {
        return taskCrossDomainReadManager.listAgentExecutions(taskId).stream()
                .map(this::toAgentExecutionVO)
                .toList();
    }

    private AgentExecutionVO toAgentExecutionVO(AiAgentExecutionDO entity) {
        AgentExecutionVO vo = new AgentExecutionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
