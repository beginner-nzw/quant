package com.quant.aiorchestrator.service;

public interface PromptTemplateConfigService {
    String loadTemplateContent(String templateCode);

    String resolveTemplatePathForDisplay(String templateCode);

    void saveTemplateContent(String templateCode, String templateContent);
}
