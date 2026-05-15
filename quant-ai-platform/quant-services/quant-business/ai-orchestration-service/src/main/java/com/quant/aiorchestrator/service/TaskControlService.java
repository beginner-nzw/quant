package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;

public interface TaskControlService {
    String cancelTask(String taskId, TaskCancelDTO dto);
}