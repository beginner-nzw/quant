package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class ConfigRollbackResultVO {
    private String storeCode;
    private String versionId;
    private Integer version;
    private String configPath;
}
