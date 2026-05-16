# Report Version Diff Compare Backend Result

Date: 2026-05-16
Status: implemented and verified with local no-fork Maven workaround

## 1. Authority

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-16-report-version-snapshot-capability-result.md`
- `docs/handoffs/2026-05-16-report-version-snapshot-result-review.md`
- Orchestrator prompt: Report Version Diff Or Compare Backend

H1 owner:

- AI Orchestration Service owns report persistence, report read-model APIs, and
  Java-owned report tables.

Authority result:

- Work stayed inside the AI Orchestration Service report read-model and
  Java-owned report persistence boundary.
- Python, frontend, SQL schema, Kafka payload/schema, Java common
  model/messaging, Redis authority, fallback, T006/T007, retry/cancel, and
  projection expansion were not touched.
- H1 boundary was not reopened.

## 2. HTTP Contract

Existing report version APIs remain compatible:

- `GET /api/tasks/{taskId}/report/versions`
- `GET /api/tasks/{taskId}/report/versions/{versionNo}`

New backend-only compare API:

- Method: `GET`
- Route: `/api/tasks/{taskId}/report/versions/compare`
- Query params:
  - `fromVersionNo`
  - `toVersionNo`
- Response wrapper: existing `Result<ReportVersionCompareVO>`
- Missing request query params use the existing Spring/controller missing-param
  error path.
- Missing, invalid, or wrong-task versions follow the existing version-detail
  service style and return `null` from the service response.

`ReportVersionCompareVO` response fields:

- `taskId`, `reportId`, `fromVersionNo`, `toVersionNo`, `sameVersion`,
  `changed`
- `fromVersion`, `toVersion` summaries
- `reportFieldsChanged`
- `sectionsAdded`, `sectionsRemoved`, `sectionsChanged`
- `evidenceRefsAdded`, `evidenceRefsRemoved`, `evidenceRefsChanged`
- `reviewFieldsChanged`

## 3. Compare Semantics

Implementation parses the existing `research_report_version.snapshot_payload`
JSON for both versions and produces a deterministic bounded field-level diff:

- report fields changed:
  - stable report body/meta fields under `snapshot.report`
- review fields changed:
  - review fields under `snapshot.report`
- sections:
  - keyed by `sectionCode`, then `sectionId`, then canonical item value
  - added, removed, and field changes are reported separately
- evidence refs:
  - keyed by `sourceRefId`, then `evidenceId`, then canonical item value
  - added, removed, and field changes are reported separately

Same-version compare returns `sameVersion=true`, `changed=false`, and empty
diff buckets.

Cross-task or wrong-report access is constrained by the existing
`task_id + version_no + deleted=0` read. A version from another task is not
selected and the compare response is `null`, matching the current version-detail
not-found style.

## 4. Changed Files

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/domain/vo/ReportVersionCompareVO.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportVersionService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportVersionServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/ReportVersionServiceTests.java`
- `docs/handoffs/2026-05-16-report-version-diff-compare-backend-result.md`

## 5. Behavior Evidence

Focused tests added in `ReportVersionServiceTests` verify:

- comparing two valid versions returns deterministic diff output for report
  fields, sections, evidence refs, and review fields;
- comparing the same version returns no-op diff output;
- missing or wrong-task version access returns `null`;
- existing version list/detail read shape remains usable after the service
  changes.

Existing latest-report APIs and snapshot write behavior were not changed.

## 6. Verification

Required focused command attempted:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dtest=*Report*Tests" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD FAILURE before tests; Surefire forked JVM failed during VM
initialization because the local Windows page file/memory could not reserve GC
or heap memory. Tests run: 0.
```

Focused verification executed with the same test selector and no fork:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dtest=*Report*Tests" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test
```

Result:

```text
BUILD SUCCESS
Focused report tests: Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

Required broader command attempted:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD FAILURE before AI Orchestration Service tests; Surefire forked JVM failed
in quant-common-core because the local Windows page file was too small.
```

Broader service verification executed with no fork:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test
```

Result:

```text
BUILD SUCCESS
AI Orchestration Service tests: Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
```

The no-fork broader run logged Surefire report-file access warnings in common
modules, but Maven completed with `BUILD SUCCESS` and the AI Orchestration
Service test set passed.

## 7. Residual Risk

- Diff semantics are intentionally bounded to stable fields in the existing
  snapshot JSON; this is not a full semantic document diff.
- Tests are mapper-mocked focused unit tests plus service suite execution, not
  live MySQL integration tests.
- Default forked Surefire verification is blocked on this machine by local
  memory/pagefile limits; no-fork verification passed.

## 8. Commit

- Final commit hash is reported by the execution window final response.
