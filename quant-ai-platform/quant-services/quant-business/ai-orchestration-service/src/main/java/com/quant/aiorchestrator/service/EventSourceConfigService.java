package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.EventSourceConfigUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigVO;

import java.util.List;

public interface EventSourceConfigService {

    EventSourceConfigVO loadConfigView();

    List<EventSourceConfigItemVO> loadSources();

    EventSourceConfigItemVO findSource(String sourceCode);

    void saveSource(String sourceCode, EventSourceConfigUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
