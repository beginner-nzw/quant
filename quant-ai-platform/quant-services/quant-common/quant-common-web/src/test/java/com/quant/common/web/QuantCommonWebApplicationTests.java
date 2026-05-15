package com.quant.common.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantCommonWebApplicationTests {

    @Test
    void exposesTraceConstants() {
        assertEquals("traceId", TraceConstants.MDC_TRACE_ID);
    }

}
