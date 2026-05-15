package com.quant.common.web;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {

    private TraceContext() {
    }

    public static String currentTraceId() {
        return MDC.get(TraceConstants.MDC_TRACE_ID);
    }

    public static String createTraceId() {
        return UUID.randomUUID().toString();
    }

    public static String resolveTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return createTraceId();
        }
        return traceId;
    }

    public static String bind(String traceId) {
        String resolvedTraceId = resolveTraceId(traceId);
        MDC.put(TraceConstants.MDC_TRACE_ID, resolvedTraceId);
        return resolvedTraceId;
    }

    public static void clear() {
        MDC.remove(TraceConstants.MDC_TRACE_ID);
    }
}
