package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.manager.PromptTemplateAuditManager;
import com.quant.aiorchestrator.manager.PromptTemplateFileManager;
import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class PromptTemplateConfigServiceImpl implements PromptTemplateConfigService {

    private final String promptTemplateDir;
    private final PromptTemplateFileManager promptTemplateFileManager;
    private final PromptTemplateAuditManager promptTemplateAuditManager;

    public PromptTemplateConfigServiceImpl(
            @Value("${quant.ai.prompt-template-dir:../../../prompt-templates}") String promptTemplateDir,
            PromptTemplateFileManager promptTemplateFileManager,
            PromptTemplateAuditManager promptTemplateAuditManager
    ) {
        this.promptTemplateDir = promptTemplateDir;
        this.promptTemplateFileManager = promptTemplateFileManager;
        this.promptTemplateAuditManager = promptTemplateAuditManager;
    }

    public String loadTemplateContent(String templateCode) {
        return promptTemplateFileManager.loadTemplateContent(promptTemplateDir, templateCode);
    }

    public String resolveTemplatePathForDisplay(String templateCode) {
        return promptTemplateFileManager.resolveTemplatePath(promptTemplateDir, templateCode).toString();
    }

    public void saveTemplateContent(String templateCode, String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            throw new BizException("PROMPT_TEMPLATE_EMPTY", "Prompt 模板内容不能为空");
        }
        String normalizedContent = templateContent.trim();
        String beforeContent = loadTemplateContent(templateCode).trim();
        Path templatePath = promptTemplateFileManager.resolveTemplatePath(promptTemplateDir, templateCode);
        promptTemplateFileManager.saveTemplateContent(promptTemplateDir, templateCode, normalizedContent);
        promptTemplateAuditManager.appendTemplateUpdateAudit(templateCode, templatePath, beforeContent, normalizedContent);
    }
}
