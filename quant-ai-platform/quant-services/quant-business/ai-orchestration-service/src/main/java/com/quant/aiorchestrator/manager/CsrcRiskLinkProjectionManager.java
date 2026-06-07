package com.quant.aiorchestrator.manager;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CsrcRiskLinkProjectionManager {

    private static final Pattern FULL_DATE_PATTERN = Pattern.compile("(20\\d{2})[-年./](\\d{1,2})[-月./](\\d{1,2})");
    private static final Pattern SHORT_DATE_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[-/.](\\d{1,2})(?!\\d)");
    private static final List<String> RISK_KEYWORDS = List.of(
            "行政处罚",
            "处罚决定",
            "市场禁入",
            "监管措施",
            "责令改正",
            "警示函",
            "立案调查",
            "纪律处分",
            "监管谈话"
    );
    private static final Set<String> SKIP_TITLES = Set.of(
            "更多",
            "首页",
            "返回顶部",
            "下一页",
            "上一页",
            "政务信息"
    );

    public List<RiskLink> resolveRiskLinks(org.jsoup.nodes.Document document) {
        Elements anchors = document == null ? new Elements() : document.select("a[href]");
        List<RiskLink> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (Element anchor : anchors) {
            String href = trimToNull(anchor.absUrl("href"));
            String title = trimToNull(anchor.text());
            if (!isRiskLink(href, title)) {
                continue;
            }
            String occurredDate = resolveOccurredDate(anchor);
            String fingerprint = href + "|" + title;
            if (!dedupe.add(fingerprint)) {
                continue;
            }
            result.add(new RiskLink(href, title, occurredDate));
        }
        return result;
    }

    private boolean isRiskLink(String href, String title) {
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title)) {
            return false;
        }
        if (SKIP_TITLES.contains(title.trim())) {
            return false;
        }
        String normalizedHref = href.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHref.contains("csrc.gov.cn")) {
            return false;
        }
        return RISK_KEYWORDS.stream().anyMatch(title::contains);
    }

    private String resolveOccurredDate(Element anchor) {
        Element current = anchor;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            String matchedDate = matchDate(current.text());
            if (StringUtils.hasText(matchedDate)) {
                return matchedDate;
            }
            current = current.parent();
        }
        return null;
    }

    private String matchDate(String text) {
        String value = trimToNull(text);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher fullMatcher = FULL_DATE_PATTERN.matcher(value);
        if (fullMatcher.find()) {
            int year = Integer.parseInt(fullMatcher.group(1));
            int month = Integer.parseInt(fullMatcher.group(2));
            int day = Integer.parseInt(fullMatcher.group(3));
            return formatDate(year, month, day);
        }
        Matcher shortMatcher = SHORT_DATE_PATTERN.matcher(value);
        if (shortMatcher.find()) {
            int month = Integer.parseInt(shortMatcher.group(1));
            int day = Integer.parseInt(shortMatcher.group(2));
            int year = Year.now().getValue();
            try {
                LocalDate candidate = LocalDate.of(year, month, day);
                if (candidate.isAfter(LocalDate.now().plusDays(7))) {
                    candidate = candidate.minusYears(1);
                }
                return candidate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String formatDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record RiskLink(String href, String title, String occurredDate) {
    }
}
