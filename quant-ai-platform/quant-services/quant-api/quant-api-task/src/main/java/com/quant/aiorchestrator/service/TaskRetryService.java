package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;

public interface TaskRetryService {
    String retryTask(String taskId, TaskRetryDTO dto);
}