package com.quant.task.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.quant.common.core.model.Result;
import com.quant.task.domain.dto.CreateResearchTaskDTO;

public class TaskSentinelBlockHandler {

    public static Result<String> handleCreateTaskBlock(CreateResearchTaskDTO dto, BlockException ex) {
        return Result.fail("RATE_LIMITED", "任务创建过于频繁，请稍后再试");
    }
}