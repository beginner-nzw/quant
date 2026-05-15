package com.quant.aiorchestrator.service;

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

public interface PromptTemplateConfigService {
        public String loadTemplateContent(String templateCode);

        public String resolveTemplatePathForDisplay(String templateCode);

        public void saveTemplateContent(String templateCode, String templateContent);
}
