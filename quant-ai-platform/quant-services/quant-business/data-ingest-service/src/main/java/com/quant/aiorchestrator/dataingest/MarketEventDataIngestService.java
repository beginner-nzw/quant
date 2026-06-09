package com.quant.aiorchestrator.dataingest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventIngestRunDO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.market.MarketDataIngestStableContract;
import com.quant.aiorchestrator.mapper.MarketEventIngestRunMapper;
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
    private final MarketEventIngestRunMapper marketEventIngestRunMapper;
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
        String ingestRunId = UUID.randomUUID().toString();
        SourceFetchResult fetchResult = null;
        String lastRawPayloadRef = null;
        SourceProvenance lastProvenance = null;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                SourceRawPayload rawPayload = adapter.fetchRaw(sourceConfig, request);
                SourceProvenance provenance = rawPayload == null || rawPayload.getProvenance() == null
                        ? SourceProvenance.from(sourceConfig, request.getTargetCode())
                        : rawPayload.getProvenance();
                String rawPayloadRef = rawPayloadStore.save(sourceConfig.getSourceCode(), "FETCHED", rawPayload);
                provenance.setRawPayloadRef(rawPayloadRef);
                lastRawPayloadRef = rawPayloadRef;
                lastProvenance = provenance;

                fetchResult = SourceFetchResult.builder()
                        .status(SourceFetchStatus.FETCHED)
                        .provenance(provenance)
                        .rawPayloadRef(rawPayloadRef)
                        .httpStatus(rawPayload == null ? null : rawPayload.getHttpStatus())
                        .standardizedEvents(adapter.standardize(rawPayload, sourceConfig, request))
                        .attemptNo(attempt)
                        .maxAttempts(attempts)
                        .build();
                recordIngestRun(ingestRunId, sourceConfig, request, fetchResult, null);
                if (fetchResult.getStandardizedEvents() == null || fetchResult.getStandardizedEvents().isEmpty()) {
                    throw new BizException("DATA_INGEST_STANDARDIZED_EMPTY", "source adapter returned no standardized market events");
                }

                MarketEventBatchImportDTO importDTO = new MarketEventBatchImportDTO();
                importDTO.setEvents(fetchResult.getStandardizedEvents());
                MarketEventBatchImportResultVO imported = importHandler.importEvents(importDTO, buildSourceDetail(sourceConfig, request, fetchResult));
                fetchResult.setStatus(SourceFetchStatus.STANDARDIZED);
                recordIngestRun(ingestRunId, sourceConfig, request, fetchResult, imported);
                return SourceIngestResult.builder()
                        .fetchResult(fetchResult)
                        .importResult(imported)
                        .build();
            } catch (RuntimeException e) {
                lastFailure = e;
                fetchResult = failedFetch(sourceConfig, request, attempt, attempts, e);
                if (StringUtils.hasText(lastRawPayloadRef)) {
                    fetchResult.setProvenance(lastProvenance);
                    fetchResult.setRawPayloadRef(lastRawPayloadRef);
                } else {
                    attachFailurePayloadRef(fetchResult, sourceConfig, request, attempt >= attempts ? "DEADLETTER" : "RETRY");
                }
                recordIngestRun(ingestRunId, sourceConfig, request, fetchResult, null);
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

    private void attachFailurePayloadRef(SourceFetchResult fetchResult,
                                         EventSourceConfigItemVO sourceConfig,
                                         MarketEventSourceSyncDTO request,
                                         String stage) {
        if (fetchResult == null) {
            return;
        }
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        rawPayload.put("status", fetchResult.getStatus());
        rawPayload.put("provenance", fetchResult.getProvenance());
        rawPayload.put("request", request);
        rawPayload.put("attemptNo", fetchResult.getAttemptNo());
        rawPayload.put("maxAttempts", fetchResult.getMaxAttempts());
        rawPayload.put("errorCode", fetchResult.getErrorCode());
        rawPayload.put("errorMessage", fetchResult.getErrorMessage());
        String ref = rawPayloadStore.save(sourceConfig.getSourceCode(), stage, rawPayload);
        if (fetchResult.getProvenance() == null) {
            fetchResult.setProvenance(SourceProvenance.from(sourceConfig, null));
        }
        fetchResult.getProvenance().setRawPayloadRef(ref);
        fetchResult.setRawPayloadRef(ref);
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
                MarketDataIngestStableContract.SOURCE_SYNC_OPERATION,
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

    private void recordIngestRun(String ingestRunId,
                                 EventSourceConfigItemVO sourceConfig,
                                 MarketEventSourceSyncDTO request,
                                 SourceFetchResult fetchResult,
                                 MarketEventBatchImportResultVO importResult) {
        try {
            MarketEventIngestRunDO entity = new MarketEventIngestRunDO();
            entity.setIngestRunId(ingestRunId);
            entity.setSourceCode(sourceConfig.getSourceCode());
            entity.setSourceName(sourceConfig.getSourceName());
            entity.setSourceCategory(sourceConfig.getSourceCategory());
            entity.setSourceChannel(sourceConfig.getSourceChannel());
            entity.setIngestMode(sourceConfig.getIngestMode());
            entity.setRequestTarget(request == null ? null : request.getTargetCode());
            entity.setFetchStatus(fetchResult == null || fetchResult.getStatus() == null ? "UNKNOWN" : fetchResult.getStatus().name());
            entity.setRawPayloadRef(resolveRawPayloadRef(fetchResult));
            entity.setRetryCount(fetchResult == null || fetchResult.getAttemptNo() == null ? 0 : Math.max(0, fetchResult.getAttemptNo() - 1));
            entity.setMaxRetryCount(fetchResult == null || fetchResult.getMaxAttempts() == null ? 0 : fetchResult.getMaxAttempts());
            entity.setDeadlettered(fetchResult != null && SourceFetchStatus.DEADLETTERED.equals(fetchResult.getStatus()) ? 1 : 0);
            entity.setTotalCount(importResult == null || importResult.getTotalCount() == null ? 0 : importResult.getTotalCount());
            entity.setSuccessCount(importResult == null || importResult.getSuccessCount() == null ? 0 : importResult.getSuccessCount());
            entity.setFailedCount(importResult == null || importResult.getFailedCount() == null ? 0 : importResult.getFailedCount());
            entity.setDuplicateCount(importResult == null || importResult.getDuplicateCount() == null ? 0 : importResult.getDuplicateCount());
            entity.setAutoTriggeredCount(importResult == null || importResult.getAutoTriggeredCount() == null ? 0 : importResult.getAutoTriggeredCount());
            entity.setErrorCode(fetchResult == null ? null : fetchResult.getErrorCode());
            entity.setErrorMessage(fetchResult == null ? null : fetchResult.getErrorMessage());
            LocalDateTime now = LocalDateTime.now();
            MarketEventIngestRunDO existing = marketEventIngestRunMapper.selectOne(
                    new LambdaQueryWrapper<MarketEventIngestRunDO>()
                            .eq(MarketEventIngestRunDO::getIngestRunId, ingestRunId)
                            .last("limit 1")
            );
            if (existing == null) {
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                marketEventIngestRunMapper.insert(entity);
            } else {
                entity.setId(existing.getId());
                entity.setCreatedAt(existing.getCreatedAt());
                entity.setUpdatedAt(now);
                marketEventIngestRunMapper.updateById(entity);
            }
        } catch (Exception e) {
            log.warn("record market event ingest run failed, ingestRunId={}, sourceCode={}",
                    ingestRunId, sourceConfig == null ? null : sourceConfig.getSourceCode(), e);
        }
    }

    private String resolveRawPayloadRef(SourceFetchResult fetchResult) {
        if (fetchResult == null) {
            return null;
        }
        if (StringUtils.hasText(fetchResult.getRawPayloadRef())) {
            return fetchResult.getRawPayloadRef();
        }
        return fetchResult.getProvenance() == null ? null : fetchResult.getProvenance().getRawPayloadRef();
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
