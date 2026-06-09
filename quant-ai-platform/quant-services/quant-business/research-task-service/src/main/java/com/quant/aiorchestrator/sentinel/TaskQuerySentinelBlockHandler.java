package com.quant.aiorchestrator.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.quant.aiorchestrator.domain.dto.TaskPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.TaskFullDetailVO;
import com.quant.aiorchestrator.domain.vo.TaskPageVO;
import com.quant.common.core.model.Result;

public class TaskQuerySentinelBlockHandler {

    public static Result<TaskPageVO> handlePageTasksBlock(TaskPageQueryDTO queryDTO, BlockException ex) {
        return Result.fail("RATE_LIMITED", "浠诲姟鍒楄〃鏌ヨ杩囦簬棰戠箒锛岃绋嶅悗鍐嶈瘯");
    }

    public static Result<TaskFullDetailVO> handleTaskFullDetailBlock(String taskId, BlockException ex) {
        return Result.fail("RATE_LIMITED", "浠诲姟璇︽儏鏌ヨ杩囦簬棰戠箒锛岃绋嶅悗鍐嶈瘯");
    }
}
