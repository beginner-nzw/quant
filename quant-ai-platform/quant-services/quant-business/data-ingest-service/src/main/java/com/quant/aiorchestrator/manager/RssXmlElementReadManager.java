package com.quant.aiorchestrator.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RssXmlElementReadManager {

    private static final List<String> DEFAULT_ITEM_PATHS = List.of(
            "rss.channel.item",
            "channel.item",
            "feed.entry",
            "entry",
            "item"
    );

    private final ObjectMapper objectMapper;

    public Document parseXml(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("EVENT_SOURCE_RESPONSE_EMPTY", "RSS response body is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            disableExternalEntities(factory);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(responseBody)));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_RESPONSE_XML_INVALID", "RSS response is not valid XML");
        }
    }

    public List<Element> resolveItemElements(Element root, String responseItemsField) {
        if (root == null) {
            return List.of();
        }
        List<String> candidatePaths = new ArrayList<>();
        if (StringUtils.hasText(responseItemsField)) {
            candidatePaths.add(responseItemsField);
        }
        candidatePaths.addAll(DEFAULT_ITEM_PATHS);
        for (String path : candidatePaths) {
            List<Element> resolved = resolveElements(root, path);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return List.of();
    }

    public String readMappedText(Element itemElement,
                                 Map<String, List<String>> fieldMappings,
                                 String canonicalField,
                                 String... fallbackFieldNames) {
        List<String> mappingFields = fieldMappings.getOrDefault(canonicalField, List.of());
        if (!mappingFields.isEmpty()) {
            String mappedValue = readText(itemElement, mappingFields.toArray(String[]::new));
            if (StringUtils.hasText(mappedValue)) {
                return mappedValue;
            }
        }
        return readText(itemElement, fallbackFieldNames);
    }

    public Map<String, List<String>> parseFieldMappings(String rawFieldMappingJson) {
        String value = trimToNull(rawFieldMappingJson);
        if (!StringUtils.hasText(value)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            if (root == null || !root.isObject()) {
                throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "RSS field mapping must be a JSON object");
            }
            Map<String, List<String>> mappings = new LinkedHashMap<>();
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = root.get(fieldName);
                if (!StringUtils.hasText(fieldName) || fieldNode == null || fieldNode.isNull()) {
                    continue;
                }
                List<String> rawFields = new ArrayList<>();
                if (fieldNode.isTextual()) {
                    rawFields.add(fieldNode.asText(""));
                } else if (fieldNode.isArray()) {
                    for (JsonNode item : fieldNode) {
                        if (item != null && item.isTextual() && StringUtils.hasText(item.asText())) {
                            rawFields.add(item.asText().trim());
                        }
                    }
                }
                List<String> normalizedFields = rawFields.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .distinct()
                        .toList();
                if (!normalizedFields.isEmpty()) {
                    mappings.put(fieldName.trim(), normalizedFields);
                }
            }
            return mappings;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_FIELD_MAPPING_INVALID", "RSS field mapping parsing failed");
        }
    }

    private void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Exception ignored) {
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
        }
        factory.setExpandEntityReferences(false);
    }

    private List<Element> resolveElements(Element root, String fieldPath) {
        if (root == null || !StringUtils.hasText(fieldPath)) {
            return List.of();
        }
        List<String> segments = List.of(fieldPath.trim().split("\\."));
        List<Element> current = new ArrayList<>();
        current.add(root);

        int startIndex = matchesElementName(root, segments.get(0)) ? 1 : 0;
        for (int index = startIndex; index < segments.size(); index++) {
            String segment = segments.get(index);
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            List<Element> next = new ArrayList<>();
            for (Element element : current) {
                next.addAll(findDirectChildElements(element, segment));
            }
            if (next.isEmpty()) {
                return List.of();
            }
            current = next;
        }
        return current;
    }

    private List<Element> findDirectChildElements(Element parent, String expectedName) {
        List<Element> result = new ArrayList<>();
        if (parent == null || !StringUtils.hasText(expectedName)) {
            return result;
        }
        NodeList childNodes = parent.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node child = childNodes.item(index);
            if (child instanceof Element element && matchesElementName(element, expectedName)) {
                result.add(element);
            }
        }
        return result;
    }

    private boolean matchesElementName(Element element, String expectedName) {
        if (element == null || !StringUtils.hasText(expectedName)) {
            return false;
        }
        String normalizedExpected = normalizeElementName(expectedName);
        String nodeName = normalizeElementName(element.getNodeName());
        String localName = normalizeElementName(element.getLocalName());
        return normalizedExpected.equalsIgnoreCase(nodeName)
                || normalizedExpected.equalsIgnoreCase(localName);
    }

    private String normalizeElementName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        int colonIndex = normalized.indexOf(':');
        return colonIndex >= 0 ? normalized.substring(colonIndex + 1) : normalized;
    }

    private String readText(Element itemElement, String... fieldPaths) {
        if (itemElement == null || fieldPaths == null) {
            return null;
        }
        for (String fieldPath : fieldPaths) {
            String value = resolveFieldPathValue(itemElement, fieldPath);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveFieldPathValue(Element root, String fieldPath) {
        if (root == null || !StringUtils.hasText(fieldPath)) {
            return null;
        }
        List<String> segments = new ArrayList<>();
        for (String segment : fieldPath.trim().split("\\.")) {
            if (StringUtils.hasText(segment)) {
                segments.add(segment.trim());
            }
        }
        if (segments.isEmpty()) {
            return null;
        }

        String attributeName = null;
        String lastSegment = segments.get(segments.size() - 1);
        if (lastSegment.startsWith("@") && lastSegment.length() > 1) {
            attributeName = lastSegment.substring(1);
            segments.remove(segments.size() - 1);
        }

        Element current = root;
        for (String segment : segments) {
            current = findFirstDirectChildElement(current, segment);
            if (current == null) {
                return null;
            }
        }

        if (StringUtils.hasText(attributeName)) {
            return trimToNull(current.getAttribute(attributeName));
        }

        String text = trimToNull(current.getTextContent());
        if (StringUtils.hasText(text)) {
            return text;
        }
        return trimToNull(current.getAttribute("href"));
    }

    private Element findFirstDirectChildElement(Element parent, String expectedName) {
        List<Element> matched = findDirectChildElements(parent, expectedName);
        return matched.isEmpty() ? null : matched.get(0);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
