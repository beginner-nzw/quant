package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class EventSourceRequestDiagnosticItemVO {

    private String stageCode;
    private String stageName;
    private String requestMethod;
    private String requestUrl;
    private Integer requestTimeoutSeconds;
    private String requestHeadersJson;
    private String requestBodyJson;
}
