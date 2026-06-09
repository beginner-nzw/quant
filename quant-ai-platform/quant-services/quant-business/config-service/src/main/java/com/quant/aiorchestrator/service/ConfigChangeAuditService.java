package com.quant.aiorchestrator.service;

import com.quant.aiorchestrator.configstore.ConfigStoreAuditAppender;
import com.quant.aiorchestrator.domain.vo.ConfigChangeAuditItemVO;

import java.util.List;

public interface ConfigChangeAuditService extends ConfigStoreAuditAppender {

    List<ConfigChangeAuditItemVO> loadRecentAudits();
}
