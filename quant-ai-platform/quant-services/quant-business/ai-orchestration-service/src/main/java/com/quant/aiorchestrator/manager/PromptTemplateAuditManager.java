package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.service.ConfigChangeAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptTemplateAuditManager {

    private final ConfigChangeAuditService configChangeAuditService;

    public void appendTemplateUpdateAudit(String templateCode,
                                          Path templatePath,
                                          String beforeContent,
                                          String normalizedContent) {
        configChangeAuditService.appendAudit(
                "PROMPT_TEMPLATE",
                templateCode,
                templateCode,
                "UPDATE",
                templatePath.toString(),
                beforeContent.equals(normalizedContent) ? "重新保存 Prompt 模板，内容未变化" : "更新 Prompt 模板内容",
                List.of("templateContent")
        );
    }
}
