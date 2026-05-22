package com.quant.aiorchestrationservice;

import com.quant.aiorchestrator.AiOrchestrationServiceApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AiOrchestrationServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=${LIVE_MYSQL_JDBC_URL:jdbc:mysql://127.0.0.1:3307/quant_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}",
                "spring.datasource.username=${LIVE_MYSQL_USERNAME:quant}",
                "spring.datasource.password=${LIVE_MYSQL_PASSWORD:quant123}",
                "spring.kafka.listener.auto-startup=false"
        }
)
@AutoConfigureMockMvc
class ReportVersionLiveMySqlIT {

    private static final String TASK_ID = "it-report-version-task";
    private static final String OTHER_TASK_ID = "it-report-version-other-task";
    private static final String REPORT_ID = "it-report-version-report";
    private static final String OTHER_REPORT_ID = "it-report-version-other-report";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedReportVersions() {
        cleanup();
        insertVersion("it-report-version-v1", REPORT_ID, TASK_ID, 1, "AI_RESULT",
                snapshot("summary v1", "PENDING", "old highlight", "ref-1"));
        insertVersion("it-report-version-v2", REPORT_ID, TASK_ID, 2, "REPORT_REVIEW",
                snapshot("summary v2", "APPROVED", "new highlight", "ref-2"));
        insertVersion("it-report-version-wrong-task", OTHER_REPORT_ID, OTHER_TASK_ID, 3, "AI_RESULT",
                snapshot("other summary", "PENDING", "other highlight", "ref-other"));
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update(
                "delete from research_report_version where task_id in (?, ?) or version_id like 'it-report-version-%'",
                TASK_ID,
                OTHER_TASK_ID
        );
    }

    @Test
    void reportVersionReadAndCompareEndpointsUseLiveMysqlRows() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/report/versions", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNo").value(2))
                .andExpect(jsonPath("$.data[0].snapshot.report.summary").value("summary v2"))
                .andExpect(jsonPath("$.data[1].versionNo").value(1));

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/{versionNo}", TASK_ID, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.snapshot.report.summary").value("summary v1"));

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/compare", TASK_ID)
                        .param("fromVersionNo", "1")
                        .param("toVersionNo", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sameVersion").value(false))
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.fromVersion.versionNo").value(1))
                .andExpect(jsonPath("$.data.toVersion.versionNo").value(2))
                .andExpect(jsonPath("$.data.reportFieldsChanged[0].path").value("report.summary"));

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/compare", TASK_ID)
                        .param("fromVersionNo", "1")
                        .param("toVersionNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sameVersion").value(true))
                .andExpect(jsonPath("$.data.changed").value(false))
                .andExpect(jsonPath("$.data.reportFieldsChanged", hasSize(0)))
                .andExpect(jsonPath("$.data.sectionsChanged", hasSize(0)))
                .andExpect(jsonPath("$.data.evidenceRefsAdded", hasSize(0)));

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/{versionNo}", TASK_ID, 9))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/{versionNo}", TASK_ID, 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/tasks/{taskId}/report/versions/compare", TASK_ID)
                        .param("fromVersionNo", "1")
                        .param("toVersionNo", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private void insertVersion(String versionId,
                               String reportId,
                               String taskId,
                               int versionNo,
                               String snapshotSource,
                               String snapshotPayload) {
        jdbcTemplate.update(
                """
                        insert into research_report_version
                            (version_id, report_id, task_id, version_no, snapshot_source, snapshot_payload, deleted)
                        values (?, ?, ?, ?, ?, ?, 0)
                        """,
                versionId,
                reportId,
                taskId,
                versionNo,
                snapshotSource,
                snapshotPayload
        );
    }

    private static String snapshot(String summary, String reviewStatus, String sectionItem, String evidenceRef) {
        return """
                {
                  "versionNo": 1,
                  "report": {
                    "reportId": "%s",
                    "taskId": "%s",
                    "versionNo": 1,
                    "taskType": "RESEARCH",
                    "finalStatus": "SUCCESS",
                    "summary": "%s",
                    "reviewStatus": "%s"
                  },
                  "sections": [
                    {
                      "sectionId": "section-1",
                      "sectionCode": "HIGHLIGHTS",
                      "sectionTitle": "Highlights",
                      "sectionOrder": 20,
                      "sectionItems": ["%s"]
                    }
                  ],
                  "evidenceRefs": [
                    {
                      "evidenceId": "evidence-%s",
                      "sourceType": "REPORT_META",
                      "sourceRefId": "%s",
                      "evidenceSummary": "summary %s"
                    }
                  ]
                }
                """.formatted(REPORT_ID, TASK_ID, summary, reviewStatus, sectionItem, evidenceRef, evidenceRef, evidenceRef);
    }
}
