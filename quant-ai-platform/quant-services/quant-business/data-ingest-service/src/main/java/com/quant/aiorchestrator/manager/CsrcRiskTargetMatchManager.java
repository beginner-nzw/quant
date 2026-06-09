package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CsrcRiskTargetMatchManager {

    private static final Pattern TRAILING_EXCHANGE_PATTERN = Pattern.compile("(?i)(?:\\.|_)?(SH|SZ|BJ|HK)$");
    private static final Pattern LEADING_EXCHANGE_PATTERN = Pattern.compile("(?i)^(SH|SZ|BJ|HK)");

    public List<String> buildTargetTokens(MarketEventSourceSyncDTO request) {
        List<String> tokens = new ArrayList<>();
        if (request == null) {
            return tokens;
        }
        addToken(tokens, request.getTargetName());
        addToken(tokens, request.getTargetCode());
        addToken(tokens, normalizeTargetCode(request.getTargetCode()));
        return tokens.stream().distinct().toList();
    }

    public boolean containsAnyToken(String text, List<String> tokens) {
        if (!StringUtils.hasText(text) || tokens == null || tokens.isEmpty()) {
            return false;
        }
        String normalizedText = text.toUpperCase(Locale.ROOT);
        return tokens.stream()
                .filter(StringUtils::hasText)
                .map(item -> item.toUpperCase(Locale.ROOT))
                .anyMatch(normalizedText::contains);
    }

    private void addToken(List<String> tokens, String value) {
        String normalized = trimToNull(value);
        if (StringUtils.hasText(normalized) && normalized.length() >= 2) {
            tokens.add(normalized);
        }
    }

    private String normalizeTargetCode(String rawTargetCode) {
        String value = trimToNull(rawTargetCode);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = TRAILING_EXCHANGE_PATTERN.matcher(value).replaceFirst("");
        normalized = LEADING_EXCHANGE_PATTERN.matcher(normalized).replaceFirst("");
        normalized = normalized.replaceAll("[^0-9A-Za-z]", "");
        return StringUtils.hasText(normalized) ? normalized : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
