package com.quant.aiorchestrator.configstore;

import java.util.List;

public interface ConfigStoreAuditAppender {

    void appendAudit(String configType,
                     String targetCode,
                     String targetName,
                     String operation,
                     String configPath,
                     String changeSummary,
                     List<String> changedFields);
}
