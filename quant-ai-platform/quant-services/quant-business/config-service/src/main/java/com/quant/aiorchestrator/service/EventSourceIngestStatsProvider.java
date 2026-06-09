package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;

public interface EventSourceIngestStatsProvider {

    void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig);
}
