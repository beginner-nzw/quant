package com.quant.aiorchestrator.dataingest;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SourceRawPayload {
    private SourceProvenance provenance;
    private Integer httpStatus;
    private String requestMethod;
    private String requestUrl;
    private Map<String, String> responseHeaders;
    private String body;
}
