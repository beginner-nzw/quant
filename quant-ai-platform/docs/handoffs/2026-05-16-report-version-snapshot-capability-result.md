# Report Version Snapshot Capability Result

Date: 2026-05-16
Status: implemented and verified

## 1. Authority

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-16-post-h4-final-closure-review.md`
- Orchestrator prompt: Report Version Snapshot Capability

H1 owner:

- AI Orchestration Service owns report persistence, report review, report
  read-model APIs, and Java-owned report tables.

Authority result:

- New persistence remains Java-owned under AI Orchestration Service report
  persistence.
- Python was not touched and does not access or own report tables.
- Frontend, Kafka payload/schema, Java common model/messaging, fallback,
  T006/T007, retry/cancel/projection expansion, and historical backfill were
  not touched.

## 2. DB Change

Added `docker/mysql/init/014_report_version_snapshot.sql`.

New table:

- `research_report_version`

Columns:

- `version_id`, `report_id`, `task_id`, `version_no`, `snapshot_source`,
  `snapshot_payload`, timestamps, and `deleted`.

Indexes:

- `UNIQUE KEY uk_report_version (report_id, version_no)`
- `idx_task_id (task_id)`
- `idx_report_id (report_id)`

Ownership:

- Java-owned report persistence under AI Orchestration Service.

Rollback note:

- In local/dev environments, remove or ignore `014_report_version_snapshot.sql`
  and drop `research_report_version` after confirming no local consumers depend
  on historical report versions. No historical report data is backfilled by this
  window.

## 3. Version Semantics

Snapshot creation:

- AI successful report projection creates a snapshot for the current
  `research_report.version_no` after report section and evidence projection.
- Report review creates a snapshot after the approved/rejected review mutation,
  section review mutation, review log insert, and human review record insert.
- Failed/non-success AI result projection does not create a report snapshot.
- Invalid review status does not update the report and does not create a
  snapshot.

`version_no`:

- `research_report.version_no` is the latest current version number.
- Initial report creation remains version `1`.
- AI report regeneration continues to increment through the existing report
  write path before projection.
- Report review revision now increments to the next version before storing the
  review mutation and snapshot.
- `research_report_section.version_no` follows the current report version for
  projected sections and review-mutated sections.
- `research_report_review_log.version_no` records the report version produced
  by that review.

Captured data:

- Report body fields, raw payload, report meta JSON fields, review fields,
  sections, and evidence refs are serialized into deterministic
  `snapshot_payload` JSON.

Latest-report compatibility:

- Existing latest-report reads still use `research_report`,
  `research_report_section`, and `report_evidence_ref`.
- Existing report DTO fields are unchanged.

## 4. HTTP/API Contract

Existing report APIs remain compatible:

- `GET /api/tasks/{taskId}/report`
- `POST /api/tasks/{taskId}/report/review`
- `GET /api/tasks/{taskId}/report/review-logs`
- report center and report stats endpoints

New report-version read APIs:

- `GET /api/tasks/{taskId}/report/versions`
- `GET /api/tasks/{taskId}/report/versions/{versionNo}`

New response DTO:

- `ReportVersionVO`, version-specific only.

Kafka contract:

- No payload/schema change.

## 5. Changed Files

- `docker/mysql/init/014_report_version_snapshot.sql`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/entity/ResearchReportVersionDO.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/vo/ReportVersionVO.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchReportVersionMapper.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportVersionService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AiResultDomainProjectionServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportVersionServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskReportServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/AiResultDomainProjectionReportVersionTests.java`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/ReportVersionServiceTests.java`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskReportVersionTests.java`
- `docs/handoffs/2026-05-16-report-version-snapshot-capability-result.md`

## 6. Verification

Required verification:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD SUCCESS
ai-orchestration-service: Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
```

Focused report verification:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dtest=*Report*Tests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD SUCCESS
Focused report tests: Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

An intermediate targeted command without `-am` failed because dependent common
modules were not built on the targeted invocation; the same targeted test set
passed with `-am`.

## 7. Behavior Evidence

- `ReportVersionServiceTests` verifies deterministic snapshot persistence,
  duplicate `(report_id, version_no)` avoidance, and historical version
  retrieval.
- `AiResultDomainProjectionReportVersionTests` verifies successful AI
  projection creates a report version snapshot and failed projection does not.
- `TaskReportVersionTests` verifies report review increments `version_no`,
  aligns review log and section versions, creates a snapshot, and invalid
  review status does not snapshot or update the report.

## 8. Residual Risk

- Snapshot payload is intentionally stored as JSON rather than a fully
  column-normalized historical model.
- No historical report data is backfilled into `research_report_version`; only
  future controlled updates create snapshots.
- Tests are focused unit tests with mapper mocks, not live MySQL integration
  tests.
