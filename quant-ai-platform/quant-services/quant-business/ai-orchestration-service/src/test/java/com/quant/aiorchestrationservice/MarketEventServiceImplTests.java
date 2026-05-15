package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.dto.MarketEventCreateDTO;
import com.quant.aiorchestrator.domain.entity.MarketEventDO;
import com.quant.aiorchestrator.domain.entity.MarketEventRelationDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchTaskDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDO;
import com.quant.aiorchestrator.domain.entity.RiskWarningDetailDO;
import com.quant.aiorchestrator.domain.vo.MarketEventCreateResultVO;
import com.quant.aiorchestrator.domain.vo.MarketEventListItemVO;
import com.quant.aiorchestrator.domain.dto.MarketEventPageQueryDTO;
import com.quant.aiorchestrator.domain.vo.EventSourceConfigItemVO;
import com.quant.aiorchestrator.mapper.MarketEventMapper;
import com.quant.aiorchestrator.mapper.MarketEventRelationMapper;
import com.quant.aiorchestrator.mapper.ResearchReportMapper;
import com.quant.aiorchestrator.mapper.ResearchTaskMapper;
import com.quant.aiorchestrator.mapper.RiskWarningDetailMapper;
import com.quant.aiorchestrator.mapper.RiskWarningMapper;
import com.quant.aiorchestrator.service.CninfoProxyAnnouncementService;
import com.quant.aiorchestrator.service.EventAutoTriggerConfigService;
import com.quant.aiorchestrator.service.EventSourceConfigService;
import com.quant.aiorchestrator.service.EventSourceSyncAdapter;
import com.quant.aiorchestrator.service.MarketEventAutoTriggerService;
import com.quant.aiorchestrator.service.MarketEventIngestHistoryService;
import com.quant.aiorchestrator.service.MarketEventMockIngestGenerator;
import com.quant.aiorchestrator.service.MarketEventStandardizedPublisherService;
import com.quant.aiorchestrator.service.impl.MarketEventServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketEventServiceImplTests {

    @Test
    void createMarketEventPublishesStandardizedEventAfterPersistingAutoTriggerState() {
        MarketEventMapper marketEventMapper = mock(MarketEventMapper.class);
        MarketEventRelationMapper marketEventRelationMapper = mock(MarketEventRelationMapper.class);
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        EventAutoTriggerConfigService eventAutoTriggerConfigService = mock(EventAutoTriggerConfigService.class);
        MarketEventAutoTriggerService marketEventAutoTriggerService = mock(MarketEventAutoTriggerService.class);
        MarketEventMockIngestGenerator marketEventMockIngestGenerator = mock(MarketEventMockIngestGenerator.class);
        MarketEventIngestHistoryService marketEventIngestHistoryService = mock(MarketEventIngestHistoryService.class);
        EventSourceConfigService eventSourceConfigService = mock(EventSourceConfigService.class);
        CninfoProxyAnnouncementService cninfoProxyAnnouncementService = mock(CninfoProxyAnnouncementService.class);
        MarketEventStandardizedPublisherService marketEventStandardizedPublisherService = mock(MarketEventStandardizedPublisherService.class);

        MarketEventServiceImpl service = new MarketEventServiceImpl(
                marketEventMapper,
                marketEventRelationMapper,
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                objectMapper,
                eventAutoTriggerConfigService,
                marketEventAutoTriggerService,
                marketEventMockIngestGenerator,
                marketEventIngestHistoryService,
                eventSourceConfigService,
                List.<EventSourceSyncAdapter>of(),
                cninfoProxyAnnouncementService,
                marketEventStandardizedPublisherService
        );

        when(marketEventMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            MarketEventDO entity = invocation.getArgument(0);
            entity.setAutoTriggerStatus("DISABLED");
            entity.setAutoTriggerMessage("事件自动触发已关闭");
            return entity;
        }).when(marketEventAutoTriggerService).prepareAutoTrigger(any(MarketEventDO.class));
        doAnswer(invocation -> {
            MarketEventDO entity = invocation.getArgument(0);
            entity.setId(1L);
            entity.setCreatedAt(LocalDateTime.of(2026, 5, 7, 14, 0));
            return 1;
        }).when(marketEventMapper).insert(any(MarketEventDO.class));
        doAnswer(invocation -> {
            MarketEventDO update = invocation.getArgument(0);
            return 1;
        }).when(marketEventMapper).updateById(any(MarketEventDO.class));
        doNothing().when(marketEventIngestHistoryService).appendHistory(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

        MarketEventCreateDTO dto = new MarketEventCreateDTO();
        dto.setTargetCode("600519");
        dto.setTargetName("贵州茅台");
        dto.setEventType("ANNOUNCEMENT");
        dto.setEventTitle("年报发布");
        dto.setEventSummary("公司发布年度报告");
        dto.setImpactLevel("HIGH");
        dto.setOccurredAt(LocalDateTime.of(2026, 5, 7, 13, 55));

        MarketEventCreateResultVO result = service.createMarketEvent(dto);

        assertFalse(Boolean.TRUE.equals(result.getDuplicate()));
        assertEquals("DISABLED", result.getAutoTriggerStatus());
        assertEquals("事件自动触发已关闭", result.getAutoTriggerMessage());

        ArgumentCaptor<MarketEventDO> publisherCaptor = ArgumentCaptor.forClass(MarketEventDO.class);
        verify(marketEventStandardizedPublisherService).publish(publisherCaptor.capture());
        assertEquals("DISABLED", publisherCaptor.getValue().getAutoTriggerStatus());
        assertEquals("600519", publisherCaptor.getValue().getTargetCode());
        assertNotNull(publisherCaptor.getValue().getCreatedAt());

        ArgumentCaptor<MarketEventRelationDO> relationCaptor = ArgumentCaptor.forClass(MarketEventRelationDO.class);
        verify(marketEventRelationMapper).insert(relationCaptor.capture());
        assertEquals(publisherCaptor.getValue().getEventId(), relationCaptor.getValue().getEventId());
        assertEquals("STOCK", relationCaptor.getValue().getRelationType());
        assertEquals("600519", relationCaptor.getValue().getRelationCode());
    }

    @Test
    void pageMarketEventsPrefersDomainRiskProjectionForDerivedFields() {
        MarketEventMapper marketEventMapper = mock(MarketEventMapper.class);
        MarketEventRelationMapper marketEventRelationMapper = mock(MarketEventRelationMapper.class);
        ResearchTaskMapper researchTaskMapper = mock(ResearchTaskMapper.class);
        ResearchReportMapper researchReportMapper = mock(ResearchReportMapper.class);
        RiskWarningMapper riskWarningMapper = mock(RiskWarningMapper.class);
        RiskWarningDetailMapper riskWarningDetailMapper = mock(RiskWarningDetailMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        EventAutoTriggerConfigService eventAutoTriggerConfigService = mock(EventAutoTriggerConfigService.class);
        MarketEventAutoTriggerService marketEventAutoTriggerService = mock(MarketEventAutoTriggerService.class);
        MarketEventMockIngestGenerator marketEventMockIngestGenerator = mock(MarketEventMockIngestGenerator.class);
        MarketEventIngestHistoryService marketEventIngestHistoryService = mock(MarketEventIngestHistoryService.class);
        EventSourceConfigService eventSourceConfigService = mock(EventSourceConfigService.class);
        CninfoProxyAnnouncementService cninfoProxyAnnouncementService = mock(CninfoProxyAnnouncementService.class);
        MarketEventStandardizedPublisherService marketEventStandardizedPublisherService = mock(MarketEventStandardizedPublisherService.class);

        MarketEventServiceImpl service = new MarketEventServiceImpl(
                marketEventMapper,
                marketEventRelationMapper,
                researchTaskMapper,
                researchReportMapper,
                riskWarningMapper,
                riskWarningDetailMapper,
                objectMapper,
                eventAutoTriggerConfigService,
                marketEventAutoTriggerService,
                marketEventMockIngestGenerator,
                marketEventIngestHistoryService,
                eventSourceConfigService,
                List.<EventSourceSyncAdapter>of(),
                cninfoProxyAnnouncementService,
                marketEventStandardizedPublisherService
        );

        MarketEventDO event = new MarketEventDO();
        event.setEventId("event-1");
        event.setTargetCode("600519");
        event.setTargetName("贵州茅台");
        event.setTargetType("STOCK");
        event.setEventType("ANNOUNCEMENT");
        event.setImpactLevel("HIGH");
        event.setEventStatus("ACTIVE");
        event.setOccurredAt(LocalDateTime.of(2026, 5, 7, 13, 0));
        when(marketEventMapper.selectList(any())).thenReturn(List.of(event));
        when(marketEventRelationMapper.selectList(any())).thenReturn(List.of());

        ResearchTaskDO followUpTask = new ResearchTaskDO();
        followUpTask.setId(1L);
        followUpTask.setTaskId("task-1");
        followUpTask.setTaskTitle("跟踪任务");
        followUpTask.setStatus("SUCCESS");
        followUpTask.setSourceEventId("event-1");
        followUpTask.setCreatedAt(LocalDateTime.of(2026, 5, 7, 13, 5));
        when(researchTaskMapper.selectList(any())).thenReturn(List.of(followUpTask));

        ResearchReportDO report = new ResearchReportDO();
        report.setId(1L);
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setReportType("EVENT_TRACKING");
        report.setSummary("事件后续跟踪");
        report.setRiskWarnings("[]");
        report.setRiskPoints("[]");
        report.setNeedHumanReview(0);
        report.setReviewStatus("APPROVED");
        report.setCreatedAt(LocalDateTime.of(2026, 5, 7, 13, 10));
        when(researchReportMapper.selectList(any())).thenReturn(List.of(report));

        RiskWarningDO warning = new RiskWarningDO();
        warning.setWarningId("warning-1");
        warning.setTaskId("task-1");
        warning.setWarningLevel("HIGH");
        warning.setWarningSummary("高风险预警");
        warning.setWarningReason("集中偿付压力");
        warning.setSuggestAction("NEED_HUMAN_REVIEW");
        warning.setReviewStatus("PENDING");
        warning.setCreatedAt(LocalDateTime.of(2026, 5, 7, 13, 11));
        when(riskWarningMapper.selectList(any())).thenReturn(List.of(warning));

        RiskWarningDetailDO detail = new RiskWarningDetailDO();
        detail.setWarningId("warning-1");
        detail.setDetailDesc("未来三个月偿付压力较高");
        when(riskWarningDetailMapper.selectList(any())).thenReturn(List.of(detail));

        MarketEventPageQueryDTO queryDTO = new MarketEventPageQueryDTO();
        MarketEventListItemVO result = service.pageMarketEvents(queryDTO).getRecords().get(0);

        assertEquals("HIGH", result.getDerivedRiskLevel());
        assertEquals(2, result.getDerivedRiskCount());
        assertEquals(1, result.getDerivedWarningCount());
        assertEquals(1, result.getDerivedRiskPointCount());
        assertEquals(Boolean.TRUE, result.getLatestNeedHumanReview());
    }
}
