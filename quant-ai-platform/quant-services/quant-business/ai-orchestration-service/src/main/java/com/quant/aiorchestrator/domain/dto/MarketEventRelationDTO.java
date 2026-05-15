package com.quant.aiorchestrator.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketEventRelationDTO {
    private String relationType;
    private String relationCode;
    private String relationName;
    private BigDecimal relationWeight;
}
