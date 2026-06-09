package com.quant.aiorchestrator.market;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;

import java.util.List;

public interface EventSourceConfigReadPort {

    List<EventSourceConfigItemVO> loadSources();

    EventSourceConfigItemVO findSource(String sourceCode);
}
