package com.quant.aiorchestrator.domain.vo;

import lombok.Data;

@Data
public class EventSourceConfigItemVO {

    private String sourceCode;
    private String sourceName;
    private String sourceCategory;
    private String sourceChannel;
    private String ingestMode;
    private Boolean enabled;
    private Boolean supportsMockIngest;
    private Boolean sslVerify;
    private String endpointUrl;
    private String requestMethod;
    private Integer requestTimeoutSeconds;
    private String requestHeadersJson;
    private String requestQueryJson;
    private String requestBodyJson;
    private String responseItemsField;
    private String fieldMappingJson;
    private String upstreamUrl;
    private String upstreamMethod;
    private String upstreamHeadersJson;
    private String upstreamQueryJson;
    private String upstreamBodyJson;
    private String upstreamItemsField;
    private String upstreamFieldMappingJson;
    private String defaultEventType;
    private String defaultImpactLevel;
    private Integer ingestRecordCount;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private Integer autoTriggeredCount;
    private String lastIngestAt;
    private String lastResultStatus;
    private String lastErrorMessage;
    private String remark;
}
