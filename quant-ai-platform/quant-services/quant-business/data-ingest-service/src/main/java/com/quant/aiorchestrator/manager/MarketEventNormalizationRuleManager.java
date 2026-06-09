package com.quant.aiorchestrator.manager;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class MarketEventNormalizationRuleManager {

    private static final BigDecimal DEFAULT_CONFIDENCE_SCORE = new BigDecimal("0.7000");
    private static final Map<String, String> TARGET_TYPE_ALIASES = buildTargetTypeAliases();
    private static final Map<String, String> EVENT_TYPE_ALIASES = buildEventTypeAliases();
    private static final Map<String, String> IMPACT_LEVEL_ALIASES = buildImpactLevelAliases();
    private static final Map<String, String> EVENT_STATUS_ALIASES = buildEventStatusAliases();
    private static final Map<String, String> SOURCE_CHANNEL_ALIASES = buildSourceChannelAliases();

    public String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public String trimMessage(String message, int maxLength) {
        String normalized = trimToNull(message);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    public String normalizeTargetType(String value) {
        return normalizeByAliases(value, TARGET_TYPE_ALIASES, "STOCK");
    }

    public String normalizeRelationType(String value) {
        return normalizeTargetType(value);
    }

    public String normalizeTargetCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public String normalizeEventType(String value) {
        return normalizeByAliases(value, EVENT_TYPE_ALIASES, "OTHER");
    }

    public String normalizeImpactLevel(String value) {
        return normalizeByAliases(value, IMPACT_LEVEL_ALIASES, "MEDIUM");
    }

    public String normalizeEventStatus(String value) {
        return normalizeByAliases(value, EVENT_STATUS_ALIASES, "ACTIVE");
    }

    public String normalizeSourceChannel(String value, String eventType) {
        String normalized = normalizeByAliases(value, SOURCE_CHANNEL_ALIASES, null);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (!StringUtils.hasText(value)) {
            return "MANUAL_ENTRY";
        }
        String aliasKey = normalizeAliasKey(value);
        if (StringUtils.hasText(aliasKey) && aliasKey.chars().allMatch(ch -> ch == '_' || Character.isLetterOrDigit(ch))) {
            return aliasKey;
        }
        if ("ANNOUNCEMENT".equalsIgnoreCase(eventType)) {
            return "ANNOUNCEMENT_FEED";
        }
        if ("EARNINGS".equalsIgnoreCase(eventType)) {
            return "EARNINGS_FEED";
        }
        if ("POLICY".equalsIgnoreCase(eventType)) {
            return "POLICY_FEED";
        }
        if ("RISK_ALERT".equalsIgnoreCase(eventType)) {
            return "RISK_MONITOR";
        }
        return "NEWS_FEED";
    }

    public String normalizeProvenanceType(String value, String sourceChannel) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            return normalized.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        }
        String channel = trimToNull(sourceChannel);
        if (channel == null) {
            return "MANUAL";
        }
        if (channel.contains("MANUAL")) {
            return "MANUAL";
        }
        if (channel.contains("MOCK")) {
            return "MOCK_INGEST";
        }
        return "EXTERNAL_FEED";
    }

    public BigDecimal normalizeConfidenceScore(BigDecimal value) {
        if (value == null) {
            return DEFAULT_CONFIDENCE_SCORE;
        }
        BigDecimal normalized = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return normalized.setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal defaultConfidence(BigDecimal value) {
        return normalizeConfidenceScore(value);
    }

    public String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    public String sha256(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String normalizeByAliases(String value, Map<String, String> aliases, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String aliasKey = normalizeAliasKey(value);
        String normalized = aliasKey == null ? null : aliases.get(aliasKey);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String normalizeAliasKey(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim()
                .replace('\u3000', ' ')
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> buildTargetTypeAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "STOCK", "STOCK", "EQUITY", "A_SHARE", "SECURITY");
        return aliases;
    }

    private static Map<String, String> buildEventTypeAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "NEWS", "NEWS", "INFO", "NEWS_EVENT");
        registerAlias(aliases, "ANNOUNCEMENT", "ANNOUNCEMENT", "NOTICE", "DISCLOSURE");
        registerAlias(aliases, "EARNINGS", "EARNINGS", "EARNING", "FINANCIAL_REPORT", "ANNUAL_REPORT", "QUARTER_REPORT");
        registerAlias(aliases, "POLICY", "POLICY", "REGULATION");
        registerAlias(aliases, "RISK_ALERT", "RISK_ALERT", "RISK");
        registerAlias(aliases, "OTHER", "OTHER", "MISC");
        return aliases;
    }

    private static Map<String, String> buildImpactLevelAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "HIGH", "HIGH", "H", "P1", "1");
        registerAlias(aliases, "MEDIUM", "MEDIUM", "M", "P2", "2");
        registerAlias(aliases, "LOW", "LOW", "L", "P3", "3");
        return aliases;
    }

    private static Map<String, String> buildEventStatusAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "ACTIVE", "ACTIVE", "OPEN");
        registerAlias(aliases, "RESOLVED", "RESOLVED", "CLOSED", "DONE");
        registerAlias(aliases, "IGNORED", "IGNORED", "SKIPPED");
        return aliases;
    }

    private static Map<String, String> buildSourceChannelAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "MANUAL_ENTRY", "MANUAL_ENTRY", "MANUAL");
        registerAlias(aliases, "MANUAL_IMPORT", "MANUAL_IMPORT", "IMPORT");
        registerAlias(aliases, "NEWS_FEED", "NEWS_FEED", "NEWS");
        registerAlias(aliases, "ANNOUNCEMENT_FEED", "ANNOUNCEMENT_FEED", "ANNOUNCEMENT");
        registerAlias(aliases, "EARNINGS_FEED", "EARNINGS_FEED", "EARNINGS");
        registerAlias(aliases, "POLICY_FEED", "POLICY_FEED", "POLICY");
        registerAlias(aliases, "RISK_MONITOR", "RISK_MONITOR", "RISK_ALERT");
        registerAlias(aliases, "THIRD_PARTY_FEED", "THIRD_PARTY_FEED", "FEED", "THIRD_PARTY");
        return aliases;
    }

    private static void registerAlias(Map<String, String> aliases, String canonical, String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                aliases.put(value.trim().replace('\u3000', ' ').replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT), canonical);
            }
        }
    }
}
