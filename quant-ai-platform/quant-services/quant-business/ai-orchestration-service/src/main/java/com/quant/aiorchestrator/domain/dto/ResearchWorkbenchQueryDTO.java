package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

@Data
public class ResearchWorkbenchQueryDTO {
    private String targetCode;
    private String targetName;
    private Integer recentTaskLimit = 6;
}