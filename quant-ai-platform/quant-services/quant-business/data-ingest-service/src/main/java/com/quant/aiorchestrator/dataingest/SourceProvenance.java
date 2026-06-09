package com.quant.aiorchestrator.dataingest;

import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
@Builder
public class SourceProvenance {
    private String sourceCode;
    private String sourceName;
    private String sourceCategory;
    private String sourceChannel;
    private String ingestMode;
    private String requestTarget;
    private String rawPayloadRef;

    public static SourceProvenance from(EventSourceConfigItemVO sourceConfig, String requestTarget) {
        return SourceProvenance.builder()
                .sourceCode(normalize(sourceConfig == null ? null : sourceConfig.getSourceCode()))
                .sourceName(normalize(sourceConfig == null ? null : sourceConfig.getSourceName()))
                .sourceCategory(normalize(sourceConfig == null ? null : sourceConfig.getSourceCategory()))
                .sourceChannel(normalize(sourceConfig == null ? null : sourceConfig.getSourceChannel()))
                .ingestMode(normalize(sourceConfig == null ? null : sourceConfig.getIngestMode()))
                .requestTarget(normalize(requestTarget))
                .build();
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
