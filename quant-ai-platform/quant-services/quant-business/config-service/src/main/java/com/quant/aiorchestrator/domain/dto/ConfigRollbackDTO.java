package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class ConfigRollbackDTO {
    private String versionId;
    private String reason;
}
