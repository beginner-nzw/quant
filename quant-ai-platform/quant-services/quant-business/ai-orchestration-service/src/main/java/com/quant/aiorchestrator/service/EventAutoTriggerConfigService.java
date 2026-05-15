package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.EventAutoTriggerRuleUpdateDTO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerConfigVO;
import com.quant.aiorchestrator.domain.vo.EventAutoTriggerRuleItemVO;
import com.quant.common.core.exception.BizException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface EventAutoTriggerConfigService {
        public EventAutoTriggerConfig loadConfig();

        public EventAutoTriggerConfigVO loadConfigView();

        public void saveRule(String ruleCode, EventAutoTriggerRuleUpdateDTO dto);

        public EventAutoTriggerRule resolveMatchedRule(String eventType, String impactLevel);

        public EventAutoTriggerRule findEnabledRuleByCode(String ruleCode);

        public String resolveConfigPathForDisplay();
        @Data
        class EventAutoTriggerConfig {
                private Boolean enabled = false;
                private List<EventAutoTriggerRule> rules = List.of();
        }

        @Data
        class EventAutoTriggerRule {
                private String ruleCode;
                private String ruleName;
                private Boolean enabled = true;
                private List<String> eventTypes = List.of();
                private List<String> impactLevels = List.of();
                private String taskType;
                private String analysisScope;
                private String priority;
                private String sourceChannel;
                private String titleTemplate;
                private String remark;
        }
}
