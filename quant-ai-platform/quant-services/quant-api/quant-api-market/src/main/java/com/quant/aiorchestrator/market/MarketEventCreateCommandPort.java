package com.quant.aiorchestrator.market;

import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;

public interface MarketEventCreateCommandPort {

    MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto, boolean recordHistory);

    MarketEventDuplicateProjection findDuplicatedEvent(MarketEventCreateDTO dto);
}
