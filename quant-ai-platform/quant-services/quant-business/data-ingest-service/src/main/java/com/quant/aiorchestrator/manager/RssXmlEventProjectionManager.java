package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RssXmlEventProjectionManager {

    private final RssXmlElementReadManager elementReadManager;
    private final RssXmlEventItemAssembler itemAssembler;

    public List<MarketEventCreateDTO> parseResponse(String responseBody,
                                                    EventSourceConfigItemVO sourceConfig,
                                                    MarketEventSourceSyncDTO request) {
        try {
            return parseXmlResponse(responseBody, sourceConfig, request);
        } catch (BizException e) {
            if ("EVENT_SOURCE_RESPONSE_XML_INVALID".equals(e.getCode())) {
                return parseHtmlResponse(responseBody, sourceConfig, request);
            }
            throw e;
        }
    }

    private List<MarketEventCreateDTO> parseXmlResponse(String responseBody,
                                                        EventSourceConfigItemVO sourceConfig,
                                                        MarketEventSourceSyncDTO request) {
        try {
            Document document = elementReadManager.parseXml(responseBody);
            List<Element> itemElements = elementReadManager.resolveItemElements(
                    document == null ? null : document.getDocumentElement(),
                    trimToNull(sourceConfig == null ? null : sourceConfig.getResponseItemsField())
            );
            if (itemElements.isEmpty()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "RSS response does not contain any items");
            }

            List<MarketEventCreateDTO> result = new ArrayList<>();
            for (Element itemElement : itemElements) {
                if (itemElement != null) {
                    result.add(itemAssembler.toMarketEvent(itemElement, sourceConfig, request));
                }
            }
            if (result.isEmpty()) {
                throw new BizException("EVENT_SOURCE_RESPONSE_ITEMS_EMPTY", "RSS source returned no importable items");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("EVENT_SOURCE_RESPONSE_PARSE_FAILED", "RSS response parsing failed");
        }
    }

    private List<MarketEventCreateDTO> parseHtmlResponse(String responseBody,
                                                         EventSourceConfigItemVO sourceConfig,
                                                         MarketEventSourceSyncDTO request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BizException("EVENT_SOURCE_RESPONSE_EMPTY", "RSS response body is empty");
        }

        String baseUri = defaultValue(sourceConfig == null ? null : sourceConfig.getEndpointUrl(), "");
        org.jsoup.nodes.Document document = Jsoup.parse(responseBody, baseUri);
        org.jsoup.select.Elements anchors = document.select("a[href]");
        if (anchors.isEmpty()) {
            throw new BizException("EVENT_SOURCE_RESPONSE_HTML_ITEMS_EMPTY", "RSS fallback HTML response contains no links");
        }

        int itemCount = request == null || request.getItemCount() == null || request.getItemCount() <= 0
                ? 10 : request.getItemCount();
        List<MarketEventCreateDTO> result = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();
        for (org.jsoup.nodes.Element anchor : anchors) {
            String href = trimToNull(anchor.absUrl("href"));
            String title = trimToNull(anchor.text());
            if (!isHtmlSearchResultItem(href, title, sourceConfig)) {
                continue;
            }
            String fingerprint = title + "|" + href;
            if (!dedupe.add(fingerprint)) {
                continue;
            }
            result.add(itemAssembler.toMarketEventFromHtml(anchor, href, title, sourceConfig, request));
            if (result.size() >= itemCount) {
                break;
            }
        }

        if (result.isEmpty()) {
            throw new BizException("EVENT_SOURCE_RESPONSE_HTML_ITEMS_EMPTY", "RSS fallback HTML response returned no importable search results");
        }
        return result;
    }

    private boolean isHtmlSearchResultItem(String href, String title, EventSourceConfigItemVO sourceConfig) {
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title) || title.trim().length() < 6) {
            return false;
        }
        String normalizedHref = href.trim().toLowerCase(Locale.ROOT);
        if (!normalizedHref.startsWith("http")) {
            return false;
        }
        if (normalizedHref.contains("javascript:")
                || normalizedHref.contains("/news/search")
                || normalizedHref.contains("/search?")) {
            return false;
        }
        String sourceCode = sourceConfig == null ? "" : defaultValue(sourceConfig.getSourceCode(), "");
        if ("POLICY_TRACKER".equalsIgnoreCase(sourceCode)) {
            return normalizedHref.contains("gov.cn")
                    || title.contains("政策")
                    || title.contains("国务院")
                    || title.contains("中国政府网");
        }
        return !title.equalsIgnoreCase("bing")
                && !title.contains("登录")
                && !title.contains("设置");
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
