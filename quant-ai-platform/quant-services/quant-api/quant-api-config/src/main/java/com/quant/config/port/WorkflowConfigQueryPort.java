package com.quant.config.port;

import com.quant.config.api.WorkflowConfigItem;

public interface WorkflowConfigQueryPort {

    WorkflowConfigItem resolveWorkflow(String taskType);
}
