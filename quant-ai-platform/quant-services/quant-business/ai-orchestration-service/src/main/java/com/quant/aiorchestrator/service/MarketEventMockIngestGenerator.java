package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MarketEventMockIngestGenerator {

    private final EventSourceConfigService eventSourceConfigService;

    public List<MarketEventCreateDTO> generate(MarketEventMockIngestDTO dto) {
        int itemCount = dto.getItemCount() == null ? 3 : Math.max(1, Math.min(dto.getItemCount(), 10));
        String targetType = StringUtils.hasText(dto.getTargetType()) ? dto.getTargetType().trim() : "STOCK";
        String targetCode = dto.getTargetCode().trim();
        String targetName = dto.getTargetName().trim();
        String sourcePreset = dto.getSourcePreset().trim().toUpperCase(Locale.ROOT);
        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(sourcePreset);

        List<MarketEventCreateDTO> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < itemCount; i++) {
            MarketEventCreateDTO item = new MarketEventCreateDTO();
            item.setTargetType(targetType);
            item.setTargetCode(targetCode);
            item.setTargetName(targetName);
            item.setOccurredAt(now.minusHours(i + 1L));
            fillByPreset(item, targetCode, targetName, sourcePreset, sourceConfig, i + 1);
            events.add(item);
        }
        return events;
    }

    private void fillByPreset(MarketEventCreateDTO item,
                              String targetCode,
                              String targetName,
                              String sourcePreset,
                              EventSourceConfigItemVO sourceConfig,
                              int sequence) {
        switch (sourcePreset) {
            case "EXCHANGE_ANNOUNCEMENT" -> {
                item.setEventType(resolveDefaultEventType(sourceConfig, "ANNOUNCEMENT"));
                item.setSourceChannel(resolveSourceChannel(sourceConfig, "EXCHANGE_FEED"));
                item.setImpactLevel(resolveImpactLevel(sourceConfig, sequence == 1 ? "HIGH" : "MEDIUM"));
                item.setEventStatus("ACTIVE");
                item.setEventTitle(targetName + "发布交易所公告-" + sequence);
                item.setEventSummary(targetName + "出现新的交易所公告更新，建议跟踪公告披露内容、市场解读和估值影响。");
                item.setSourceUrl("https://mock.exchange.local/" + targetCode + "/announcement/" + sequence);
            }
            case "POLICY_TRACKER" -> {
                item.setEventType(resolveDefaultEventType(sourceConfig, "POLICY"));
                item.setSourceChannel(resolveSourceChannel(sourceConfig, "POLICY_MONITOR"));
                item.setImpactLevel(resolveImpactLevel(sourceConfig, sequence == 1 ? "HIGH" : "MEDIUM"));
                item.setEventStatus("ACTIVE");
                item.setEventTitle(targetName + "相关政策动态监测-" + sequence);
                item.setEventSummary("模拟政策跟踪源发现与" + targetName + "相关的监管或行业政策变化，建议评估经营影响和情绪传导。");
                item.setSourceUrl("https://mock.policy.local/" + targetCode + "/policy/" + sequence);
            }
            case "RISK_MONITOR" -> {
                item.setEventType(resolveDefaultEventType(sourceConfig, "RISK_ALERT"));
                item.setSourceChannel(resolveSourceChannel(sourceConfig, "RISK_MONITOR"));
                item.setImpactLevel(resolveImpactLevel(sourceConfig, "HIGH"));
                item.setEventStatus("ACTIVE");
                item.setEventTitle(targetName + "风险监测提醒-" + sequence);
                item.setEventSummary("模拟风险监测源发现" + targetName + "存在需要重点复核的风险信号，建议尽快跟踪风险点和处置动作。");
                item.setSourceUrl("https://mock.risk.local/" + targetCode + "/alert/" + sequence);
            }
            default -> {
                item.setEventType(resolveDefaultEventType(sourceConfig, "NEWS"));
                item.setSourceChannel(resolveSourceChannel(sourceConfig, "NEWS_FEED"));
                item.setImpactLevel(resolveImpactLevel(sourceConfig, sequence == 1 ? "HIGH" : "MEDIUM"));
                item.setEventStatus("ACTIVE");
                item.setEventTitle(targetName + "新闻快讯监测-" + sequence);
                item.setEventSummary("模拟新闻源推送了一条与" + targetName + "相关的市场快讯，建议结合已有研究结论继续跟踪。");
                item.setSourceUrl("https://mock.news.local/" + targetCode + "/flash/" + sequence);
            }
        }
    }

    private String resolveDefaultEventType(EventSourceConfigItemVO sourceConfig, String fallback) {
        return sourceConfig != null && StringUtils.hasText(sourceConfig.getDefaultEventType())
                ? sourceConfig.getDefaultEventType().trim()
                : fallback;
    }

    private String resolveSourceChannel(EventSourceConfigItemVO sourceConfig, String fallback) {
        return sourceConfig != null && StringUtils.hasText(sourceConfig.getSourceChannel())
                ? sourceConfig.getSourceChannel().trim()
                : fallback;
    }

    private String resolveImpactLevel(EventSourceConfigItemVO sourceConfig, String fallback) {
        return sourceConfig != null && StringUtils.hasText(sourceConfig.getDefaultImpactLevel())
                ? sourceConfig.getDefaultImpactLevel().trim()
                : fallback;
    }
}
