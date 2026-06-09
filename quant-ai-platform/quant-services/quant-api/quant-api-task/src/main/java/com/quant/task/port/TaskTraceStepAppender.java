package com.quant.task.port;

public interface TaskTraceStepAppender {

    void appendSucceededStep(String taskId, String stage, String node, int executionOrder);
}
