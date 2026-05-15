package com.quant.aiorchestrator.service.impl;

import com.quant.aiorchestrator.service.PromptTemplateConfigService;
import com.quant.aiorchestrator.service.*;

import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class PromptTemplateConfigServiceImpl implements PromptTemplateConfigService {

    private static final Map<String, String> TEMPLATE_FILE_MAP = Map.of(
            "planner_agent_template", "planner_agent_template.txt",
            "intent_agent_template", "intent_agent_template.txt",
            "financial_analysis_agent_template", "financial_analysis_agent_template.txt",
            "risk_review_agent_template", "risk_review_agent_template.txt",
            "report_generation_agent_template", "report_generation_agent_template.txt"
    );

    private final String promptTemplateDir;
    private final ConfigChangeAuditService configChangeAuditService;

    public PromptTemplateConfigServiceImpl(
            @Value("${quant.ai.prompt-template-dir:../../../prompt-templates}") String promptTemplateDir,
            ConfigChangeAuditService configChangeAuditService
    ) {
        this.promptTemplateDir = promptTemplateDir;
        this.configChangeAuditService = configChangeAuditService;
    }

    public String loadTemplateContent(String templateCode) {
        Path templatePath = resolveTemplatePath(templateCode);
        if (!Files.exists(templatePath)) {
            return "";
        }
        try {
            return Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("PROMPT_TEMPLATE_READ_FAILED", "读取 Prompt 模板失败: " + templateCode);
        }
    }

    public String resolveTemplatePathForDisplay(String templateCode) {
        return resolveTemplatePath(templateCode).toString();
    }

    public void saveTemplateContent(String templateCode, String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            throw new BizException("PROMPT_TEMPLATE_EMPTY", "Prompt 模板内容不能为空");
        }
        Path templatePath = resolveTemplatePath(templateCode);
        String normalizedContent = templateContent.trim();
        String beforeContent = loadTemplateContent(templateCode).trim();
        try {
            Files.createDirectories(templatePath.getParent());
            Files.writeString(templatePath, normalizedContent + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("PROMPT_TEMPLATE_SAVE_FAILED", "保存 Prompt 模板失败: " + templateCode);
        }
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

    private Path resolveTemplatePath(String templateCode) {
        String fileName = TEMPLATE_FILE_MAP.get(templateCode);
        if (fileName == null) {
            throw new BizException("PROMPT_TEMPLATE_UNSUPPORTED", "不支持的 Prompt 模板编码: " + templateCode);
        }

        Path templateDir = resolveTemplateDirectory(fileName);
        return templateDir.resolve(fileName).normalize();
    }

    private Path resolveTemplateDirectory(String fileName) {
        Path userDir = Paths.get(System.getProperty("user.dir")).normalize();
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();

        Path configuredPath = Paths.get(promptTemplateDir);
        if (configuredPath.isAbsolute()) {
            candidates.add(configuredPath.normalize());
        } else {
            candidates.add(userDir.resolve(configuredPath).normalize());
        }

        candidates.add(userDir.resolve("prompt-templates").normalize());
        candidates.add(userDir.resolve("quant-ai-platform").resolve("prompt-templates").normalize());

        List<Path> ancestors = new ArrayList<>();
        Path current = userDir;
        while (current != null) {
            ancestors.add(current);
            current = current.getParent();
        }

        for (Path ancestor : ancestors) {
            candidates.add(ancestor.resolve("prompt-templates").normalize());
            candidates.add(ancestor.resolve("quant-ai-platform").resolve("prompt-templates").normalize());
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve(fileName))) {
                return candidate;
            }
        }

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }

        return candidates.iterator().next();
    }
}
