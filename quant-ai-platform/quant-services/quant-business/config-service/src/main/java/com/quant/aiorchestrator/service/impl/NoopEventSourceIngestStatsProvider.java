package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.aiorchestrator.service.EventSourceIngestStatsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EventSourceIngestStatsProvider.class)
public class NoopEventSourceIngestStatsProvider implements EventSourceIngestStatsProvider {

    @Override
    public void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig) {
        // Data-ingest-service provides the real enrichment adapter when present.
    }
}
