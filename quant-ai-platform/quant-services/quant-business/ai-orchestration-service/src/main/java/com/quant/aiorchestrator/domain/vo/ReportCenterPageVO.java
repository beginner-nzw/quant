package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReportCenterPageVO {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<ReportCenterListItemVO> records;
}
