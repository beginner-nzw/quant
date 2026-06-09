package com.quant.market;

import com.quant.aiorchestrator.domain.dto.MarketEventBatchImportDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventMockIngestDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventRelationDTO;
import com.quant.aiorchestrator.domain.dto.MarketEventSourceSyncDTO;
import com.quant.aiorchestrator.domain.dto.MarketIntelligencePageQueryDTO;
import com.quant.aiorchestrator.consumer.MarketEventStandardizedConsumer;
import com.quant.aiorchestrator.domain.entity.MarketEventAnalysisDO;
import com.quant.aiorchestrator.domain.entity.MarketEventAutoTriggerAttemptDO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.projection.MarketEventFollowUpProjection;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchImportResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventBatchPreviewResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketEventPageVO;
import com.quant.aiorchestrator.domain.vo.MarketEventRelationVO;
import com.quant.aiorchestrator.domain.vo.MarketEventStatsVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceListItemVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligencePageVO;
import com.quant.aiorchestrator.domain.vo.MarketIntelligenceStatsVO;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerAttemptManager;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerEventLoaderManager;
import com.quant.aiorchestrator.manager.MarketEventAutoTriggerOrchestrationManager;
import com.quant.aiorchestrator.manager.MarketEventCommandManager;
import com.quant.aiorchestrator.manager.MarketEventCreateManager;
import com.quant.aiorchestrator.manager.MarketEventListItemAssembler;
import com.quant.aiorchestrator.manager.MarketEventNormalizationManager;
import com.quant.aiorchestrator.manager.MarketEventNormalizationRuleManager;
import com.quant.aiorchestrator.manager.MarketEventProjectionManager;
import com.quant.aiorchestrator.manager.MarketEventQueryManager;
import com.quant.aiorchestrator.manager.MarketEventReadManager;
import com.quant.aiorchestrator.manager.MarketEventStandardizedMessageManager;
import com.quant.aiorchestrator.manager.MarketEventStatsManager;
import com.quant.aiorchestrator.manager.MarketEventWriteManager;
import com.quant.aiorchestrator.manager.MarketIntelligenceItemAssembler;
import com.quant.aiorchestrator.manager.MarketIntelligenceProjectionManager;
import com.quant.aiorchestrator.manager.MarketIntelligenceReadManager;
import com.quant.aiorchestrator.mapper.MarketEventAnalysisMapper;
import com.quant.aiorchestrator.mapper.MarketEventAutoTriggerAttemptMapper;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.EventAutoTaskDispatchService;
import com.quant.aiorchestrator.service.MarketEventFollowUpProjectionProvider;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryAppender;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.aiorchestrator.service.MarketEventTrackingStatsProvider;
import com.quant.aiorchestrator.service.MarketIntelligenceQueryService;
import com.quant.aiorchestrator.service.NoopMarketEventFollowUpProjectionProvider;
import com.quant.aiorchestrator.service.ResearchTaskMarketEventFollowUpProjectionProvider;
import com.quant.aiorchestrator.service.ResearchTaskMarketEventTrackingStatsProvider;
import com.quant.aiorchestrator.service.impl.MarketEventAutoTriggerServiceImpl;
import com.quant.aiorchestrator.service.impl.MarketEventStandardizedPublisherServiceImpl;
import com.quant.aiorchestrator.service.impl.MarketQueryServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketEventServiceBoundaryTests {

    @Test
    void marketEventServiceOwnsMarketEventPersistenceTypes() {
        assertEquals("com.quant.aiorchestrator.domain.entity", MarketEventDO.class.getPackageName());
        assertEquals(MarketEventDO.class.getPackageName(), MarketEventRelationDO.class.getPackageName());
        assertEquals(MarketEventDO.class.getPackageName(), MarketEventAnalysisDO.class.getPackageName());
        assertEquals(MarketEventDO.class.getPackageName(), MarketEventAutoTriggerAttemptDO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.mapper", MarketEventMapper.class.getPackageName());
        assertEquals(MarketEventMapper.class.getPackageName(), MarketEventRelationMapper.class.getPackageName());
        assertEquals(MarketEventMapper.class.getPackageName(), MarketEventAnalysisMapper.class.getPackageName());
        assertEquals(MarketEventMapper.class.getPackageName(), MarketEventAutoTriggerAttemptMapper.class.getPackageName());
    }

    @Test
    void marketEventServiceOwnsCreateContractsAndNormalizationRuntime() {
        assertEquals("com.quant.aiorchestrator.domain.dto", MarketEventCreateDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketEventRelationDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketEventBatchImportDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketEventMockIngestDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketEventPageQueryDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketEventSourceSyncDTO.class.getPackageName());
        assertEquals(MarketEventCreateDTO.class.getPackageName(), MarketIntelligencePageQueryDTO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.vo", MarketEventRelationVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventBatchImportItemVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventBatchImportResultVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventBatchPreviewItemVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventBatchPreviewResultVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventCreateResultVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventListItemVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventPageVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketEventStatsVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketIntelligenceListItemVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketIntelligencePageVO.class.getPackageName());
        assertEquals(MarketEventRelationVO.class.getPackageName(), MarketIntelligenceStatsVO.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.manager", MarketEventNormalizationManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventNormalizationRuleManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventCreateManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventWriteManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventStandardizedMessageManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventStatsManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventReadManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventProjectionManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventQueryManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventListItemAssembler.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventCommandManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventAutoTriggerEventLoaderManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventAutoTriggerAttemptManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketEventAutoTriggerOrchestrationManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketIntelligenceProjectionManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketIntelligenceReadManager.class.getPackageName());
        assertEquals(MarketEventNormalizationManager.class.getPackageName(), MarketIntelligenceItemAssembler.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.domain.projection", MarketEventFollowUpProjection.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", MarketEventFollowUpProjectionProvider.class.getPackageName());
        assertEquals(MarketEventFollowUpProjectionProvider.class.getPackageName(), NoopMarketEventFollowUpProjectionProvider.class.getPackageName());
        assertEquals(MarketEventFollowUpProjectionProvider.class.getPackageName(), ResearchTaskMarketEventFollowUpProjectionProvider.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service", MarketEventTrackingStatsProvider.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), ResearchTaskMarketEventTrackingStatsProvider.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), MarketIntelligenceQueryService.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), MarketEventAutoTriggerService.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), EventAutoTaskDispatchService.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), MarketEventStandardizedPublisherService.class.getPackageName());
        assertEquals(MarketEventTrackingStatsProvider.class.getPackageName(), MarketEventIngestHistoryAppender.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.consumer", MarketEventStandardizedConsumer.class.getPackageName());
        assertEquals("com.quant.aiorchestrator.service.impl", MarketEventAutoTriggerServiceImpl.class.getPackageName());
        assertEquals(MarketEventAutoTriggerServiceImpl.class.getPackageName(), MarketEventStandardizedPublisherServiceImpl.class.getPackageName());
        assertEquals(MarketEventAutoTriggerServiceImpl.class.getPackageName(), MarketQueryServiceImpl.class.getPackageName());
    }
}
