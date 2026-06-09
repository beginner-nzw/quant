package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ModelAgentConfigStatsVO {

    private Integer workflowCount;
    private Integer activeAgentCount;
    private Integer modelStrategyCount;
    private Integer promptTemplateCount;
    private Integer toolWhitelistCount;
    private Integer placeholderStrategyCount;
    private Integer eventAutoTriggerRuleCount;
    private Integer eventSourceConfigCount;
    private Integer configAuditCount;
    private Integer roleAccessConfigCount;
}
