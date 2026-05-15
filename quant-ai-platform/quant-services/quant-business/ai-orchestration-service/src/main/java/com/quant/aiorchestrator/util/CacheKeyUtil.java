package com.quant.aiorchestrator.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CacheKeyUtil {

    private CacheKeyUtil() {
    }

    public static String md5(String source) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("md5 generate failed", e);
        }
    }
}