package com.quant.aiorchestrator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventRelationDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.CninfoProxyAnnouncementResponseVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventIngestHistoryItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventRelationVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import com.quant.aiorchestrator.service.CninfoProxyAnnouncementService;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.MarketEventMockIngestGenerator;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.aiorchestrator.service.MarketEventService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.common.core.exception.BizException;
import com.quant.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketEventServiceImpl implements MarketEventService {

    private static final String FOLLOW_UP_STATUS_NOT_TRACKED = "NOT_TRACKED";
    private static final String FOLLOW_UP_STATUS_TRACKING = "TRACKING";
    private static final String FOLLOW_UP_STATUS_COMPLETED = "COMPLETED";
    private static final String FOLLOW_UP_STATUS_FAILED = "FAILED";
    private static final String AUTO_TRIGGER_DISABLED = "DISABLED";
    private static final String AUTO_TRIGGER_NO_MATCH = "NO_MATCH";
    private static final String AUTO_TRIGGER_SUCCESS = "SUCCESS";
    private static final String AUTO_TRIGGER_FAILED = "FAILED";
    private static final String AUTO_TRIGGER_WILL_TRIGGER = "WILL_TRIGGER";
    private static final String AUTO_TRIGGER_SKIPPED_DUPLICATE = "SKIPPED_DUPLICATE";
    private static final String AUTO_TRIGGER_INVALID = "INVALID";
    private static final String DUPLICATE_SOURCE_EXISTING = "EXISTING_EVENT";
    private static final String DUPLICATE_SOURCE_BATCH = "SAME_BATCH";
    private static final Map<String, String> TARGET_TYPE_ALIASES = buildTargetTypeAliases();
    private static final Map<String, String> EVENT_TYPE_ALIASES = buildEventTypeAliases();
    private static final Map<String, String> IMPACT_LEVEL_ALIASES = buildImpactLevelAliases();
    private static final Map<String, String> EVENT_STATUS_ALIASES = buildEventStatusAliases();
    private static final Map<String, String> SOURCE_CHANNEL_ALIASES = buildSourceChannelAliases();

    private final MarketEventMapper marketEventMapper;
    private final MarketEventRelationMapper marketEventRelationMapper;
    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchReportMapper researchReportMapper;
    private final RiskWarningMapper riskWarningMapper;
    private final RiskWarningDetailMapper riskWarningDetailMapper;
    private final ObjectMapper objectMapper;
    private final EventAutoTriggerConfigService eventAutoTriggerConfigService;
    private final MarketEventAutoTriggerService marketEventAutoTriggerService;
    private final MarketEventMockIngestGenerator marketEventMockIngestGenerator;
    private final MarketEventIngestHistoryService marketEventIngestHistoryService;
    private final EventSourceConfigService eventSourceConfigService;
    private final List<EventSourceSyncAdapter> eventSourceSyncAdapters;
    private final CninfoProxyAnnouncementService cninfoProxyAnnouncementService;
    private final MarketEventStandardizedPublisherService marketEventStandardizedPublisherService;

    @Override
    public MarketEventPageVO pageMarketEvents(MarketEventPageQueryDTO queryDTO) {
        MarketEventPageQueryDTO safeQuery = queryDTO == null ? new MarketEventPageQueryDTO() : queryDTO;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : safeQuery.getPageSize();

        List<MarketEventListItemVO> matchedRecords = listMatchedEvents(safeQuery);
        int fromIndex = Math.min((pageNum - 1) * pageSize, matchedRecords.size());
        int toIndex = Math.min(fromIndex + pageSize, matchedRecords.size());

        MarketEventPageVO vo = new MarketEventPageVO();
        vo.setTotal((long) matchedRecords.size());
        vo.setPageNum((long) pageNum);
        vo.setPageSize((long) pageSize);
        vo.setRecords(fromIndex >= toIndex ? List.of() : matchedRecords.subList(fromIndex, toIndex));
        return vo;
    }

    @Override
    public MarketEventStatsVO getMarketEventStats() {
        MarketEventStatsVO vo = new MarketEventStatsVO();
        vo.setTotalCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)));
        vo.setActiveCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getEventStatus, "ACTIVE")));
        vo.setHighImpactCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getImpactLevel, "HIGH")));
        vo.setTrackedCount(countTrackedMarketEvents());
        LocalDate today = LocalDate.now();
        vo.setTodayCount(countMarketEvents(new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .ge(MarketEventDO::getOccurredAt, today.atStartOfDay())
                .lt(MarketEventDO::getOccurredAt, today.plusDays(1).atStartOfDay())));
        return vo;
    }

    @Override
    public MarketEventListItemVO getMarketEvent(String eventId) {
        MarketEventDO event = marketEventMapper.selectOne(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getEventId, eventId)
                        .eq(MarketEventDO::getDeleted, 0)
                        .last("limit 1")
        );
        if (event == null) {
            throw new BizException("MARKET_EVENT_NOT_FOUND", "市场事件不存在");
        }

        Map<String, List<ResearchTaskDO>> followUpTaskMap = loadFollowUpTaskMap(List.of(eventId));
        Map<String, List<MarketEventRelationDO>> relationMap = loadRelationMap(List.of(eventId));
        Map<String, ResearchReportDO> latestReportMap = loadLatestReportMap(followUpTaskMap.getOrDefault(eventId, List.of()));
        Map<String, RiskWarningDO> latestRiskWarningMap = loadLatestRiskWarningMap(followUpTaskMap.getOrDefault(eventId, List.of()));
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMap(latestRiskWarningMap.values().stream()
                .map(RiskWarningDO::getWarningId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        return toMarketEventItem(
                event,
                relationMap.getOrDefault(eventId, List.of()),
                followUpTaskMap.getOrDefault(eventId, List.of()),
                latestReportMap,
                latestRiskWarningMap,
                riskWarningDetailMap
        );
    }

    @Override
    public List<MarketEventIngestHistoryItemVO> listMarketEventIngestHistory() {
        return marketEventIngestHistoryService.loadRecentHistory();
    }

    @Override
    public MarketEventCreateResultVO createMarketEvent(MarketEventCreateDTO dto) {
        return executeCreateMarketEvent(dto, true);
    }

    private MarketEventCreateResultVO executeCreateMarketEvent(MarketEventCreateDTO dto, boolean recordHistory) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_EMPTY", "市场事件内容不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new BizException("MARKET_EVENT_TYPE_EMPTY", "事件类型不能为空");
        }
        if (!StringUtils.hasText(dto.getEventTitle())) {
            throw new BizException("MARKET_EVENT_TITLE_EMPTY", "事件标题不能为空");
        }
        if (!StringUtils.hasText(dto.getEventSummary())) {
            throw new BizException("MARKET_EVENT_SUMMARY_EMPTY", "事件摘要不能为空");
        }
        if (!StringUtils.hasText(dto.getImpactLevel())) {
            throw new BizException("MARKET_EVENT_IMPACT_EMPTY", "影响等级不能为空");
        }
        if (dto.getOccurredAt() == null) {
            throw new BizException("MARKET_EVENT_OCCURRED_AT_EMPTY", "事件发生时间不能为空");
        }

        MarketEventCreateDTO prepared = prepareCreateInput(dto);
        MarketEventDO duplicated = findDuplicatedEvent(prepared);
        if (duplicated != null) {
            MarketEventCreateResultVO result = buildCreateResult(duplicated, true, "命中已有市场事件记录");
            if (recordHistory) {
                appendManualCreateHistory(prepared, result);
            }
            return result;
        }

        String targetType = prepared.getTargetType();
        String targetCode = prepared.getTargetCode();
        String targetName = prepared.getTargetName();
        String eventType = prepared.getEventType();
        String eventTitle = prepared.getEventTitle();
        String eventSummary = prepared.getEventSummary();
        String sourceChannel = prepared.getSourceChannel();
        String sourceUrl = prepared.getSourceUrl();
        String impactLevel = prepared.getImpactLevel();
        String eventStatus = prepared.getEventStatus();

        LocalDateTime now = LocalDateTime.now();
        MarketEventDO event = new MarketEventDO();
        event.setEventId(UUID.randomUUID().toString());
        event.setTargetType(targetType);
        event.setTargetCode(targetCode);
        event.setTargetName(targetName);
        event.setEventType(eventType);
        event.setEventTitle(eventTitle);
        event.setEventSummary(eventSummary);
        event.setSourceChannel(sourceChannel);
        event.setSourceUrl(sourceUrl);
        event.setImpactLevel(impactLevel);
        event.setEventStatus(eventStatus);
        event.setOccurredAt(prepared.getOccurredAt());
        event.setCreatedBy(String.valueOf(SecurityUtils.currentUserId()));
        event.setCreatedAt(now);
        event.setDeleted(0);
        marketEventMapper.insert(event);
        saveEventRelations(event, prepared.getRelations());

        marketEventAutoTriggerService.prepareAutoTrigger(event);
        marketEventStandardizedPublisherService.publish(event);
        MarketEventCreateResultVO result = buildCreateResult(event, false, "市场事件创建成功");
        if (recordHistory) {
            appendManualCreateHistory(prepared, result);
        }
        return result;
    }

    @Override
    public MarketEventBatchPreviewResultVO previewImportMarketEvents(MarketEventBatchImportDTO dto) {
        List<MarketEventCreateDTO> events = dto == null || dto.getEvents() == null ? List.of() : dto.getEvents();
        if (events.isEmpty()) {
            throw new BizException("MARKET_EVENT_BATCH_IMPORT_EMPTY", "批量导入事件不能为空");
        }

        EventAutoTriggerConfigService.EventAutoTriggerConfig autoTriggerConfig = eventAutoTriggerConfigService.loadConfig();
        boolean autoTriggerEnabled = Boolean.TRUE.equals(autoTriggerConfig.getEnabled());
        Map<String, Integer> batchDuplicateTracker = new LinkedHashMap<>();
        List<MarketEventBatchPreviewItemVO> items = new java.util.ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;
        int duplicateCount = 0;
        int autoTriggerCandidateCount = 0;

        for (int i = 0; i < events.size(); i++) {
            MarketEventCreateDTO current = events.get(i);
            MarketEventBatchPreviewItemVO item = new MarketEventBatchPreviewItemVO();
            item.setItemNo(i + 1);
            item.setTargetCode(current == null ? null : trimToNull(current.getTargetCode()));
            item.setTargetName(current == null ? null : trimToNull(current.getTargetName()));
            item.setEventTitle(current == null ? null : trimToNull(current.getEventTitle()));

            try {
                MarketEventCreateDTO prepared = prepareCreateInput(current);
                item.setValid(true);
                item.setNormalizedTargetCode(prepared.getTargetCode());
                item.setNormalizedEventType(prepared.getEventType());
                item.setNormalizedImpactLevel(prepared.getImpactLevel());
                item.setNormalizedEventStatus(prepared.getEventStatus());
                item.setNormalizedSourceChannel(prepared.getSourceChannel());

                String duplicateFingerprint = buildDuplicateFingerprint(prepared);
                Integer firstSeenIndex = duplicateFingerprint == null ? null : batchDuplicateTracker.get(duplicateFingerprint);
                MarketEventDO duplicated = firstSeenIndex == null ? findDuplicatedEvent(prepared) : null;
                String duplicateSource = firstSeenIndex != null ? DUPLICATE_SOURCE_BATCH : duplicated != null ? DUPLICATE_SOURCE_EXISTING : null;

                item.setDuplicate(StringUtils.hasText(duplicateSource));
                item.setImportable(!StringUtils.hasText(duplicateSource));
                item.setDuplicateSource(duplicateSource);
                item.setExistingEventId(duplicated == null ? null : duplicated.getEventId());
                if (!StringUtils.hasText(duplicateSource) && duplicateFingerprint != null) {
                    batchDuplicateTracker.put(duplicateFingerprint, i + 1);
                } else if (StringUtils.hasText(duplicateSource)) {
                    duplicateCount++;
                }

                EventAutoTriggerConfigService.EventAutoTriggerRule matchedRule = null;
                String autoTriggerStatus;
                if (StringUtils.hasText(duplicateSource)) {
                    autoTriggerStatus = AUTO_TRIGGER_SKIPPED_DUPLICATE;
                } else if (!autoTriggerEnabled) {
                    autoTriggerStatus = AUTO_TRIGGER_DISABLED;
                } else {
                    matchedRule = resolveMatchedRule(autoTriggerConfig, prepared.getEventType(), prepared.getImpactLevel());
                    if (matchedRule == null) {
                        autoTriggerStatus = AUTO_TRIGGER_NO_MATCH;
                    } else {
                        autoTriggerStatus = AUTO_TRIGGER_WILL_TRIGGER;
                        autoTriggerCandidateCount++;
                    }
                }

                item.setAutoTriggerStatus(autoTriggerStatus);
                item.setAutoTriggerRuleCode(matchedRule == null ? null : matchedRule.getRuleCode());
                item.setEstimatedTaskType(matchedRule == null ? null : matchedRule.getTaskType());
                item.setMessage(resolvePreviewMessage(duplicateSource, duplicated, firstSeenIndex, autoTriggerStatus, matchedRule));
                validCount++;
            } catch (Exception e) {
                item.setValid(false);
                item.setImportable(false);
                item.setDuplicate(false);
                item.setInvalidField(resolvePreviewInvalidField(e));
                item.setAutoTriggerStatus(AUTO_TRIGGER_INVALID);
                item.setMessage(trimMessage(resolveExceptionMessage(e), 255));
                invalidCount++;
            }
            items.add(item);
        }

        MarketEventBatchPreviewResultVO result = new MarketEventBatchPreviewResultVO();
        result.setTotalCount(events.size());
        result.setValidCount(validCount);
        result.setInvalidCount(invalidCount);
        result.setDuplicateCount(duplicateCount);
        result.setAutoTriggerCandidateCount(autoTriggerCandidateCount);
        result.setItems(items);
        return result;
    }

    @Override
    public MarketEventBatchImportResultVO importMarketEvents(MarketEventBatchImportDTO dto) {
        return executeBatchImport(dto, "BATCH_IMPORT", "批量导入", "BATCH_IMPORT", "批量导入", "IMPORT", resolveBatchSourceChannel(dto == null ? null : dto.getEvents()), null);
    }

    @Override
    public MarketEventBatchImportResultVO mockIngestMarketEvents(MarketEventMockIngestDTO dto) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_MOCK_INGEST_EMPTY", "模拟接入请求不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }
        if (!StringUtils.hasText(dto.getSourcePreset())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_EMPTY", "模拟来源不能为空");
        }
        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(dto.getSourcePreset());
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_NOT_FOUND", "模拟来源配置不存在");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getEnabled())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_DISABLED", "模拟来源已禁用");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getSupportsMockIngest())) {
            throw new BizException("MARKET_EVENT_SOURCE_PRESET_UNSUPPORTED", "当前来源不支持模拟接入");
        }
        MarketEventBatchImportDTO importDTO = new MarketEventBatchImportDTO();
        importDTO.setEvents(marketEventMockIngestGenerator.generate(dto));
        return executeBatchImport(
                importDTO,
                "MOCK_INGEST",
                "模拟接入",
                defaultIfBlank(sourceConfig.getSourceCode(), dto.getSourcePreset()),
                defaultIfBlank(sourceConfig.getSourceName(), "模拟接入"),
                defaultIfBlank(sourceConfig.getSourceCategory(), "MOCK"),
                defaultIfBlank(sourceConfig.getSourceChannel(), null),
                buildMockSourceDetail(dto)
        );
    }

    @Override
    public MarketEventBatchImportResultVO syncMarketEventSource(String sourceCode, MarketEventSourceSyncDTO dto) {
        if (!StringUtils.hasText(sourceCode)) {
            throw new BizException("MARKET_EVENT_SOURCE_CODE_EMPTY", "事件源编码不能为空");
        }
        if (dto == null) {
            throw new BizException("MARKET_EVENT_SOURCE_SYNC_EMPTY", "事件源同步请求不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }

        EventSourceConfigItemVO sourceConfig = eventSourceConfigService.findSource(sourceCode);
        if (sourceConfig == null) {
            throw new BizException("MARKET_EVENT_SOURCE_NOT_FOUND", "事件源配置不存在");
        }
        if (!Boolean.TRUE.equals(sourceConfig.getEnabled())) {
            throw new BizException("MARKET_EVENT_SOURCE_DISABLED", "事件源已禁用");
        }

        EventSourceSyncAdapter adapter = eventSourceSyncAdapters.stream()
                .filter(item -> item.supports(sourceConfig))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            throw new BizException("MARKET_EVENT_SOURCE_SYNC_UNSUPPORTED", "当前事件源不支持同步");
        }

        String sourceDetail = buildSourceSyncDetail(sourceConfig, dto);
        try {
        MarketEventBatchImportDTO importDTO = new MarketEventBatchImportDTO();
        importDTO.setEvents(adapter.sync(sourceConfig, dto));
        return executeBatchImport(
                importDTO,
                "SOURCE_SYNC",
                defaultIfBlank(sourceConfig.getSourceName(), "事件源同步"),
                defaultIfBlank(sourceConfig.getSourceCode(), sourceCode),
                defaultIfBlank(sourceConfig.getSourceName(), "事件源同步"),
                defaultIfBlank(sourceConfig.getSourceCategory(), "SOURCE"),
                defaultIfBlank(sourceConfig.getSourceChannel(), null),
                sourceDetail
        );
        } catch (Exception e) {
            appendFailedIngestHistory(
                    "SOURCE_SYNC",
                    defaultIfBlank(sourceConfig.getSourceName(), "事件源同步"),
                    defaultIfBlank(sourceConfig.getSourceCode(), sourceCode),
                    defaultIfBlank(sourceConfig.getSourceName(), "事件源同步"),
                    defaultIfBlank(sourceConfig.getSourceCategory(), "SOURCE"),
                    defaultIfBlank(sourceConfig.getSourceChannel(), null),
                    sourceDetail,
                    e
            );
            throw e;
        }
    }

    @Override
    public CninfoProxyAnnouncementResponseVO previewCninfoProxyAnnouncements(MarketEventSourceSyncDTO dto) {
        return cninfoProxyAnnouncementService.previewAnnouncements(dto);
    }

    private MarketEventBatchImportResultVO executeBatchImport(MarketEventBatchImportDTO dto,
                                                              String sourceType,
                                                              String sourceLabel,
                                                              String sourceCode,
                                                              String sourceName,
                                                              String sourceCategory,
                                                              String sourceChannel,
        String sourceDetail) {
        List<MarketEventCreateDTO> events = dto == null || dto.getEvents() == null ? List.of() : dto.getEvents();
        if (events.isEmpty()) {
            throw new BizException("MARKET_EVENT_BATCH_IMPORT_EMPTY", "批量导入事件不能为空");
        }

        List<MarketEventBatchImportItemVO> items = new java.util.ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int duplicateCount = 0;
        int autoTriggeredCount = 0;

        for (int i = 0; i < events.size(); i++) {
            MarketEventCreateDTO item = events.get(i);
            MarketEventBatchImportItemVO resultItem = new MarketEventBatchImportItemVO();
            resultItem.setItemNo(i + 1);
            resultItem.setTargetCode(item == null ? null : trimToNull(item.getTargetCode()));
            resultItem.setTargetName(item == null ? null : trimToNull(item.getTargetName()));
            resultItem.setEventTitle(item == null ? null : trimToNull(item.getEventTitle()));

            try {
                MarketEventCreateResultVO created = executeCreateMarketEvent(item, false);
                resultItem.setSuccess(true);
                resultItem.setDuplicate(created != null && Boolean.TRUE.equals(created.getDuplicate()));
                resultItem.setEventId(created == null ? null : created.getEventId());
                resultItem.setAutoTriggerStatus(created == null ? null : created.getAutoTriggerStatus());
                resultItem.setAutoTriggerTaskId(created == null ? null : created.getAutoTriggerTaskId());
                resultItem.setMessage(resolveBatchImportMessage(created));
                successCount++;
                if (created != null && Boolean.TRUE.equals(created.getDuplicate())) {
                    duplicateCount++;
                }
                if (created != null
                        && !Boolean.TRUE.equals(created.getDuplicate())
                        && marketEventAutoTriggerService.shouldCountAsQueued(created.getAutoTriggerStatus())) {
                    autoTriggeredCount++;
                }
            } catch (Exception e) {
                resultItem.setSuccess(false);
                resultItem.setDuplicate(false);
                resultItem.setMessage(trimMessage(resolveExceptionMessage(e), 255));
                failedCount++;
            }
            items.add(resultItem);
        }

        MarketEventBatchImportResultVO result = new MarketEventBatchImportResultVO();
        result.setTotalCount(events.size());
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setDuplicateCount(duplicateCount);
        result.setAutoTriggeredCount(autoTriggeredCount);
        result.setItems(items);
        appendBatchIngestHistory(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                StringUtils.hasText(sourceDetail) ? sourceDetail : buildBatchSourceDetail(events),
                result
        );
        return result;
    }

    private MarketEventCreateDTO prepareCreateInput(MarketEventCreateDTO dto) {
        if (dto == null) {
            throw new BizException("MARKET_EVENT_EMPTY", "市场事件内容不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetCode())) {
            throw new BizException("MARKET_EVENT_TARGET_CODE_EMPTY", "标的代码不能为空");
        }
        if (!StringUtils.hasText(dto.getTargetName())) {
            throw new BizException("MARKET_EVENT_TARGET_NAME_EMPTY", "标的名称不能为空");
        }
        if (!StringUtils.hasText(dto.getEventType())) {
            throw new BizException("MARKET_EVENT_TYPE_EMPTY", "事件类型不能为空");
        }
        if (!StringUtils.hasText(dto.getEventTitle())) {
            throw new BizException("MARKET_EVENT_TITLE_EMPTY", "事件标题不能为空");
        }
        if (!StringUtils.hasText(dto.getEventSummary())) {
            throw new BizException("MARKET_EVENT_SUMMARY_EMPTY", "事件摘要不能为空");
        }
        if (!StringUtils.hasText(dto.getImpactLevel())) {
            throw new BizException("MARKET_EVENT_IMPACT_EMPTY", "影响等级不能为空");
        }
        if (dto.getOccurredAt() == null) {
            throw new BizException("MARKET_EVENT_OCCURRED_AT_EMPTY", "事件发生时间不能为空");
        }

        MarketEventCreateDTO prepared = new MarketEventCreateDTO();
        prepared.setTargetType(normalizeTargetType(dto.getTargetType()));
        prepared.setTargetCode(normalizeTargetCode(dto.getTargetCode()));
        prepared.setTargetName(trimToNull(dto.getTargetName()));
        prepared.setEventType(normalizeEventType(dto.getEventType()));
        prepared.setEventTitle(trimToNull(dto.getEventTitle()));
        prepared.setEventSummary(trimToNull(dto.getEventSummary()));
        prepared.setSourceChannel(normalizeSourceChannel(dto.getSourceChannel(), prepared.getEventType()));
        prepared.setSourceUrl(trimToNull(dto.getSourceUrl()));
        prepared.setImpactLevel(normalizeImpactLevel(dto.getImpactLevel()));
        prepared.setEventStatus(normalizeEventStatus(dto.getEventStatus()));
        prepared.setOccurredAt(dto.getOccurredAt());
        prepared.setRelations(normalizeRelations(dto.getRelations()));
        return prepared;
    }

    private List<MarketEventRelationDTO> normalizeRelations(List<MarketEventRelationDTO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        Map<String, MarketEventRelationDTO> normalized = new LinkedHashMap<>();
        for (MarketEventRelationDTO relation : relations) {
            if (relation == null) {
                continue;
            }
            String relationType = normalizeRelationType(relation.getRelationType());
            String relationCode = normalizeTargetCode(relation.getRelationCode());
            if (!StringUtils.hasText(relationCode)) {
                continue;
            }
            MarketEventRelationDTO item = new MarketEventRelationDTO();
            item.setRelationType(relationType);
            item.setRelationCode(relationCode);
            item.setRelationName(trimToNull(relation.getRelationName()));
            item.setRelationWeight(relation.getRelationWeight());
            normalized.put(relationType + "|" + relationCode, item);
        }
        return List.copyOf(normalized.values());
    }

    private void saveEventRelations(MarketEventDO event, List<MarketEventRelationDTO> extraRelations) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            return;
        }
        Map<String, MarketEventRelationDTO> relations = new LinkedHashMap<>();
        MarketEventRelationDTO primary = new MarketEventRelationDTO();
        primary.setRelationType(normalizeRelationType(event.getTargetType()));
        primary.setRelationCode(normalizeTargetCode(event.getTargetCode()));
        primary.setRelationName(trimToNull(event.getTargetName()));
        primary.setRelationWeight(java.math.BigDecimal.ONE);
        if (StringUtils.hasText(primary.getRelationCode())) {
            relations.put(primary.getRelationType() + "|" + primary.getRelationCode(), primary);
        }
        if (extraRelations != null) {
            for (MarketEventRelationDTO relation : extraRelations) {
                if (relation == null || !StringUtils.hasText(relation.getRelationCode())) {
                    continue;
                }
                relations.put(relation.getRelationType() + "|" + relation.getRelationCode(), relation);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (MarketEventRelationDTO relation : relations.values()) {
            MarketEventRelationDO entity = new MarketEventRelationDO();
            entity.setEventId(event.getEventId());
            entity.setRelationType(relation.getRelationType());
            entity.setRelationCode(relation.getRelationCode());
            entity.setRelationName(relation.getRelationName());
            entity.setRelationWeight(relation.getRelationWeight());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setDeleted(0);
            marketEventRelationMapper.insert(entity);
        }
    }

    private String buildDuplicateFingerprint(MarketEventCreateDTO dto) {
        if (dto == null || dto.getOccurredAt() == null) {
            return null;
        }
        String targetCode = normalizeTargetCode(dto.getTargetCode());
        String eventType = normalizeEventType(dto.getEventType());
        String sourceUrl = trimToNull(dto.getSourceUrl());
        if (StringUtils.hasText(sourceUrl)) {
            return String.join("|", defaultIfBlank(targetCode, ""), defaultIfBlank(eventType, ""), sourceUrl.toLowerCase(Locale.ROOT));
        }
        String eventTitle = trimToNull(dto.getEventTitle());
        if (!StringUtils.hasText(eventTitle)) {
            return null;
        }
        return String.join("|", defaultIfBlank(targetCode, ""), defaultIfBlank(eventType, ""), eventTitle.toLowerCase(Locale.ROOT), String.valueOf(dto.getOccurredAt()));
    }

    private EventAutoTriggerConfigService.EventAutoTriggerRule resolveMatchedRule(
            EventAutoTriggerConfigService.EventAutoTriggerConfig config,
            String eventType,
            String impactLevel
    ) {
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return null;
        }
        return config.getRules().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .filter(item -> matchesIgnoreCase(item.getEventTypes(), eventType))
                .filter(item -> matchesIgnoreCase(item.getImpactLevels(), impactLevel))
                .findFirst()
                .orElse(null);
    }

    private String resolvePreviewMessage(String duplicateSource,
                                         MarketEventDO duplicated,
                                         Integer firstSeenIndex,
                                         String autoTriggerStatus,
                                         EventAutoTriggerConfigService.EventAutoTriggerRule matchedRule) {
        if (DUPLICATE_SOURCE_BATCH.equalsIgnoreCase(duplicateSource)) {
            return firstSeenIndex == null ? "与本批次其他记录重复" : "与本次导入第 " + firstSeenIndex + " 条记录重复";
        }
        if (DUPLICATE_SOURCE_EXISTING.equalsIgnoreCase(duplicateSource)) {
            return duplicated == null ? "命中已有事件" : "命中已有事件：" + duplicated.getEventId();
        }
        if (AUTO_TRIGGER_DISABLED.equalsIgnoreCase(autoTriggerStatus)) {
            return "事件自动触发已关闭";
        }
        if (AUTO_TRIGGER_NO_MATCH.equalsIgnoreCase(autoTriggerStatus)) {
            return "未命中自动触发规则";
        }
        if (AUTO_TRIGGER_WILL_TRIGGER.equalsIgnoreCase(autoTriggerStatus)) {
            return matchedRule == null ? "导入后将进入自动触发队列" : "导入后将进入自动触发队列，命中规则：" + matchedRule.getRuleCode();
        }
        return "预校验通过";
    }

    private String resolvePreviewInvalidField(Exception e) {
        if (!(e instanceof BizException bizException)) {
            return null;
        }
        String code = trimToNull(bizException.getCode());
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "MARKET_EVENT_EMPTY", "MARKET_EVENT_BATCH_IMPORT_EMPTY" -> "events";
            case "MARKET_EVENT_TARGET_CODE_EMPTY" -> "targetCode";
            case "MARKET_EVENT_TARGET_NAME_EMPTY" -> "targetName";
            case "MARKET_EVENT_TYPE_EMPTY" -> "eventType";
            case "MARKET_EVENT_TITLE_EMPTY" -> "eventTitle";
            case "MARKET_EVENT_SUMMARY_EMPTY" -> "eventSummary";
            case "MARKET_EVENT_IMPACT_EMPTY" -> "impactLevel";
            case "MARKET_EVENT_OCCURRED_AT_EMPTY" -> "occurredAt";
            default -> null;
        };
    }

    private MarketEventDO findDuplicatedEvent(MarketEventCreateDTO dto) {
        String targetCode = normalizeTargetCode(dto.getTargetCode());
        String eventType = normalizeEventType(dto.getEventType());
        String sourceUrl = trimToNull(dto.getSourceUrl());
        String eventTitle = trimToNull(dto.getEventTitle());
        LocalDateTime occurredAt = dto.getOccurredAt();

        LambdaQueryWrapper<MarketEventDO> wrapper = new LambdaQueryWrapper<MarketEventDO>()
                .eq(MarketEventDO::getDeleted, 0)
                .eq(MarketEventDO::getTargetCode, targetCode)
                .orderByDesc(MarketEventDO::getCreatedAt, MarketEventDO::getId);
        if (StringUtils.hasText(sourceUrl)) {
            wrapper.eq(MarketEventDO::getSourceUrl, sourceUrl);
        } else {
            wrapper.eq(MarketEventDO::getOccurredAt, occurredAt);
        }

        return marketEventMapper.selectList(wrapper).stream()
                .filter(item -> isDuplicatedEvent(item, eventType, sourceUrl, eventTitle, occurredAt))
                .findFirst()
                .orElse(null);
    }

    private boolean isDuplicatedEvent(MarketEventDO existing,
                                      String expectedEventType,
                                      String sourceUrl,
                                      String eventTitle,
                                      LocalDateTime occurredAt) {
        if (existing == null || occurredAt == null) {
            return false;
        }
        if (!Objects.equals(normalizeEventType(existing.getEventType()), expectedEventType)) {
            return false;
        }
        if (StringUtils.hasText(sourceUrl)) {
            return sourceUrl.equalsIgnoreCase(defaultIfBlank(existing.getSourceUrl(), ""));
        }
        return eventTitle != null
                && eventTitle.equalsIgnoreCase(defaultIfBlank(existing.getEventTitle(), ""))
                && Objects.equals(existing.getOccurredAt(), occurredAt);
    }

    private MarketEventCreateResultVO buildCreateResult(MarketEventDO event, boolean duplicate, String message) {
        MarketEventCreateResultVO result = new MarketEventCreateResultVO();
        result.setEventId(event == null ? null : event.getEventId());
        result.setDuplicate(duplicate);
        result.setAutoTriggerStatus(event == null ? null : trimToNull(event.getAutoTriggerStatus()));
        result.setAutoTriggerTaskId(event == null ? null : trimToNull(event.getAutoTriggerTaskId()));
        result.setAutoTriggerMessage(event == null ? null : trimToNull(event.getAutoTriggerMessage()));
        result.setMessage(trimToNull(message));
        return result;
    }

    private void appendManualCreateHistory(MarketEventCreateDTO prepared, MarketEventCreateResultVO result) {
        String sourceDetail = String.format(
                "%s / %s",
                defaultIfBlank(prepared == null ? null : prepared.getTargetCode(), "-"),
                defaultIfBlank(prepared == null ? null : prepared.getTargetName(), "-")
        );
        boolean duplicate = result != null && Boolean.TRUE.equals(result.getDuplicate());
        marketEventIngestHistoryService.appendHistory(
                "MANUAL_CREATE",
                "手工录入",
                "MANUAL_CREATE",
                "手工录入",
                "MANUAL",
                prepared == null ? null : prepared.getSourceChannel(),
                sourceDetail,
                1,
                1,
                0,
                duplicate ? 1 : 0,
                result != null && marketEventAutoTriggerService.shouldCountAsQueued(result.getAutoTriggerStatus()) ? 1 : 0,
                duplicate ? "手工录入命中已有事件" : "手工录入成功"
        );
    }

    private void appendBatchIngestHistory(String sourceType,
                                          String sourceLabel,
                                          String sourceCode,
                                          String sourceName,
                                          String sourceCategory,
                                          String sourceChannel,
                                          String sourceDetail,
                                          MarketEventBatchImportResultVO result) {
        if (result == null) {
            return;
        }
        String summary = String.format(
                "%s，共 %d 条，成功 %d 条，失败 %d 条，自动入队 %d 条",
                defaultIfBlank(sourceLabel, "事件接入"),
                defaultIfNull(result.getTotalCount()),
                defaultIfNull(result.getSuccessCount()),
                defaultIfNull(result.getFailedCount()),
                defaultIfNull(result.getAutoTriggeredCount())
        );
        marketEventIngestHistoryService.appendHistory(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailedCount(),
                result.getDuplicateCount(),
                result.getAutoTriggeredCount(),
                summary
        );
    }

    private void appendFailedIngestHistory(String sourceType,
                                          String sourceLabel,
                                          String sourceCode,
                                          String sourceName,
                                          String sourceCategory,
                                          String sourceChannel,
                                          String sourceDetail,
                                          Exception e) {
        String errorMessage = trimMessage(resolveExceptionMessage(e), 255);
        String summary = String.format(
                "%s失败：%s",
                defaultIfBlank(sourceLabel, "事件接入"),
                defaultIfBlank(errorMessage, "未知错误")
        );
        marketEventIngestHistoryService.appendHistory(
                sourceType,
                sourceLabel,
                sourceCode,
                sourceName,
                sourceCategory,
                sourceChannel,
                sourceDetail,
                0,
                0,
                1,
                0,
                0,
                "FAILED",
                errorMessage,
                summary
        );
    }

    private String buildBatchSourceDetail(List<MarketEventCreateDTO> events) {
        if (events == null || events.isEmpty()) {
            return "批量导入";
        }
        MarketEventCreateDTO first = events.get(0);
        String base = String.format(
                "%s / %s",
                defaultIfBlank(first == null ? null : trimToNull(first.getTargetCode()), "-"),
                defaultIfBlank(first == null ? null : trimToNull(first.getTargetName()), "-")
        );
        if (events.size() == 1) {
            return base;
        }
        return base + " 等 " + events.size() + " 条";
    }

    private String resolveBatchSourceChannel(List<MarketEventCreateDTO> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        for (MarketEventCreateDTO item : events) {
            String sourceChannel = normalizeSourceChannel(item == null ? null : item.getSourceChannel(), item == null ? null : item.getEventType());
            if (StringUtils.hasText(sourceChannel)) {
                return sourceChannel;
            }
        }
        return null;
    }

    private String buildMockSourceDetail(MarketEventMockIngestDTO dto) {
        EventSourceConfigItemVO source = eventSourceConfigService.findSource(dto == null ? null : dto.getSourcePreset());
        String presetLabel = source == null ? resolveMockPresetLabel(dto == null ? null : dto.getSourcePreset()) : defaultIfBlank(source.getSourceName(), "模拟接入");
        return String.format(
                "%s / %s / %s",
                presetLabel,
                defaultIfBlank(dto == null ? null : trimToNull(dto.getTargetCode()), "-"),
                defaultIfBlank(dto == null ? null : trimToNull(dto.getTargetName()), "-")
        );
    }

    private String buildSourceSyncDetail(EventSourceConfigItemVO sourceConfig, MarketEventSourceSyncDTO dto) {
        return String.format(
                "%s / %s / %s",
                defaultIfBlank(sourceConfig == null ? null : sourceConfig.getSourceName(), "事件源同步"),
                defaultIfBlank(dto == null ? null : trimToNull(dto.getTargetCode()), "-"),
                defaultIfBlank(dto == null ? null : trimToNull(dto.getTargetName()), "-")
        );
    }

    private String resolveMockPresetLabel(String sourcePreset) {
        if (!StringUtils.hasText(sourcePreset)) {
            return "模拟来源";
        }
        return switch (sourcePreset.trim().toUpperCase(Locale.ROOT)) {
            case "EXCHANGE_ANNOUNCEMENT" -> "交易所公告源";
            case "POLICY_TRACKER" -> "政策跟踪源";
            case "RISK_MONITOR" -> "风险监测源";
            case "NEWS_WIRE" -> "新闻快讯源";
            default -> "模拟来源";
        };
    }

    private String resolveBatchImportMessage(MarketEventCreateResultVO created) {
        if (created == null) {
            return "事件导入成功";
        }
        if (StringUtils.hasText(created.getMessage())) {
            return created.getMessage().trim();
        }
        return "事件导入成功";
    }

    private int defaultIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private Long countMarketEvents(LambdaQueryWrapper<MarketEventDO> wrapper) {
        try {
            return marketEventMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.warn("Failed to count market events, fallback to 0", e);
            return 0L;
        }
    }

    private Long countTrackedMarketEvents() {
        try {
            List<Object> values = researchTaskMapper.selectObjs(
                    new QueryWrapper<ResearchTaskDO>()
                            .select("COUNT(DISTINCT source_event_id)")
                            .eq("deleted", 0)
                            .eq("source_domain", "MARKET_EVENT")
                            .isNotNull("source_event_id")
            );
            if (values == null || values.isEmpty() || values.get(0) == null) {
                return 0L;
            }
            Object value = values.get(0);
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            log.warn("Failed to count tracked market events, fallback to 0", e);
            return 0L;
        }
    }

    private List<MarketEventListItemVO> listMatchedEvents(MarketEventPageQueryDTO queryDTO) {
        List<MarketEventDO> events = marketEventMapper.selectList(
                new LambdaQueryWrapper<MarketEventDO>()
                        .eq(MarketEventDO::getDeleted, 0)
                        .orderByDesc(MarketEventDO::getOccurredAt, MarketEventDO::getCreatedAt, MarketEventDO::getId)
        );
        if (events.isEmpty()) {
            return List.of();
        }

        List<String> eventIds = events.stream()
                .map(MarketEventDO::getEventId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, List<ResearchTaskDO>> followUpTaskMap = loadFollowUpTaskMap(eventIds);
        Map<String, List<MarketEventRelationDO>> relationMap = loadRelationMap(eventIds);
        Map<String, ResearchReportDO> latestReportMap = loadLatestReportMap(
                followUpTaskMap.values().stream().flatMap(List::stream).toList()
        );
        Map<String, RiskWarningDO> latestRiskWarningMap = loadLatestRiskWarningMap(
                followUpTaskMap.values().stream().flatMap(List::stream).toList()
        );
        Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap = loadRiskWarningDetailMap(latestRiskWarningMap.values().stream()
                .map(RiskWarningDO::getWarningId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));

        return events.stream()
                .map(item -> toMarketEventItem(
                        item,
                        relationMap.getOrDefault(item.getEventId(), List.of()),
                        followUpTaskMap.getOrDefault(item.getEventId(), List.of()),
                        latestReportMap,
                        latestRiskWarningMap,
                        riskWarningDetailMap
                ))
                .filter(matchesTargetCode(queryDTO.getTargetCode()))
                .filter(matchesTargetName(queryDTO.getTargetName()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getEventType, queryDTO.getEventType()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getImpactLevel, queryDTO.getImpactLevel()))
                .filter(matchesIgnoreCase(MarketEventListItemVO::getEventStatus, queryDTO.getEventStatus()))
                .toList();
    }

    private Map<String, List<MarketEventRelationDO>> loadRelationMap(List<String> eventIds) {
        List<String> validEventIds = eventIds == null ? List.of() : eventIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validEventIds.isEmpty()) {
            return Map.of();
        }

        return marketEventRelationMapper.selectList(
                new LambdaQueryWrapper<MarketEventRelationDO>()
                        .eq(MarketEventRelationDO::getDeleted, 0)
                        .in(MarketEventRelationDO::getEventId, validEventIds)
                        .orderByAsc(MarketEventRelationDO::getEventId, MarketEventRelationDO::getId)
        ).stream().collect(Collectors.groupingBy(MarketEventRelationDO::getEventId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<String, List<ResearchTaskDO>> loadFollowUpTaskMap(List<String> eventIds) {
        List<String> validEventIds = eventIds == null ? List.of() : eventIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (validEventIds.isEmpty()) {
            return Map.of();
        }

        return researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTaskDO>()
                        .eq(ResearchTaskDO::getDeleted, 0)
                        .eq(ResearchTaskDO::getSourceDomain, "MARKET_EVENT")
                        .in(ResearchTaskDO::getSourceEventId, validEventIds)
                        .orderByDesc(ResearchTaskDO::getCreatedAt, ResearchTaskDO::getId)
        ).stream()
                .filter(item -> StringUtils.hasText(item.getSourceEventId()))
                .collect(Collectors.groupingBy(ResearchTaskDO::getSourceEventId));
    }

    private Map<String, ResearchReportDO> loadLatestReportMap(List<ResearchTaskDO> followUpTasks) {
        List<String> taskIds = followUpTasks == null ? List.of() : followUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReportDO>()
                        .eq(ResearchReportDO::getDeleted, 0)
                        .in(ResearchReportDO::getTaskId, taskIds)
                        .orderByDesc(ResearchReportDO::getCreatedAt, ResearchReportDO::getId)
        ).stream().collect(Collectors.toMap(
                ResearchReportDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<String, RiskWarningDO> loadLatestRiskWarningMap(List<ResearchTaskDO> followUpTasks) {
        List<String> taskIds = followUpTasks == null ? List.of() : followUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        return riskWarningMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDO>()
                        .eq(RiskWarningDO::getDeleted, 0)
                        .in(RiskWarningDO::getTaskId, taskIds)
                        .orderByDesc(RiskWarningDO::getCreatedAt, RiskWarningDO::getId)
        ).stream().collect(Collectors.toMap(
                RiskWarningDO::getTaskId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<String, List<RiskWarningDetailDO>> loadRiskWarningDetailMap(Set<String> warningIds) {
        if (warningIds == null || warningIds.isEmpty()) {
            return Map.of();
        }
        return riskWarningDetailMapper.selectList(
                new LambdaQueryWrapper<RiskWarningDetailDO>()
                        .eq(RiskWarningDetailDO::getDeleted, 0)
                        .in(RiskWarningDetailDO::getWarningId, warningIds)
                        .orderByAsc(RiskWarningDetailDO::getId)
        ).stream().collect(Collectors.groupingBy(RiskWarningDetailDO::getWarningId, LinkedHashMap::new, Collectors.toList()));
    }

    private MarketEventListItemVO toMarketEventItem(MarketEventDO event,
                                                    List<MarketEventRelationDO> relations,
                                                    List<ResearchTaskDO> followUpTasks,
                                                    Map<String, ResearchReportDO> latestReportMap,
                                                    Map<String, RiskWarningDO> latestRiskWarningMap,
                                                    Map<String, List<RiskWarningDetailDO>> riskWarningDetailMap) {
        MarketEventListItemVO vo = new MarketEventListItemVO();
        BeanUtils.copyProperties(event, vo);
        vo.setTargetType(normalizeTargetType(vo.getTargetType()));
        vo.setTargetCode(normalizeTargetCode(vo.getTargetCode()));
        vo.setEventType(normalizeEventType(vo.getEventType()));
        vo.setSourceChannel(normalizeSourceChannel(vo.getSourceChannel(), vo.getEventType()));
        vo.setImpactLevel(normalizeImpactLevel(vo.getImpactLevel()));
        vo.setEventStatus(normalizeEventStatus(vo.getEventStatus()));
        List<MarketEventRelationVO> relationVOs = toRelationVOs(relations);
        vo.setRelations(relationVOs);
        vo.setRelationCount(relationVOs.size());

        List<ResearchTaskDO> safeFollowUpTasks = followUpTasks == null ? List.of() : followUpTasks;
        ResearchTaskDO latestFollowUp = safeFollowUpTasks.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing(ResearchTaskDO::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(ResearchTaskDO::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);

        vo.setFollowUpTaskCount(safeFollowUpTasks.size());
        vo.setFollowUpStatus(resolveFollowUpStatus(latestFollowUp));
        if (latestFollowUp != null) {
            vo.setLatestFollowUpTaskId(latestFollowUp.getTaskId());
            vo.setLatestFollowUpTaskTitle(latestFollowUp.getTaskTitle());
            vo.setLatestFollowUpTaskStatus(latestFollowUp.getStatus());
            vo.setLatestFollowUpCreatedAt(latestFollowUp.getCreatedAt());
        }

        List<ResearchReportDO> relatedReports = safeFollowUpTasks.stream()
                .map(ResearchTaskDO::getTaskId)
                .map(latestReportMap::get)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(ResearchReportDO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ResearchReportDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        vo.setRelatedReportCount(relatedReports.size());
        if (!relatedReports.isEmpty()) {
            ResearchReportDO latestReport = relatedReports.get(0);
            RiskWarningDO latestWarning = latestRiskWarningMap.get(latestReport.getTaskId());
            populateDerivedResultFields(
                    vo,
                    latestReport,
                    latestWarning,
                    latestWarning == null ? List.of() : riskWarningDetailMap.getOrDefault(latestWarning.getWarningId(), List.of())
            );
        }
        return vo;
    }

    private boolean matchesIgnoreCase(List<String> expectedValues, String actualValue) {
        if (expectedValues == null || expectedValues.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(actualValue)) {
            return false;
        }
        return expectedValues.stream().anyMatch(item -> actualValue.equalsIgnoreCase(item));
    }

    private void populateDerivedResultFields(MarketEventListItemVO vo,
                                             ResearchReportDO report,
                                             RiskWarningDO warning,
                                             List<RiskWarningDetailDO> details) {
        if (vo == null || report == null) {
            return;
        }

        String summary = resolveReportSummary(report);
        List<String> warningList = warning == null ? readTextList(report.getRiskWarnings()) : buildDomainRiskWarningMessages(warning);
        int warningCount = warning == null ? warningList.size() : 1;
        int riskPointCount = warning == null
                ? readPreferredTextList(report.getRevisedRiskPoints(), report.getRiskPoints()).size()
                : (details == null ? 0 : details.size());
        int totalRiskCount = warningCount + riskPointCount;
        boolean needHumanReview = warning == null
                ? report.getNeedHumanReview() != null && report.getNeedHumanReview() == 1
                : isDomainRiskHumanReview(warning);
        Double confidenceScore = report.getConfidenceScore() == null ? null : report.getConfidenceScore().doubleValue();
        String reviewStatus = normalizeReviewStatus(report.getReviewStatus());
        String riskLevel = warning == null
                ? (totalRiskCount > 0 || needHumanReview ? resolveRiskLevel(totalRiskCount, needHumanReview) : null)
                : resolveDomainRiskLevel(warning);
        String signalDirection = resolveSignalDirection(summary, totalRiskCount, needHumanReview, confidenceScore);
        int signalScore = calculateSignalScore(confidenceScore, totalRiskCount, needHumanReview, reviewStatus, signalDirection);
        String signalStrength = resolveSignalStrength(signalScore);
        String intelligenceType = resolveMarketIntelligenceType(totalRiskCount, needHumanReview, confidenceScore, signalDirection);

        vo.setLatestReportTaskId(report.getTaskId());
        vo.setLatestReportId(report.getReportId());
        vo.setLatestReportType(trimToNull(report.getReportType()));
        vo.setLatestReportReviewStatus(reviewStatus);
        vo.setLatestReportSummary(summary);
        vo.setLatestReportConfidenceScore(report.getConfidenceScore());
        vo.setLatestNeedHumanReview(needHumanReview);
        vo.setLatestReportCreatedAt(report.getCreatedAt());
        vo.setDerivedRiskLevel(riskLevel);
        vo.setDerivedWarningCount(warningCount);
        vo.setDerivedRiskPointCount(riskPointCount);
        vo.setDerivedRiskCount(totalRiskCount);
        vo.setDerivedSignalDirection(signalDirection);
        vo.setDerivedSignalStrength(signalStrength);
        vo.setDerivedSignalScore(signalScore);
        vo.setDerivedIntelligenceType(intelligenceType);
    }

    private List<String> buildDomainRiskWarningMessages(RiskWarningDO warning) {
        LinkedHashSet<String> messages = new LinkedHashSet<>();
        if (warning == null) {
            return List.of();
        }
        String summary = trimToNull(warning.getWarningSummary());
        if (summary != null) {
            messages.add(summary);
        }
        String reason = trimToNull(warning.getWarningReason());
        if (reason != null) {
            for (String item : reason.split("\\R")) {
                if (item != null && !item.isBlank()) {
                    messages.add(item.trim());
                }
            }
        }
        return List.copyOf(messages);
    }

    private boolean isDomainRiskHumanReview(RiskWarningDO warning) {
        if (warning == null) {
            return false;
        }
        if ("NEED_HUMAN_REVIEW".equalsIgnoreCase(trimToNull(warning.getSuggestAction()))) {
            return true;
        }
        return "HIGH".equalsIgnoreCase(trimToNull(warning.getWarningLevel()))
                && "PENDING".equalsIgnoreCase(trimToNull(warning.getReviewStatus()));
    }

    private String resolveDomainRiskLevel(RiskWarningDO warning) {
        String warningLevel = trimToNull(warning == null ? null : warning.getWarningLevel());
        if ("HIGH".equalsIgnoreCase(warningLevel)) {
            return "HIGH";
        }
        if ("MEDIUM".equalsIgnoreCase(warningLevel)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveFollowUpStatus(ResearchTaskDO latestFollowUp) {
        if (latestFollowUp == null || !StringUtils.hasText(latestFollowUp.getStatus())) {
            return FOLLOW_UP_STATUS_NOT_TRACKED;
        }
        String status = latestFollowUp.getStatus().trim().toUpperCase();
        if ("SUCCESS".equals(status)) {
            return FOLLOW_UP_STATUS_COMPLETED;
        }
        if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
            return FOLLOW_UP_STATUS_FAILED;
        }
        return FOLLOW_UP_STATUS_TRACKING;
    }

    private String resolveReportSummary(ResearchReportDO report) {
        if (report == null) {
            return null;
        }
        if (StringUtils.hasText(report.getRevisedSummary())) {
            return report.getRevisedSummary().trim();
        }
        if (StringUtils.hasText(report.getSummary())) {
            return report.getSummary().trim();
        }
        return null;
    }

    private List<String> readTextList(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {})
                    .stream()
                    .filter(item -> item != null && !item.isBlank())
                    .map(String::trim)
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> readPreferredTextList(String preferredJson, String fallbackJson) {
        List<String> preferred = readTextList(preferredJson);
        return preferred.isEmpty() ? readTextList(fallbackJson) : preferred;
    }

    private String normalizeReviewStatus(String reviewStatus) {
        if ("APPROVED".equalsIgnoreCase(reviewStatus)) {
            return "APPROVED";
        }
        if ("REJECTED".equalsIgnoreCase(reviewStatus)) {
            return "REJECTED";
        }
        return "PENDING";
    }

    private String resolveRiskLevel(int totalRiskCount, boolean needHumanReview) {
        if (needHumanReview || totalRiskCount >= 4) {
            return "HIGH";
        }
        if (totalRiskCount >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveSignalDirection(String summary,
                                          int totalRiskCount,
                                          boolean needHumanReview,
                                          Double confidenceScore) {
        String normalizedSummary = summary == null ? "" : summary.toLowerCase();
        int positiveHit = countKeywords(normalizedSummary, List.of("增长", "改善", "受益", "修复", "上行", "提振", "利好", "突破"));
        int negativeHit = countKeywords(normalizedSummary, List.of("下滑", "承压", "不及预期", "减值", "下行", "风险", "波动", "利空"))
                + totalRiskCount
                + (needHumanReview ? 1 : 0);

        if (negativeHit >= positiveHit + 2) {
            return "NEGATIVE";
        }
        if (positiveHit >= negativeHit + 2 && !needHumanReview && totalRiskCount <= 1 && isHighConfidence(confidenceScore)) {
            return "POSITIVE";
        }
        if (needHumanReview || totalRiskCount >= 3) {
            return "NEGATIVE";
        }
        if (isHighConfidence(confidenceScore) && totalRiskCount == 0) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private int calculateSignalScore(Double confidenceScore,
                                     int totalRiskCount,
                                     boolean needHumanReview,
                                     String reviewStatus,
                                     String signalDirection) {
        int score = confidenceScore == null ? 60 : (int) Math.round(Math.max(0D, Math.min(1D, confidenceScore)) * 100D);
        score -= totalRiskCount * 8;
        if (needHumanReview) {
            score -= 12;
        }
        if ("REJECTED".equalsIgnoreCase(reviewStatus)) {
            score -= 10;
        }
        if ("POSITIVE".equalsIgnoreCase(signalDirection)) {
            score += 5;
        }
        if ("NEGATIVE".equalsIgnoreCase(signalDirection)) {
            score -= 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String resolveSignalStrength(int signalScore) {
        if (signalScore >= 80) {
            return "STRONG";
        }
        if (signalScore >= 60) {
            return "MEDIUM";
        }
        return "WEAK";
    }

    private String resolveMarketIntelligenceType(int totalRiskCount,
                                                 boolean needHumanReview,
                                                 Double confidenceScore,
                                                 String signalDirection) {
        if (needHumanReview || totalRiskCount > 0) {
            return "RISK_ALERT";
        }
        if ("POSITIVE".equalsIgnoreCase(signalDirection)
                || "NEGATIVE".equalsIgnoreCase(signalDirection)
                || isHighConfidence(confidenceScore)) {
            return "STRATEGY_SIGNAL";
        }
        return "REPORT_INSIGHT";
    }

    private boolean isHighConfidence(Double confidenceScore) {
        return confidenceScore != null && confidenceScore >= 0.8D;
    }

    private int countKeywords(String content, List<String> keywords) {
        if (!StringUtils.hasText(content) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && content.contains(keyword.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private Predicate<MarketEventListItemVO> matchesTargetCode(String targetCode) {
        if (!StringUtils.hasText(targetCode)) {
            return item -> true;
        }
        String keyword = targetCode.trim().toUpperCase();
        return item -> item.getTargetCode() != null && item.getTargetCode().toUpperCase().contains(keyword)
                || (item.getRelations() != null && item.getRelations().stream()
                .anyMatch(relation -> relation.getRelationCode() != null
                        && relation.getRelationCode().toUpperCase().contains(keyword)));
    }

    private Predicate<MarketEventListItemVO> matchesTargetName(String targetName) {
        if (!StringUtils.hasText(targetName)) {
            return item -> true;
        }
        String keyword = targetName.trim().toUpperCase();
        return item -> item.getTargetName() != null && item.getTargetName().toUpperCase().contains(keyword);
    }

    private Predicate<MarketEventListItemVO> matchesIgnoreCase(
            java.util.function.Function<MarketEventListItemVO, String> extractor,
            String expected
    ) {
        if (!StringUtils.hasText(expected)) {
            return item -> true;
        }
        String normalized = expected.trim().toUpperCase();
        return item -> {
            String value = extractor.apply(item);
            return value != null && normalized.equals(value.trim().toUpperCase());
        };
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String trimMessage(String message, int maxLength) {
        String normalized = trimToNull(message);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String resolveExceptionMessage(Exception e) {
        if (e instanceof BizException bizException && StringUtils.hasText(bizException.getMessage())) {
            return bizException.getMessage();
        }
        return e == null ? "批量导入失败" : defaultIfBlank(e.getMessage(), "批量导入失败");
    }

    private String normalizeTargetType(String value) {
        return normalizeByAliases(value, TARGET_TYPE_ALIASES, "STOCK");
    }

    private String normalizeRelationType(String value) {
        return normalizeTargetType(value);
    }

    private List<MarketEventRelationVO> toRelationVOs(List<MarketEventRelationDO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        return relations.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    MarketEventRelationVO vo = new MarketEventRelationVO();
                    vo.setRelationType(normalizeRelationType(item.getRelationType()));
                    vo.setRelationCode(normalizeTargetCode(item.getRelationCode()));
                    vo.setRelationName(trimToNull(item.getRelationName()));
                    vo.setRelationWeight(item.getRelationWeight());
                    return vo;
                })
                .toList();
    }

    private String normalizeTargetCode(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeEventType(String value) {
        return normalizeByAliases(value, EVENT_TYPE_ALIASES, "OTHER");
    }

    private String normalizeImpactLevel(String value) {
        return normalizeByAliases(value, IMPACT_LEVEL_ALIASES, "MEDIUM");
    }

    private String normalizeEventStatus(String value) {
        return normalizeByAliases(value, EVENT_STATUS_ALIASES, "ACTIVE");
    }

    private String normalizeSourceChannel(String value, String eventType) {
        String normalized = normalizeByAliases(value, SOURCE_CHANNEL_ALIASES, null);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (!StringUtils.hasText(value)) {
            return "MANUAL_ENTRY";
        }
        String aliasKey = normalizeAliasKey(value);
        if (StringUtils.hasText(aliasKey) && aliasKey.chars().allMatch(ch -> ch == '_' || Character.isLetterOrDigit(ch))) {
            return aliasKey;
        }
        if ("ANNOUNCEMENT".equalsIgnoreCase(eventType)) {
            return "ANNOUNCEMENT_FEED";
        }
        if ("EARNINGS".equalsIgnoreCase(eventType)) {
            return "EARNINGS_FEED";
        }
        if ("POLICY".equalsIgnoreCase(eventType)) {
            return "POLICY_FEED";
        }
        if ("RISK_ALERT".equalsIgnoreCase(eventType)) {
            return "RISK_MONITOR";
        }
        return "NEWS_FEED";
    }

    private String normalizeByAliases(String value, Map<String, String> aliases, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String aliasKey = normalizeAliasKey(value);
        String normalized = aliasKey == null ? null : aliases.get(aliasKey);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String normalizeAliasKey(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim()
                .replace('　', ' ')
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> buildTargetTypeAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "STOCK", "STOCK", "EQUITY", "股票", "证券");
        return aliases;
    }

    private static Map<String, String> buildEventTypeAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "NEWS", "NEWS", "新闻", "资讯", "NEWS_EVENT");
        registerAlias(aliases, "ANNOUNCEMENT", "ANNOUNCEMENT", "公告", "披露", "NOTICE", "DISCLOSURE");
        registerAlias(aliases, "EARNINGS", "EARNINGS", "EARNING", "财报", "业绩", "年报", "季报", "中报", "快报", "FINANCIAL_REPORT");
        registerAlias(aliases, "POLICY", "POLICY", "政策", "监管", "REGULATION");
        registerAlias(aliases, "RISK_ALERT", "RISK_ALERT", "RISK", "风险", "风险预警");
        registerAlias(aliases, "OTHER", "OTHER", "其他", "MISC");
        return aliases;
    }

    private static Map<String, String> buildImpactLevelAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "HIGH", "HIGH", "H", "高", "高影响", "高等级", "P1", "1");
        registerAlias(aliases, "MEDIUM", "MEDIUM", "M", "中", "中影响", "一般", "P2", "2");
        registerAlias(aliases, "LOW", "LOW", "L", "低", "低影响", "低等级", "P3", "3");
        return aliases;
    }

    private static Map<String, String> buildEventStatusAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "ACTIVE", "ACTIVE", "OPEN", "进行中", "待处理");
        registerAlias(aliases, "RESOLVED", "RESOLVED", "CLOSED", "已解决", "已关闭", "已完成");
        registerAlias(aliases, "IGNORED", "IGNORED", "SKIPPED", "已忽略", "忽略");
        return aliases;
    }

    private static Map<String, String> buildSourceChannelAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<>();
        registerAlias(aliases, "MANUAL_ENTRY", "MANUAL_ENTRY", "MANUAL", "手工录入", "人工录入");
        registerAlias(aliases, "MANUAL_IMPORT", "MANUAL_IMPORT", "IMPORT", "批量导入", "手工导入");
        registerAlias(aliases, "NEWS_FEED", "NEWS_FEED", "NEWS", "新闻", "新闻源");
        registerAlias(aliases, "ANNOUNCEMENT_FEED", "ANNOUNCEMENT_FEED", "ANNOUNCEMENT", "公告", "公告源");
        registerAlias(aliases, "EARNINGS_FEED", "EARNINGS_FEED", "EARNINGS", "财报", "业绩", "财报源");
        registerAlias(aliases, "POLICY_FEED", "POLICY_FEED", "POLICY", "政策", "政策源");
        registerAlias(aliases, "RISK_MONITOR", "RISK_MONITOR", "RISK_ALERT", "风险预警", "风险监测");
        registerAlias(aliases, "THIRD_PARTY_FEED", "THIRD_PARTY_FEED", "FEED", "THIRD_PARTY", "第三方", "外部源");
        return aliases;
    }

    private static void registerAlias(Map<String, String> aliases, String canonical, String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                aliases.put(value.trim().replace('　', ' ').replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT), canonical);
            }
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }
}
