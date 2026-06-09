package com.quant.aiorchestrator.risk;

import com.quant.common.model.message.AiTaskResultMessage;

public interface RiskWarningProjectionPort {

    void project(AiTaskResultMessage message);
}
