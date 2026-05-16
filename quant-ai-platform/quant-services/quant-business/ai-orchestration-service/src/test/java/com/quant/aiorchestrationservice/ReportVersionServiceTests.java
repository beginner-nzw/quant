package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.mapper.ReportEvidenceRefMapper;
import com.quant.aiorchestrator.mapper.ResearchReportSectionMapper;
import com.quant.aiorchestrator.mapper.ResearchReportVersionMapper;
import com.quant.aiorchestrator.service.ReportVersionService;
import com.quant.aiorchestrator.service.impl.ReportVersionServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportVersionServiceTests {

    @Test
    void createSnapshotCapturesCurrentReportSectionsAndEvidence() throws Exception {
        TestDeps deps = new TestDeps();
        when(deps.versionMapper.selectOne(any())).thenReturn(null);
        when(deps.sectionMapper.selectList(any())).thenReturn(List.of(buildSection()));
        when(deps.evidenceMapper.selectList(any())).thenReturn(List.of(buildEvidence()));
        ReportVersionService service = newService(deps);

        ResearchReportDO report = buildReport(2);
        service.createSnapshot(report, "AI_RESULT");

        ArgumentCaptor<ResearchReportVersionDO> versionCaptor = ArgumentCaptor.forClass(ResearchReportVersionDO.class);
        verify(deps.versionMapper).insert(versionCaptor.capture());
        ResearchReportVersionDO version = versionCaptor.getValue();
        assertEquals("report-1", version.getReportId());
        assertEquals("task-1", version.getTaskId());
        assertEquals(2, version.getVersionNo());
        assertEquals("AI_RESULT", version.getSnapshotSource());

        Map<?, ?> snapshot = new ObjectMapper().readValue(version.getSnapshotPayload(), Map.class);
        assertEquals(2, snapshot.get("versionNo"));
        Map<?, ?> reportSnapshot = (Map<?, ?>) snapshot.get("report");
        assertEquals("updated summary", reportSnapshot.get("summary"));
        List<?> sections = (List<?>) snapshot.get("sections");
        assertEquals(1, sections.size());
        assertEquals("SUMMARY", ((Map<?, ?>) sections.get(0)).get("sectionCode"));
        List<?> evidenceRefs = (List<?>) snapshot.get("evidenceRefs");
        assertEquals("ref-1", ((Map<?, ?>) evidenceRefs.get(0)).get("sourceRefId"));
    }

    @Test
    void createSnapshotDoesNotDuplicateExistingVersion() {
        TestDeps deps = new TestDeps();
        when(deps.versionMapper.selectOne(any())).thenReturn(new ResearchReportVersionDO());
        ReportVersionService service = newService(deps);

        service.createSnapshot(buildReport(3), "AI_RESULT");

        verify(deps.versionMapper, never()).insert(any(ResearchReportVersionDO.class));
    }

    @Test
    void getVersionReturnsHistoricalSnapshot() {
        TestDeps deps = new TestDeps();
        ResearchReportVersionDO entity = new ResearchReportVersionDO();
        entity.setVersionId("version-1");
        entity.setReportId("report-1");
        entity.setTaskId("task-1");
        entity.setVersionNo(1);
        entity.setSnapshotSource("AI_RESULT");
        entity.setSnapshotPayload("{\"versionNo\":1}");
        when(deps.versionMapper.selectOne(any())).thenReturn(entity);
        ReportVersionService service = newService(deps);

        assertEquals(1, service.getVersion("task-1", 1).getSnapshot().get("versionNo"));
    }

    private static ReportVersionService newService(TestDeps deps) {
        return new ReportVersionServiceImpl(
                deps.versionMapper,
                deps.sectionMapper,
                deps.evidenceMapper,
                new ObjectMapper()
        );
    }

    private static ResearchReportDO buildReport(int versionNo) {
        ResearchReportDO report = new ResearchReportDO();
        report.setReportId("report-1");
        report.setTaskId("task-1");
        report.setVersionNo(versionNo);
        report.setTaskType("RESEARCH");
        report.setFinalStatus("SUCCESS");
        report.setSummary("updated summary");
        report.setConfidenceScore(BigDecimal.valueOf(0.91));
        report.setNeedHumanReview(0);
        report.setHighlights("[\"h1\"]");
        report.setRiskPoints("[\"r1\"]");
        report.setRiskWarnings("[]");
        report.setRawPayload("{\"summary\":\"updated summary\"}");
        return report;
    }

    private static ResearchReportSectionDO buildSection() {
        ResearchReportSectionDO section = new ResearchReportSectionDO();
        section.setSectionId("section-1");
        section.setVersionNo(2);
        section.setSectionCode("SUMMARY");
        section.setSectionTitle("Summary");
        section.setSectionOrder(10);
        section.setSectionContent("updated summary");
        section.setDeleted(0);
        return section;
    }

    private static ReportEvidenceRefDO buildEvidence() {
        ReportEvidenceRefDO evidence = new ReportEvidenceRefDO();
        evidence.setEvidenceId("evidence-1");
        evidence.setSourceType("REPORT_META");
        evidence.setSourceRefId("ref-1");
        evidence.setEvidenceSummary("evidence summary");
        evidence.setDeleted(0);
        return evidence;
    }

    private static final class TestDeps {
        private final ResearchReportVersionMapper versionMapper = mock(ResearchReportVersionMapper.class);
        private final ResearchReportSectionMapper sectionMapper = mock(ResearchReportSectionMapper.class);
        private final ReportEvidenceRefMapper evidenceMapper = mock(ReportEvidenceRefMapper.class);
    }
}
