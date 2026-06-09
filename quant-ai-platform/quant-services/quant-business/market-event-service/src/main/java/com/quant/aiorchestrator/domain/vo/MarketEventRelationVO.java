package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketEventRelationVO {
    private String relationType;
    private String relationCode;
    private String relationName;
    private BigDecimal relationWeight;
}
