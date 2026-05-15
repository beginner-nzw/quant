package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class CninfoProxyAnnouncementResponseVO {

    private String sourceCode;
    private String sourceName;
    private String targetCode;
    private String targetName;
    private Integer itemCount;
    private List<CninfoProxyAnnouncementItemVO> items;
}
