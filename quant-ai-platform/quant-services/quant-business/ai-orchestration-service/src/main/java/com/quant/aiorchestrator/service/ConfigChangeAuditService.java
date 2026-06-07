package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;

import java.util.List;

public interface ConfigChangeAuditService {

    void appendAudit(String configType,
                     String targetCode,
                     String targetName,
                     String operation,
                     String configPath,
                     String changeSummary,
                     List<String> changedFields);

    List<ConfigChangeAuditItemVO> loadRecentAudits();
}
