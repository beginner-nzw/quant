package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.TaskCancelDTO;
import com.quant.aiorchestrator.domain.dto.TaskWorkflowControlDTO;
import com.quant.aiorchestrator.manager.TaskControlCommandManager;
import com.quant.aiorchestrator.manager.TaskControlTaskLoaderManager;
import com.quant.aiorchestrator.service.TaskControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskControlServiceImpl implements TaskControlService {

    private final TaskControlTaskLoaderManager taskControlTaskLoaderManager;
    private final TaskControlCommandManager taskControlCommandManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cancelTask(String taskId, TaskCancelDTO dto) {
        return taskControlCommandManager.cancelTask(taskControlTaskLoaderManager.selectRequiredTask(taskId), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resumeTask(String taskId, TaskWorkflowControlDTO dto) {
        return taskControlCommandManager.resumeTask(taskControlTaskLoaderManager.selectRequiredTask(taskId), dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String rerunNode(String taskId, TaskWorkflowControlDTO dto) {
        return taskControlCommandManager.rerunNode(taskControlTaskLoaderManager.selectRequiredTask(taskId), dto);
    }

}
