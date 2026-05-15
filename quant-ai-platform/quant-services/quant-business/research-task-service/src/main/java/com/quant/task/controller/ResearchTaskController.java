package com.quant.task.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.quant.common.core.model.Result;
import com.quant.task.domain.dto.CreateResearchTaskDTO;
import com.quant.task.sentinel.TaskSentinelBlockHandler;
import com.quant.task.service.ResearchTaskService;
import com.quant.task.service.TaskRoleAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research/tasks")
@RequiredArgsConstructor
public class ResearchTaskController {

    private final ResearchTaskService researchTaskService;
    private final TaskRoleAccessService taskRoleAccessService;

    @PostMapping
    @SentinelResource(
            value = "createResearchTask",
            blockHandlerClass = TaskSentinelBlockHandler.class,
            blockHandler = "handleCreateTaskBlock"
    )
    public Result<String> createTask(@RequestBody @Valid CreateResearchTaskDTO dto) {
        taskRoleAccessService.requirePermission(TaskRoleAccessService.PERMISSION_TASK_CREATE);
        return Result.success(researchTaskService.createTask(dto));
    }
}
