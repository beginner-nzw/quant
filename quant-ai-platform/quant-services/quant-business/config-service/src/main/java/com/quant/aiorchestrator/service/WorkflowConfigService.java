package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.WorkflowConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.WorkflowConfigItemVO;
import com.quant.config.port.WorkflowConfigQueryPort;
import java.util.List;

public interface WorkflowConfigService extends WorkflowConfigQueryPort {
    List<WorkflowConfigItemVO> loadWorkflows();

    @Override
    WorkflowConfigItemVO resolveWorkflow(String taskType);

    void saveWorkflow(String workflowCode, WorkflowConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
