package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.aiorchestrator.manager.EventSourceConfigCommandManager;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventSourceConfigServiceImpl implements EventSourceConfigService {

    private final String configPath;
    private final EventSourceConfigCommandManager eventSourceConfigCommandManager;

    public EventSourceConfigServiceImpl(
            @Value("${quant.ai.event-source-config:../../../ai-config/event-source-configs.json}") String configPath,
            EventSourceConfigCommandManager eventSourceConfigCommandManager
    ) {
        this.configPath = configPath;
        this.eventSourceConfigCommandManager = eventSourceConfigCommandManager;
    }

    public EventSourceConfigVO loadConfigView() {
        return eventSourceConfigCommandManager.loadConfigView(configPath);
    }

    public List<EventSourceConfigItemVO> loadSources() {
        return eventSourceConfigCommandManager.loadSources(configPath);
    }

    public EventSourceConfigItemVO findSource(String sourceCode) {
        return eventSourceConfigCommandManager.findSource(configPath, sourceCode);
    }

    public void saveSource(String sourceCode, EventSourceConfigUpdateDTO dto) {
        eventSourceConfigCommandManager.saveSource(configPath, sourceCode, dto);
    }

    public String resolveConfigPathForDisplay() {
        return eventSourceConfigCommandManager.resolveConfigPathForDisplay(configPath);
    }
}
