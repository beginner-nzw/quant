package com.quant.common.model.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiTaskResultMessage extends MessageEnvelope {

    private ResultPayload payload;

    @Data
    public static class ResultPayload {
        private String workflowInstanceId;
        private String taskType;
        private String taskTitle;
        private String analysisScope;
        private String targetType;
        private String targetCode;
        private String targetName;
        private String priority;
        private String sourceTaskId;
        private String sourceReportId;
        private String sourceEventId;
        private String sourceDomain;
        private String sourceReviewStatus;
        private String finalStatus;
        private String finalStage;
        private String summary;
        private Double confidenceScore;
        private Boolean needHumanReview;
        private List<String> riskWarnings;
        private Map<String, Object> reportMeta;
        private String resultRef;
    }
}
