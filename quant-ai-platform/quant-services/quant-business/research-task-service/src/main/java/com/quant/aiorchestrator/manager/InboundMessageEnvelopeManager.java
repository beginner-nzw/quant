package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.model.message.MessageEnvelope;
import com.quant.common.model.message.SimpleMessageEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class InboundMessageEnvelopeManager {

    private final ObjectMapper objectMapper;

    public SimpleMessageEnvelope buildLogEnvelope(MessageEnvelope message,
                                                  String rawMessage,
                                                  String consumerService) {
        SimpleMessageEnvelope extracted = extractEnvelope(rawMessage);
        if (message != null) {
            extracted.setMessageId(firstNonBlank(message.getMessageId(), extracted.getMessageId()));
            extracted.setTaskId(firstNonBlank(message.getTaskId(), extracted.getTaskId()));
            extracted.setEventId(firstNonBlank(message.getEventId(), extracted.getEventId()));
            extracted.setMessageType(firstNonBlank(message.getMessageType(), extracted.getMessageType()));
            extracted.setSourceService(firstNonBlank(message.getSourceService(), extracted.getSourceService()));
            extracted.setTargetService(firstNonBlank(message.getTargetService(), extracted.getTargetService()));
            extracted.setTenantId(firstNonBlank(message.getTenantId(), extracted.getTenantId()));
            extracted.setBizKey(firstNonBlank(message.getBizKey(), extracted.getBizKey()));
            extracted.setRetryCount(message.getRetryCount() == null ? extracted.getRetryCount() : message.getRetryCount());
            extracted.setTimestamp(message.getTimestamp() == null ? extracted.getTimestamp() : message.getTimestamp());
            extracted.setVersion(firstNonBlank(message.getVersion(), extracted.getVersion()));
            extracted.setTraceId(firstNonBlank(message.getTraceId(), extracted.getTraceId()));
        }
        extracted.setTargetService(firstNonBlank(extracted.getTargetService(), consumerService));
        return extracted;
    }

    public String formatError(String errorCode, String errorMessage) {
        if (!StringUtils.hasText(errorCode)) {
            return errorMessage;
        }
        if (!StringUtils.hasText(errorMessage)) {
            return errorCode;
        }
        return errorCode + ": " + errorMessage;
    }

    private SimpleMessageEnvelope extractEnvelope(String rawMessage) {
        SimpleMessageEnvelope envelope = new SimpleMessageEnvelope();
        envelope.setMessageType("UNKNOWN");
        envelope.setSourceService("unknown-producer");
        envelope.setTargetService("ai-orchestration-service");
        envelope.setTenantId("default");
        envelope.setVersion("1.0");
        envelope.setRetryCount(0);
        envelope.setTimestamp(System.currentTimeMillis());
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            envelope.setMessageId(textValue(root, "messageId"));
            envelope.setTaskId(textValue(root, "taskId"));
            envelope.setEventId(firstNonBlank(
                    textValue(root, "eventId"),
                    nestedTextValue(root, "payload", "sourceEventId")
            ));
            envelope.setMessageType(firstNonBlank(textValue(root, "messageType"), envelope.getMessageType()));
            envelope.setSourceService(firstNonBlank(textValue(root, "sourceService"), envelope.getSourceService()));
            envelope.setTargetService(firstNonBlank(textValue(root, "targetService"), envelope.getTargetService()));
            envelope.setTenantId(firstNonBlank(textValue(root, "tenantId"), envelope.getTenantId()));
            envelope.setBizKey(textValue(root, "bizKey"));
            envelope.setTraceId(textValue(root, "traceId"));
            envelope.setVersion(firstNonBlank(textValue(root, "version"), envelope.getVersion()));
            JsonNode retryCountNode = root.get("retryCount");
            if (retryCountNode != null && retryCountNode.isNumber()) {
                envelope.setRetryCount(retryCountNode.asInt());
            }
            JsonNode timestampNode = root.get("timestamp");
            if (timestampNode != null && timestampNode.isNumber()) {
                envelope.setTimestamp(timestampNode.asLong());
            }
        } catch (Exception ignored) {
            // Fall through to relaxed extraction so common malformed key:value payloads still keep task/message metadata.
        }
        fillFromLooseMessage(rawMessage, envelope);
        if (!StringUtils.hasText(envelope.getMessageId())) {
            envelope.setMessageId("invalid-" + java.util.UUID.randomUUID());
        }
        return envelope;
    }

    private void fillFromLooseMessage(String rawMessage, SimpleMessageEnvelope envelope) {
        if (!StringUtils.hasText(rawMessage) || envelope == null) {
            return;
        }
        envelope.setMessageId(firstNonBlank(envelope.getMessageId(), looseStringValue(rawMessage, "messageId")));
        envelope.setTaskId(firstNonBlank(envelope.getTaskId(), looseStringValue(rawMessage, "taskId")));
        envelope.setEventId(firstNonBlank(envelope.getEventId(), looseStringValue(rawMessage, "eventId")));
        envelope.setMessageType(firstNonBlank(envelope.getMessageType(), looseStringValue(rawMessage, "messageType")));
        envelope.setSourceService(firstNonBlank(envelope.getSourceService(), looseStringValue(rawMessage, "sourceService")));
        envelope.setTargetService(firstNonBlank(envelope.getTargetService(), looseStringValue(rawMessage, "targetService")));
        envelope.setTenantId(firstNonBlank(envelope.getTenantId(), looseStringValue(rawMessage, "tenantId")));
        envelope.setBizKey(firstNonBlank(envelope.getBizKey(), looseStringValue(rawMessage, "bizKey")));
        envelope.setTraceId(firstNonBlank(envelope.getTraceId(), looseStringValue(rawMessage, "traceId")));
        envelope.setVersion(firstNonBlank(envelope.getVersion(), looseStringValue(rawMessage, "version")));

        Integer retryCount = looseIntegerValue(rawMessage, "retryCount");
        if (retryCount != null) {
            envelope.setRetryCount(retryCount);
        }

        Long timestamp = looseLongValue(rawMessage, "timestamp");
        if (timestamp != null) {
            envelope.setTimestamp(timestamp);
        }
    }

    private String looseStringValue(String rawMessage, String fieldName) {
        if (!StringUtils.hasText(rawMessage) || !StringUtils.hasText(fieldName)) {
            return null;
        }
        String quotedPattern = "\"?" + Pattern.quote(fieldName) + "\"?\\s*:\\s*\"([^\"]*)\"";
        Matcher quotedMatcher = Pattern.compile(quotedPattern).matcher(rawMessage);
        if (quotedMatcher.find()) {
            String value = quotedMatcher.group(1);
            return StringUtils.hasText(value) ? value : null;
        }

        String unquotedPattern = "(?:^|[,{])\\s*\"?" + Pattern.quote(fieldName) + "\"?\\s*:\\s*([^,}\\s]+)";
        Matcher unquotedMatcher = Pattern.compile(unquotedPattern).matcher(rawMessage);
        if (unquotedMatcher.find()) {
            String value = sanitizeLooseValue(unquotedMatcher.group(1));
            return StringUtils.hasText(value) ? value : null;
        }
        return null;
    }

    private Integer looseIntegerValue(String rawMessage, String fieldName) {
        String value = looseNumericValue(rawMessage, fieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long looseLongValue(String rawMessage, String fieldName) {
        String value = looseNumericValue(rawMessage, fieldName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String looseNumericValue(String rawMessage, String fieldName) {
        if (!StringUtils.hasText(rawMessage) || !StringUtils.hasText(fieldName)) {
            return null;
        }
        String numberPattern = "(?:^|[,{])\\s*\"?" + Pattern.quote(fieldName) + "\"?\\s*:\\s*(-?\\d+)";
        Matcher matcher = Pattern.compile(numberPattern).matcher(rawMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String sanitizeLooseValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String sanitized = value.trim();
        while (sanitized.endsWith(",") || sanitized.endsWith("}")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }
        return sanitized;
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private String nestedTextValue(JsonNode node, String parentFieldName, String childFieldName) {
        if (node == null || parentFieldName == null || childFieldName == null) {
            return null;
        }
        JsonNode parentNode = node.get(parentFieldName);
        if (parentNode == null || parentNode.isNull()) {
            return null;
        }
        return textValue(parentNode, childFieldName);
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return StringUtils.hasText(second) ? second : null;
    }
}
