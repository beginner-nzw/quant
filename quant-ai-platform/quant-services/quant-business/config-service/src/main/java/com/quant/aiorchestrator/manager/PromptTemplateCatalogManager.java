package com.quant.aiorchestrator.manager;

import com.quant.common.core.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptTemplateCatalogManager {

    private static final Map<String, String> TEMPLATE_FILE_MAP = Map.of(
            "planner_agent_template", "planner_agent_template.txt",
            "intent_agent_template", "intent_agent_template.txt",
            "financial_analysis_agent_template", "financial_analysis_agent_template.txt",
            "risk_review_agent_template", "risk_review_agent_template.txt",
            "report_generation_agent_template", "report_generation_agent_template.txt"
    );

    public String resolveFileName(String templateCode) {
        String fileName = TEMPLATE_FILE_MAP.get(templateCode);
        if (fileName == null) {
            throw new BizException("PROMPT_TEMPLATE_UNSUPPORTED", "Unsupported prompt template code: " + templateCode);
        }
        return fileName;
    }
}
