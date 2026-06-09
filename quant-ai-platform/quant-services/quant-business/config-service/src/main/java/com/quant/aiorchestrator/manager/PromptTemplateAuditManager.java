package com.quant.aiorchestrator.manager;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptTemplateAuditManager {

    private final ConfigStoreAuditAppender configStoreAuditAppender;

    public void appendTemplateUpdateAudit(String templateCode,
                                          Path templatePath,
                                          String beforeContent,
                                          String normalizedContent) {
        configStoreAuditAppender.appendAudit(
                "PROMPT_TEMPLATE",
                templateCode,
                templateCode,
                "UPDATE",
                templatePath.toString(),
                beforeContent.equals(normalizedContent)
                        ? "Prompt template saved again with unchanged content"
                        : "Prompt template content updated",
                List.of("templateContent")
        );
    }
}
