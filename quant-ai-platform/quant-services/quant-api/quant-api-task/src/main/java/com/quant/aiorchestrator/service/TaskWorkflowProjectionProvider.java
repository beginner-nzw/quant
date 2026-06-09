package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.AgentExecutionVO;
import com.quant.aiorchestrator.domain.vo.WorkflowInstanceVO;

import java.util.List;

public interface TaskWorkflowProjectionProvider {
    WorkflowInstanceVO getWorkflowInstance(String taskId);

    List<AgentExecutionVO> listAgentExecutions(String taskId);
}
