package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventSourcePreviewResultVO {

    private String sourceCode;
    private String sourceName;
    private String sourceCategory;
    private String ingestMode;
    private String endpointUrl;
    private String upstreamUrl;
    private Integer itemCount;
    private String previewedAt;
    private List<EventSourcePreviewItemVO> items;
}
