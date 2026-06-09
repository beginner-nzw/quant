package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class MarketIntelligencePageVO {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<MarketIntelligenceListItemVO> records;
}