package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class PromptTemplateItemVO {

    private String templateCode;
    private String templateName;
    private String version;
    private String sourceType;
    private Boolean editable;
    private Boolean enabled;
    private String boundAgentCode;
    private List<String> variables;
    private String templatePath;
    private String templateContent;
    private String remark;
}
