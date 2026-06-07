package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceRequestDiagnosticItemVO;
import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CninfoProxyAnnouncementManager {

    private final CninfoProxyAnnouncementParserManager parserManager;
    private final CninfoProxyUpstreamRequestManager upstreamRequestManager;

    public EventSourceRequestDiagnosticItemVO buildUpstreamRequestDiagnosticItem(EventSourceConfigItemVO sourceConfig,
                                                                                 MarketEventSourceSyncDTO dto) {
        return upstreamRequestManager.buildUpstreamRequestDiagnosticItem(sourceConfig, dto);
    }

    public List<CninfoProxyAnnouncementItemVO> loadUpstreamAnnouncements(EventSourceConfigItemVO sourceConfig,
                                                                         MarketEventSourceSyncDTO dto) {
        String upstreamUrl = trimToNull(sourceConfig == null ? null : sourceConfig.getUpstreamUrl());
        if (!StringUtils.hasText(upstreamUrl)) {
            return List.of();
        }

        String upstreamMethod = defaultValue(sourceConfig == null ? null : sourceConfig.getUpstreamMethod(), "GET").toUpperCase(Locale.ROOT);
        int timeoutSeconds = sourceConfig == null || sourceConfig.getRequestTimeoutSeconds() == null || sourceConfig.getRequestTimeoutSeconds() <= 0
                ? 15 : sourceConfig.getRequestTimeoutSeconds();

        HttpRequest request = upstreamRequestManager.buildRequest(upstreamUrl, upstreamMethod, timeoutSeconds, dto, sourceConfig);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 10)))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("CNINFO_PROXY_UPSTREAM_HTTP_FAILED", "巨潮上游接口调用失败，HTTP 状态码: " + response.statusCode());
            }
            return parserManager.parseUpstreamResponse(response.body(), sourceConfig);
        } catch (BizException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BizException("CNINFO_PROXY_UPSTREAM_REQUEST_FAILED", "巨潮上游接口请求失败");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
