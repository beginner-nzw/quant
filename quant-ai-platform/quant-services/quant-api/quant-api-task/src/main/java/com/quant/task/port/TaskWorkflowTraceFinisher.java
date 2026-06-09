package com.quant.task.port;

public interface TaskWorkflowTraceFinisher {

    void finishWorkflow(String workflowInstanceId, String finalNode, String finalStatus);
}
