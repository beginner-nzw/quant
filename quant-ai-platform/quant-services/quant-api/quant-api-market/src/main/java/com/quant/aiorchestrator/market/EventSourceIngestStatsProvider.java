package com.quant.aiorchestrator.market;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;

public interface EventSourceIngestStatsProvider {

    void enrichEventSourceConfigStats(EventSourceConfigVO eventSourceConfig);
}
