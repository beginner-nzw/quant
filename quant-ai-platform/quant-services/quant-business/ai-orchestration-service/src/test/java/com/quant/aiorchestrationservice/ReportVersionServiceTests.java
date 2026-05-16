package com.quant.aiorchestrationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.aiorchestrator.domain.entity.ReportEvidenceRefDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportSectionDO;
import com.quant.aiorchestrator.domain.entity.ResearchReportVersionDO;
import com.quant.aiorchestrator.domain.vo.ReportVersionCompareVO;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        ResearchReportVersionDO entity = buildVersion(1, snapshotJson("summary v1", List.of("h1"), List.of("ref-1"), "PENDING"));
        when(deps.versionMapper.selectOne(any())).thenReturn(entity);
        ReportVersionService service = newService(deps);

        assertEquals(1, service.getVersion("task-1", 1).getSnapshot().get("versionNo"));
    }

    @Test
    void compareVersionsReturnsDeterministicFieldDiff() {
        TestDeps deps = new TestDeps();
        when(deps.versionMapper.selectOne(any()))
                .thenReturn(
                        buildVersion(1, snapshotJson("summary v1", List.of("h1"), List.of("ref-1"), "PENDING")),
                        buildVersion(2, snapshotJson("summary v2", List.of("h1", "h2"), List.of("ref-2"), "APPROVED"))
                );
        ReportVersionService service = newService(deps);

        ReportVersionCompareVO compare = service.compareVersions("task-1", 1, 2);

        assertFalse(compare.getSameVersion());
        assertTrue(compare.getChanged());
        assertEquals(1, compare.getReportFieldsChanged().size());
        assertEquals("report.summary", compare.getReportFieldsChanged().get(0).getPath());
        assertEquals(1, compare.getSectionsChanged().size());
        assertEquals("sections[HIGHLIGHTS].sectionItems", compare.getSectionsChanged().get(0).getPath());
        assertEquals(1, compare.getEvidenceRefsAdded().size());
        assertEquals(1, compare.getEvidenceRefsRemoved().size());
        assertEquals(1, compare.getReviewFieldsChanged().size());
        assertEquals("report.review.reviewStatus", compare.getReviewFieldsChanged().get(0).getPath());
    }

    @Test
    void compareSameVersionReturnsNoOpDiff() {
        TestDeps deps = new TestDeps();
        ResearchReportVersionDO version = buildVersion(1, snapshotJson("summary v1", List.of("h1"), List.of("ref-1"), "PENDING"));
        when(deps.versionMapper.selectOne(any())).thenReturn(version).thenReturn(version);
        ReportVersionService service = newService(deps);

        ReportVersionCompareVO compare = service.compareVersions("task-1", 1, 1);

        assertTrue(compare.getSameVersion());
        assertFalse(compare.getChanged());
        assertTrue(compare.getReportFieldsChanged().isEmpty());
        assertTrue(compare.getSectionsChanged().isEmpty());
        assertTrue(compare.getEvidenceRefsAdded().isEmpty());
        assertTrue(compare.getEvidenceRefsRemoved().isEmpty());
        assertTrue(compare.getReviewFieldsChanged().isEmpty());
    }

    @Test
    void compareVersionsReturnsNullForMissingOrWrongTaskVersion() {
        TestDeps deps = new TestDeps();
        when(deps.versionMapper.selectOne(any()))
                .thenReturn(buildVersion(1, snapshotJson("summary v1", List.of("h1"), List.of("ref-1"), "PENDING")))
                .thenReturn(null);
        ReportVersionService service = newService(deps);

        assertNull(service.compareVersions("task-1", 1, 9));
        assertNull(service.compareVersions("task-1", null, 2));
    }

    @Test
    void listVersionsKeepsExistingSummaryReadShape() {
        TestDeps deps = new TestDeps();
        when(deps.versionMapper.selectList(any()))
                .thenReturn(List.of(buildVersion(2, snapshotJson("summary v2", List.of("h2"), List.of("ref-2"), "APPROVED"))));
        ReportVersionService service = newService(deps);

        assertEquals(1, service.listVersions("task-1").size());
        assertEquals(2, service.listVersions("task-1").get(0).getVersionNo());
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

    private static ResearchReportVersionDO buildVersion(int versionNo, String snapshotPayload) {
        ResearchReportVersionDO entity = new ResearchReportVersionDO();
        entity.setVersionId("version-" + versionNo);
        entity.setReportId("report-1");
        entity.setTaskId("task-1");
        entity.setVersionNo(versionNo);
        entity.setSnapshotSource(versionNo == 1 ? "AI_RESULT" : "REPORT_REVIEW");
        entity.setSnapshotPayload(snapshotPayload);
        return entity;
    }

    private static String snapshotJson(String summary,
                                       List<String> highlights,
                                       List<String> evidenceRefs,
                                       String reviewStatus) {
        try {
            Map<String, Object> report = new java.util.LinkedHashMap<>();
            report.put("reportId", "report-1");
            report.put("taskId", "task-1");
            report.put("versionNo", 1);
            report.put("taskType", "RESEARCH");
            report.put("finalStatus", "SUCCESS");
            report.put("summary", summary);
            report.put("reviewStatus", reviewStatus);

            Map<String, Object> section = new java.util.LinkedHashMap<>();
            section.put("sectionId", "section-1");
            section.put("sectionCode", "HIGHLIGHTS");
            section.put("sectionTitle", "Highlights");
            section.put("sectionOrder", 20);
            section.put("sectionItems", highlights);

            List<Map<String, Object>> evidence = evidenceRefs.stream().map(ref -> {
                Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("evidenceId", "evidence-" + ref);
                item.put("sourceType", "REPORT_META");
                item.put("sourceRefId", ref);
                item.put("evidenceSummary", "summary " + ref);
                return item;
            }).toList();

            Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("versionNo", 1);
            snapshot.put("report", report);
            snapshot.put("sections", List.of(section));
            snapshot.put("evidenceRefs", evidence);
            return new ObjectMapper().writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final class TestDeps {
        private final ResearchReportVersionMapper versionMapper = mock(ResearchReportVersionMapper.class);
        private final ResearchReportSectionMapper sectionMapper = mock(ResearchReportSectionMapper.class);
        private final ReportEvidenceRefMapper evidenceMapper = mock(ReportEvidenceRefMapper.class);
    }
}
