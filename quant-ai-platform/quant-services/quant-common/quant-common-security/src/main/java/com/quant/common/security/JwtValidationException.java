package com.quant.common.security;

public class JwtValidationException extends RuntimeException {

    private final String errorCode;

    public JwtValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
