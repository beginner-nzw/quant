package com.quant.common.model.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskStatusMessage extends MessageEnvelope {

    private StatusPayload payload;

    @Data
    public static class StatusPayload {
        private String workflowInstanceId;
        private String status;
        private String currentStage;
        private String currentNode;
        private Integer progress;
    }
}
