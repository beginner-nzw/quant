package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.TaskRetryDTO;
import com.quant.aiorchestrator.manager.TaskRetryCommandManager;
import com.quant.aiorchestrator.service.TaskRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskRetryServiceImpl implements TaskRetryService {

    private final TaskRetryCommandManager taskRetryCommandManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryTask(String taskId, TaskRetryDTO dto) {
        return taskRetryCommandManager.retryTask(taskId, dto);
    }
}
