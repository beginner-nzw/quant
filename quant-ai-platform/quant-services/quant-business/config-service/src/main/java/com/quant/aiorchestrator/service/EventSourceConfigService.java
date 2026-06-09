package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;
import com.quant.aiorchestrator.market.EventSourceConfigReadPort;

import java.util.List;

public interface EventSourceConfigService extends EventSourceConfigReadPort {

    EventSourceConfigVO loadConfigView();

    @Override
    List<EventSourceConfigItemVO> loadSources();

    @Override
    EventSourceConfigItemVO findSource(String sourceCode);

    void saveSource(String sourceCode, EventSourceConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
