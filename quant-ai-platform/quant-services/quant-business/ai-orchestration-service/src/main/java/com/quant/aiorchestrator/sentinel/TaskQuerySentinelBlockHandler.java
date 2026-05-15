package com.quant.aiorchestrator.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.common.core.model.Result;

public class TaskQuerySentinelBlockHandler {

    public static Result<TaskPageVO> handlePageTasksBlock(TaskPageQueryDTO queryDTO, BlockException ex) {
        return Result.fail("RATE_LIMITED", "任务列表查询过于频繁，请稍后再试");
    }

    public static Result<TaskFullDetailVO> handleTaskFullDetailBlock(String taskId, BlockException ex) {
        return Result.fail("RATE_LIMITED", "任务详情查询过于频繁，请稍后再试");
    }
}