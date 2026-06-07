package com.quant.aiorchestrator.manager;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CsrcRiskExtractionManager {

    private static final Pattern DECISION_NO_PATTERN = Pattern.compile("([〔（(]?20\\d{2}[〕）)]?\\s*\\d+\\s*号)");
    private static final Pattern AGENCY_PATTERN = Pattern.compile("([^，。；;\\s]{2,40}(?:证监局|证券监督管理委员会|证券监督管理局|证券监管局|证券交易所|期货交易所|交易所|证监会))");
    private static final List<Pattern> MONEY_AMOUNT_PATTERNS = List.of(
            Pattern.compile("((?:罚款|没收违法所得|罚没|罚没款)(?:人民币)?[0-9０-９一二三四五六七八九十百千万亿点\\.,，、]+元)"),
            Pattern.compile("(处以(?:人民币)?[0-9０-９一二三四五六七八九十百千万亿点\\.,，、]+元罚款)")
    );
    private static final List<Pattern> SUBJECT_PATTERNS = List.of(
            Pattern.compile("(?:当事人|处罚对象|监管对象|被处罚人|被监管对象)[:：]?\\s*([^。；;\\n]{2,120})"),
            Pattern.compile("对([^，。；;\\n]{2,80})(?:采取|出具|给予|作出|责令|予以|实施)")
    );
    private static final List<String> VIOLATION_KEYWORDS = List.of(
            "违法",
            "违规",
            "信息披露",
            "内幕交易",
            "操纵",
            "未按规定",
            "欺诈发行",
            "虚假记载",
            "误导性陈述",
            "重大遗漏"
    );
    private static final List<RiskTypeRule> RISK_TYPE_RULES = List.of(
            new RiskTypeRule("市场禁入", "市场禁入"),
            new RiskTypeRule("行政处罚", "行政处罚"),
            new RiskTypeRule("处罚决定", "行政处罚"),
            new RiskTypeRule("立案调查", "立案调查"),
            new RiskTypeRule("责令改正", "责令改正"),
            new RiskTypeRule("警示函", "出具警示函"),
            new RiskTypeRule("监管措施", "监管措施"),
            new RiskTypeRule("监管谈话", "监管谈话"),
            new RiskTypeRule("纪律处分", "纪律处分")
    );

    public RiskExtraction extractRiskDetail(String title, String content) {
        String combinedText = normalizeWhitespace(combineText(title, content));
        String regulatoryType = extractRegulatoryType(combinedText);
        String agency = extractAgency(combinedText);
        String subject = extractSubject(combinedText);
        String penaltyAmount = extractPenaltyAmount(combinedText);
        String decisionNo = extractFirstMatch(DECISION_NO_PATTERN, combinedText, 1);
        String violationSummary = extractViolationSummary(combinedText);
        return new RiskExtraction(
                regulatoryType,
                agency,
                subject,
                penaltyAmount,
                decisionNo,
                violationSummary
        );
    }

    public String buildStructuredRiskDetail(RiskExtraction riskExtraction, boolean targetMatched) {
        List<String> parts = new ArrayList<>();
        if (riskExtraction != null) {
            appendStructuredPart(parts, "监管类型", riskExtraction.regulatoryType());
            appendStructuredPart(parts, "监管机构", riskExtraction.agency());
            appendStructuredPart(parts, "处罚/监管对象", riskExtraction.subject());
            appendStructuredPart(parts, "罚没金额", riskExtraction.penaltyAmount());
            appendStructuredPart(parts, "文号", riskExtraction.decisionNo());
            appendStructuredPart(parts, "违规事项", riskExtraction.violationSummary());
        }
        parts.add("命中标的=" + (targetMatched ? "是" : "否"));
        return String.join("；", parts);
    }

    public String buildRiskContentExcerpt(String content, RiskExtraction riskExtraction) {
        if (riskExtraction != null && StringUtils.hasText(riskExtraction.violationSummary())) {
            return abbreviate(riskExtraction.violationSummary(), 260);
        }
        return abbreviate(content, 360);
    }

    public String combineText(String... values) {
        List<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    parts.add(value.trim());
                }
            }
        }
        return String.join(" ", parts);
    }

    public boolean containsAnyKeyword(String text, List<String> keywords) {
        if (!StringUtils.hasText(text) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        return keywords.stream()
                .filter(StringUtils::hasText)
                .anyMatch(text::contains);
    }

    private void appendStructuredPart(List<String> parts, String label, String value) {
        String normalized = abbreviate(value, 120);
        if (StringUtils.hasText(normalized)) {
            parts.add(label + "=" + normalized);
        }
    }

    private String extractRegulatoryType(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (RiskTypeRule rule : RISK_TYPE_RULES) {
            if (text.contains(rule.keyword())) {
                return rule.label();
            }
        }
        return null;
    }

    private String extractAgency(String text) {
        String agency = extractFirstMatch(AGENCY_PATTERN, text, 1);
        if (StringUtils.hasText(agency)) {
            return agency;
        }
        if (StringUtils.hasText(text) && text.contains("证监会")) {
            return "中国证监会";
        }
        return null;
    }

    private String extractPenaltyAmount(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : MONEY_AMOUNT_PATTERNS) {
            String amount = cleanupExtractedPhrase(extractFirstMatch(pattern, text, 1));
            if (StringUtils.hasText(amount)) {
                return amount;
            }
        }
        return null;
    }

    private String extractSubject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (Pattern pattern : SUBJECT_PATTERNS) {
            String subject = cleanupExtractedPhrase(extractFirstMatch(pattern, text, 1));
            if (StringUtils.hasText(subject)) {
                return subject;
            }
        }
        return null;
    }

    private String extractViolationSummary(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] sentences = text.split("[。！？!?]");
        for (String sentence : sentences) {
            String normalized = cleanupExtractedPhrase(sentence);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (containsAnyKeyword(normalized, VIOLATION_KEYWORDS)) {
                return normalized;
            }
        }
        return null;
    }

    private String extractFirstMatch(Pattern pattern, String text, int groupIndex) {
        if (pattern == null || !StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return trimToNull(matcher.group(groupIndex));
    }

    private String normalizeWhitespace(String value) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized.replaceAll("\\s+", " ").trim() : null;
    }

    private String cleanupExtractedPhrase(String value) {
        String normalized = normalizeWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        normalized = normalized
                .replaceAll("^[：:，,；;。\\s]+", "")
                .replaceAll("[：:，,；;。\\s]+$", "")
                .replaceAll("^关于对?", "")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String abbreviate(String value, int maxLength) {
        String normalized = normalizeWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record RiskExtraction(String regulatoryType,
                                 String agency,
                                 String subject,
                                 String penaltyAmount,
                                 String decisionNo,
                                 String violationSummary) {
    }

    private record RiskTypeRule(String keyword, String label) {
    }
}
