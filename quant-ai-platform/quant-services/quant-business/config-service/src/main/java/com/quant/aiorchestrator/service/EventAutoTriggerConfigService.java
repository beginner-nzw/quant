package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.config.port.EventAutoTriggerConfigPort;

public interface EventAutoTriggerConfigService extends EventAutoTriggerConfigPort {

    EventAutoTriggerConfigVO loadConfigView();

    void saveRule(String ruleCode, EventAutoTriggerRuleUpdateDTO dto);

    String resolveConfigPathForDisplay();
}
