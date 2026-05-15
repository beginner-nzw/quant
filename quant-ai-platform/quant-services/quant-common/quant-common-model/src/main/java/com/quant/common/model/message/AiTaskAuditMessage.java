package com.quant.common.model.message;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiTaskAuditMessage extends MessageEnvelope {

    private AuditPayload payload;

    @Data
    public static class AuditPayload {
        private String workflowInstanceId;
        private List<AgentAuditItem> agents;
        private String reviewSuggestion;
        private List<String> evidenceRefs;
    }

    @Data
    public static class AgentAuditItem {
        private String executionId;
        private String agentCode;
        private String agentName;
        private String nodeCode;
        private String status;
        private Double confidenceScore;
        private Boolean needHumanReview;
        private Long startTimestamp;
        private Long finishTimestamp;
        private Long durationMs;
    }
}
