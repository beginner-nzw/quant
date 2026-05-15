package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ModelStrategyItemVO {

    private String strategyCode;
    private String scenarioCode;
    private String provider;
    private String modelName;
    private String baseUrl;
    private String accessMode;
    private Boolean enabled;
    private Boolean placeholder;
    private Boolean fallbackEnabled;
    private Integer requestTimeoutSeconds;
    private Double temperature;
    private Integer maxTokens;
    private String promptTemplateCode;
    private List<String> boundAgents;
    private String remark;
}
