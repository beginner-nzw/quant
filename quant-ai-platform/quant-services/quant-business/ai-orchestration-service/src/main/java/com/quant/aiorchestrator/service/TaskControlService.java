package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;

public interface TaskControlService {
    String cancelTask(String taskId, TaskCancelDTO dto);
    String resumeTask(String taskId, TaskWorkflowControlDTO dto);
    String rerunNode(String taskId, TaskWorkflowControlDTO dto);
}
