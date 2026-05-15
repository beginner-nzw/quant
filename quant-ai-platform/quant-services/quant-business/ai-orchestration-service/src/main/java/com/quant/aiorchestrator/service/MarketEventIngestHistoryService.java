package com.quant.aiorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MarketEventIngestHistoryService {
        public void appendHistory(String sourceType,
                                  String sourceLabel,
                                  String sourceCode,
                                  String sourceName,
                                  String sourceCategory,
                                  String sourceChannel,
                                  String sourceDetail,
                                  Integer totalCount,
                                  Integer successCount,
                                  Integer failedCount,
                                  Integer duplicateCount,
                                  Integer autoTriggeredCount,
                                  String summary);

        public void appendHistory(String sourceType,
                                  String sourceLabel,
                                  String sourceCode,
                                  String sourceName,
                                  String sourceCategory,
                                  String sourceChannel,
                                  String sourceDetail,
                                  Integer totalCount,
                                  Integer successCount,
                                  Integer failedCount,
                                  Integer duplicateCount,
                                  Integer autoTriggeredCount,
                                  String resultStatus,
                                  String errorMessage,
                                  String summary);

        public List<MarketEventIngestHistoryItemVO> loadRecentHistory();
}
