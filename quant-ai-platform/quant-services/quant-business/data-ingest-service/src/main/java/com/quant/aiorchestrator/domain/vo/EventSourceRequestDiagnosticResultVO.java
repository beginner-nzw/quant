package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class EventSourceRequestDiagnosticResultVO {

    private String sourceCode;
    private String sourceName;
    private String ingestMode;
    private String diagnosedAt;
    private List<EventSourceRequestDiagnosticItemVO> items;
}
