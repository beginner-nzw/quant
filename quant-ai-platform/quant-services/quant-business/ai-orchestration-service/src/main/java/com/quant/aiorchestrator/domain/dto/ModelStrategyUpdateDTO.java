package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModelStrategyUpdateDTO {

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
