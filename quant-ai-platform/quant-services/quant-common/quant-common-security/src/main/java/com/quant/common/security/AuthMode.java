package com.quant.common.security;

import java.util.Locale;

public enum AuthMode {
    DEMO,
    JWT,
    JWT_WITH_DEMO_FALLBACK;

    public static AuthMode from(String value) {
        if (value == null || value.isBlank()) {
            return JWT;
        }
        try {
            return AuthMode.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            return JWT;
        }
    }
}
