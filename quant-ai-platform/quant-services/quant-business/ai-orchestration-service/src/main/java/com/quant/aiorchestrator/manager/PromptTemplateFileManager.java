package com.quant.aiorchestrator.manager;

import com.quant.common.core.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptTemplateFileManager {

    private final PromptTemplateCatalogManager promptTemplateCatalogManager;

    public String loadTemplateContent(String promptTemplateDir, String templateCode) {
        Path templatePath = resolveTemplatePath(promptTemplateDir, templateCode);
        if (!Files.exists(templatePath)) {
            return "";
        }
        try {
            return Files.readString(templatePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("PROMPT_TEMPLATE_READ_FAILED", "读取 Prompt 模板失败: " + templateCode);
        }
    }

    public void saveTemplateContent(String promptTemplateDir, String templateCode, String templateContent) {
        Path templatePath = resolveTemplatePath(promptTemplateDir, templateCode);
        try {
            Files.createDirectories(templatePath.getParent());
            Files.writeString(templatePath, templateContent + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException("PROMPT_TEMPLATE_SAVE_FAILED", "保存 Prompt 模板失败: " + templateCode);
        }
    }

    public Path resolveTemplatePath(String promptTemplateDir, String templateCode) {
        String fileName = promptTemplateCatalogManager.resolveFileName(templateCode);
        Path templateDir = resolveTemplateDirectory(promptTemplateDir, fileName);
        return templateDir.resolve(fileName).normalize();
    }

    private Path resolveTemplateDirectory(String promptTemplateDir, String fileName) {
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
