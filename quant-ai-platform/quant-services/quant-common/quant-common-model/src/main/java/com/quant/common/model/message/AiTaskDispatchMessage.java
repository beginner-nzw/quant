package com.quant.common.model.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiTaskDispatchMessage extends MessageEnvelope {

    private AiTaskDispatchPayload payload;

    @Data
    public static class AiTaskDispatchPayload {
        private String taskType;
        private String taskTitle;
        private String targetType;
        private String targetCode;
        private String targetName;
        private String priority;
        private String sourceTaskId;
        private String sourceReportId;
        private String sourceEventId;
        private String sourceDomain;
        private String sourceReviewStatus;
        private String analysisScope;
    }
}
