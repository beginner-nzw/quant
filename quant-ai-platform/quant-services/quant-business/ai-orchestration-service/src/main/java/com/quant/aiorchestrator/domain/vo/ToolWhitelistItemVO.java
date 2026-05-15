package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ToolWhitelistItemVO {

    private String toolCode;
    private String toolName;
    private String toolType;
    private Boolean enabled;
    private String scope;
    private String remark;
}
