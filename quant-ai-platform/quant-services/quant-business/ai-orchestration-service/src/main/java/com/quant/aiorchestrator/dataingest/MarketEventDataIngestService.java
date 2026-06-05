package com.quant.aiorchestrator.dataingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.common.core.exception.BizException;
import com.quant.common.messaging.KafkaTopicConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventDataIngestService implements DataIngestService {

    private final List<EventSourceSyncAdapter> eventSourceSyncAdapters;
    private final RawPayloadStore rawPayloadStore;
    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${quant.ai.data-ingest.max-attempts:3}")
    private Integer maxAttempts;

    @Override
    public SourceIngestResult ingestMarketEventSource(EventSourceConfigItemVO sourceConfig,
                                                      MarketEventSourceSyncDTO request,
                                                      SourceImportHandler importHandler) {
        validateRequest(sourceConfig, request, importHandler);
        EventSourceSyncAdapter adapter = resolveAdapter(sourceConfig);
        int attempts = resolveMaxAttempts();
        SourceFetchResult fetchResult = null;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                fetchResult = adapter.fetch(sourceConfig, request);
                fetchResult.setAttemptNo(attempt);
                fetchResult.setMaxAttempts(attempts);
                ensureProvenance(fetchResult, sourceConfig, request);
                attachRawPayloadRef(fetchResult, sourceConfig, "FETCHED");
                if (fetchResult.getStandardizedEvents() == null || fetchResult.getStandardizedEvents().isEmpty()) {
                    throw new BizException("DATA_INGEST_STANDARDIZED_EMPTY", "source adapter returned no standardized market events");
                }

                MarketEventBatchImportDTO importDTO = new MarketEventBatchImportDTO();
                importDTO.setEvents(fetchResult.getStandardizedEvents());
                MarketEventBatchImportResultVO imported = importHandler.importEvents(importDTO, buildSourceDetail(sourceConfig, request, fetchResult));
                fetchResult.setStatus(SourceFetchStatus.STANDARDIZED);
                return SourceIngestResult.builder()
                        .fetchResult(fetchResult)
                        .importResult(imported)
                        .build();
            } catch (RuntimeException e) {
                lastFailure = e;
                fetchResult = failedFetch(sourceConfig, request, attempt, attempts, e);
                attachRawPayloadRef(fetchResult, sourceConfig, attempt >= attempts ? "DEADLETTER" : "RETRY");
                appendFetchFailureHistory(sourceConfig, request, fetchResult);
                if (attempt >= attempts) {
                    publishDeadletter(sourceConfig, request, fetchResult);
                    throw e;
                }
                log.warn("data ingest source fetch failed, sourceCode={}, attempt={}/{}",
                        sourceConfig.getSourceCode(), attempt, attempts, e);
            }
        }

        throw lastFailure == null
                ? new BizException("DATA_INGEST_FAILED", "source ingest failed")
                : lastFailure;
    }

    private void validateRequest(EventSourceConfigItemVO sourceConfig,
                                 MarketEventSourceSyncDTO request,
                                 SourceImportHandler importHandler) {
        if (sourceConfig == null || !StringUtils.hasText(sourceConfig.getSourceCode())) {
            throw new BizException("DATA_INGEST_SOURCE_EMPTY", "data ingest source config cannot be empty");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getEnabled())) {
            throw new BizException("DATA_INGEST_SOURCE_DISABLED", "data ingest source is disabled");
        }
        if (request == null) {
            throw new BizException("DATA_INGEST_REQUEST_EMPTY", "data ingest request cannot be empty");
        }
        if (importHandler == null) {
            throw new BizException("DATA_INGEST_IMPORT_HANDLER_EMPTY", "data ingest import handler cannot be empty");
        }
    }

    private EventSourceSyncAdapter resolveAdapter(EventSourceConfigItemVO sourceConfig) {
        return eventSourceSyncAdapters.stream()
                .filter(item -> item.supports(sourceConfig))
                .findFirst()
                .orElseThrow(() -> new BizException("DATA_INGEST_ADAPTER_UNSUPPORTED", "no data ingest source adapter supports current source"));
    }

    private void ensureProvenance(SourceFetchResult fetchResult,
                                  EventSourceConfigItemVO sourceConfig,
                                  MarketEventSourceSyncDTO request) {
        if (fetchResult.getStatus() == null) {
            fetchResult.setStatus(SourceFetchStatus.FETCHED);
        }
        if (fetchResult.getProvenance() == null) {
            fetchResult.setProvenance(SourceProvenance.from(sourceConfig, request.getTargetCode()));
        }
    }

    private void attachRawPayloadRef(SourceFetchResult fetchResult,
                                     EventSourceConfigItemVO sourceConfig,
                                     String stage) {
        if (fetchResult == null) {
            return;
        }
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("status", fetchResult.getStatus());
        rawPayload.put("provenance", fetchResult.getProvenance());
        rawPayload.put("httpStatus", fetchResult.getHttpStatus());
        rawPayload.put("attemptNo", fetchResult.getAttemptNo());
        rawPayload.put("maxAttempts", fetchResult.getMaxAttempts());
        rawPayload.put("errorCode", fetchResult.getErrorCode());
        rawPayload.put("errorMessage", fetchResult.getErrorMessage());
        rawPayload.put("standardizedEvents", fetchResult.getStandardizedEvents());
        String ref = rawPayloadStore.save(sourceConfig.getSourceCode(), stage, rawPayload);
        if (fetchResult.getProvenance() == null) {
            fetchResult.setProvenance(SourceProvenance.from(sourceConfig, null));
        }
        fetchResult.getProvenance().setRawPayloadRef(ref);
    }

    private SourceFetchResult failedFetch(EventSourceConfigItemVO sourceConfig,
                                          MarketEventSourceSyncDTO request,
                                          int attempt,
                                          int attempts,
                                          RuntimeException e) {
        return SourceFetchResult.builder()
                .status(attempt >= attempts ? SourceFetchStatus.DEADLETTERED : SourceFetchStatus.FETCH_FAILED)
                .provenance(SourceProvenance.from(sourceConfig, request == null ? null : request.getTargetCode()))
                .standardizedEvents(List.of())
                .attemptNo(attempt)
                .maxAttempts(attempts)
                .errorCode(resolveErrorCode(e))
                .errorMessage(e == null ? null : e.getMessage())
                .build();
    }

    private void appendFetchFailureHistory(EventSourceConfigItemVO sourceConfig,
                                           MarketEventSourceSyncDTO request,
                                           SourceFetchResult fetchResult) {
        marketEventIngestHistoryService.appendHistory(
                "SOURCE_SYNC",
                defaultValue(sourceConfig.getSourceName(), "data ingest source"),
                sourceConfig.getSourceCode(),
                sourceConfig.getSourceName(),
                sourceConfig.getSourceCategory(),
                sourceConfig.getSourceChannel(),
                buildSourceDetail(sourceConfig, request, fetchResult),
                0,
                0,
                1,
                0,
                0,
                fetchResult.getStatus().name(),
                fetchResult.getErrorMessage(),
                "source fetch failed; retry=" + fetchResult.getAttemptNo() + "/" + fetchResult.getMaxAttempts()
        );
    }

    private void publishDeadletter(EventSourceConfigItemVO sourceConfig,
                                   MarketEventSourceSyncDTO request,
                                   SourceFetchResult fetchResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", UUID.randomUUID().toString());
        payload.put("messageType", "MARKET_EVENT_SOURCE_INGEST_DEADLETTER");
        payload.put("sourceService", "data-ingest");
        payload.put("targetService", "deadletter-queue");
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("sourceCode", sourceConfig.getSourceCode());
        payload.put("ingestMode", sourceConfig.getIngestMode());
        payload.put("request", request);
        payload.put("fetchStatus", fetchResult.getStatus());
        payload.put("rawPayloadRef", fetchResult.getProvenance() == null ? null : fetchResult.getProvenance().getRawPayloadRef());
        payload.put("errorCode", fetchResult.getErrorCode());
        payload.put("errorMessage", fetchResult.getErrorMessage());
        payload.put("createdAt", LocalDateTime.now().toString());
        try {
            kafkaTemplate.send(KafkaTopicConstants.MARKET_EVENT_DEADLETTER, sourceConfig.getSourceCode(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("publish market event ingest deadletter failed, sourceCode={}", sourceConfig.getSourceCode(), e);
        }
    }

    private String buildSourceDetail(EventSourceConfigItemVO sourceConfig,
                                     MarketEventSourceSyncDTO request,
                                     SourceFetchResult fetchResult) {
        String target = request == null ? null : request.getTargetCode();
        String rawRef = fetchResult == null || fetchResult.getProvenance() == null
                ? null : fetchResult.getProvenance().getRawPayloadRef();
        return String.format(
                "data-ingest source=%s mode=%s target=%s status=%s rawPayloadRef=%s",
                defaultValue(sourceConfig.getSourceCode(), "UNKNOWN"),
                defaultValue(sourceConfig.getIngestMode(), "UNKNOWN"),
                defaultValue(target, "UNKNOWN"),
                fetchResult == null ? "UNKNOWN" : fetchResult.getStatus(),
                defaultValue(rawRef, "NONE")
        );
    }

    private int resolveMaxAttempts() {
        return maxAttempts == null || maxAttempts < 1 ? 1 : Math.min(maxAttempts, 5);
    }

    private String resolveErrorCode(RuntimeException e) {
        if (e instanceof BizException bizException && StringUtils.hasText(bizException.getCode())) {
            return bizException.getCode();
        }
        return "DATA_INGEST_FETCH_FAILED";
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
