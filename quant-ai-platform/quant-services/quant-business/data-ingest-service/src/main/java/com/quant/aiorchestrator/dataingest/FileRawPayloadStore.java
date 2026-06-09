package com.quant.aiorchestrator.dataingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.common.core.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class FileRawPayloadStore implements RawPayloadStore {

    private final String rootPath;
    private final ObjectMapper objectMapper;

    public FileRawPayloadStore(
            @Value("${quant.ai.data-ingest.raw-payload-dir:../../../ai-config/raw-payloads}") String rootPath,
            ObjectMapper objectMapper
    ) {
        this.rootPath = rootPath;
        this.objectMapper = objectMapper;
    }

    @Override
    public String save(String sourceCode, String stage, Object payload) {
        String normalizedSource = normalizeSegment(sourceCode, "UNKNOWN_SOURCE");
        String normalizedStage = normalizeSegment(stage, "UNKNOWN_STAGE");
        Path root = resolveRootPath();
        Path payloadPath = root
                .resolve(normalizedSource)
                .resolve(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmssSSS")) + "-" + UUID.randomUUID() + ".json")
                .normalize();
        try {
            Files.createDirectories(payloadPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            Files.writeString(payloadPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
            return "file://" + payloadPath.toString().replace('\\', '/')
                    + "#source=" + normalizedSource
                    + "&stage=" + normalizedStage;
        } catch (Exception e) {
            throw new BizException("DATA_INGEST_RAW_PAYLOAD_SAVE_FAILED", "save raw ingest payload failed");
        }
    }

    private Path resolveRootPath() {
        Path configured = Paths.get(rootPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(configured).normalize();
    }

    private String normalizeSegment(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        normalized = normalized.replaceAll("[^0-9A-Za-z._-]", "_").toUpperCase(Locale.ROOT);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }
}
