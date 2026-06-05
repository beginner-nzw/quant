package com.quant.common.security;

public final class SecurityConstants {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_SERVICE_PRINCIPAL = "X-Service-Principal";
    public static final String HEADER_SERVICE_ACTOR_ID = "X-Service-Actor-Id";
    public static final String HEADER_SERVICE_ACTOR_ROLE = "X-Service-Actor-Role";
    public static final String HEADER_SERVICE_ORIGINAL_ACTOR_ID = "X-Service-Original-Actor-Id";
    public static final String HEADER_SERVICE_ORIGINAL_ACTOR_ROLE = "X-Service-Original-Actor-Role";
    public static final String HEADER_SERVICE_TIMESTAMP = "X-Service-Timestamp";
    public static final String HEADER_SERVICE_SIGNATURE = "X-Service-Signature";

    public static final String DEFAULT_USER_ID = "guest";
    public static final String DEFAULT_USER_ROLE = "USER";

    private SecurityConstants() {
    }
}
