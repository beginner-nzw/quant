package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class StrategySignalPageVO {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<StrategySignalListItemVO> records;
}
