# Report Boundary Readiness

## Status And Scope

Status: Phase 009 durable report boundary readiness artifact.

Scope: docs-only architecture and governance inventory for the report domain inside the current `ai-orchestration-service` transition host.

This artifact applies the Phase 008 readiness template to the report domain. It clarifies report belongs, authority, contract and behavior boundaries before any later report-service extraction, route migration, permanent modular monolith decision or report contract reshaping is considered.

This artifact does not implement or approve service extraction, route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation, gateway/auth work, config-store migration, database schema change, entity/DTO/VO reshaping, Kafka topic or payload change, frontend reshaping, Python behavior change, business code change or new feature work.

Phase 005 remains the current governance-horizon policy: continue as a modular monolith inside `ai-orchestration-service`, while keeping that host transitional and not final architecture. Phase 006 remains the frozen legacy /api/tasks contract inventory. Phase 007 remains the frontend guardrail for workbench and fallback provenance consumers. Phase 008 remains the common transition-host exit criteria inventory.

## Inputs And Read-Only Inspection Sources

Harness and handoff inputs:

- `docs/harness/00-project-charter.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/state/current-state.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/handoffs/steering-decision-phase-009.md`
- `docs/harness/handoffs/phase-009-architect.md`

Read-only report inventory sources:

- `ReportController.java`
- `ReportQueryService.java`
- `ReportQueryServiceImpl.java`
- `TaskReportService.java`
- `TaskReportServiceImpl.java`
- `ReportVersionService.java`
- `ReportVersionServiceImpl.java`
- `AiResultDomainProjectionService.java`
- `AiResultDomainProjectionServiceImpl.java`
- `LegacyTaskApiContractFreezeTest.java`
- `TaskControllerMappingTest.java`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `TaskReportView.vue`
- `TaskReportCard.vue`
- `ResearchWorkbenchView.vue`
- `quant-ui/scripts/authority-boundary-check.mjs`
- report-generation, Kafka producer and fallback provenance references under `quant-ai-platform/quant-ai-engine/app`
- report-related entity table annotations under `ai-orchestration-service`

No source, test, frontend, Python, config, database, build or deployment file was modified for Phase 009.

## Report Belongs Analysis

Current report facts, read models, version snapshots, evidence projection, review logs, review stats and report review command handling belong to `ai-orchestration-service` as a transition host.

`ai-orchestration-service` currently owns the Java persistence and read-model boundary for:

- task report read model
- report center and report center stats
- report review stats
- report review logs
- report review command
- report versions, version detail and version comparison
- persisted report sections
- persisted report evidence refs
- review records and review audit records currently represented by report review log and human review record tables

`AiResultDomainProjectionService` is a current projection dependency. It writes report and evidence records from `ai.task.result` payloads, and also participates in shared risk and strategy projection. Phase 009 does not move, split, redesign or rename it.

`TaskReportService` is the current report review command handler. It updates report review state, writes review logs, writes human review records and creates report version snapshots through the existing services. Phase 009 does not change that command behavior.

`ReportVersionService` is the current report version snapshot, list, detail and compare service. Phase 009 does not change versioning behavior.

`ReportQueryService` is the current report read-model service for report center, task report, review stats, review logs and version read paths. Phase 009 does not change read-model behavior.

`quant-ai-engine` remains the AI execution and report-generation producer. Its report output, `reportMeta`, `contextSnapshot`, `generationMode`, `fallbackReason`, `reportFallbackReason`, `evidenceRefs` and `evidenceItems` are execution output or provenance metadata until the current Java projection persists selected data into report authority objects.

`quant-ui` remains a contract consumer and display host. It may render report detail, report center, report review workbenches, version panels, evidence, contextSnapshot and fallback provenance. It must not infer report truth from display hydration fields, workbench latest insight or frontend local state.

`research-task-service` remains the formal host for task creation. Existing task-create source-context prefill from report/workbench screens is UI convenience only and does not create report authority.

## Authority Object Inventory

Stable report authority objects:

| Authority object | Current meaning | Current host classification |
| --- | --- | --- |
| `research_report` | report fact root for a task report, including current report/review status fields | `ai-orchestration-service` transition host |
| `research_report_version` | version snapshot authority for report history and comparison | `ai-orchestration-service` transition host |
| `research_report_section` | persisted report section authority and section display source | `ai-orchestration-service` transition host |
| `report_evidence_ref` | persisted report evidence reference authority | `ai-orchestration-service` transition host |
| `research_report_review_log` | report review log record for review actions | `ai-orchestration-service` transition host |
| `human_review_record` | human review/audit record for report review actions | `ai-orchestration-service` transition host |

Authority rules:

- `research_report` remains the current persisted report fact root.
- `research_report_version` remains the version snapshot authority and is not replaced by frontend comparison state.
- `research_report_section` remains the persisted section authority.
- `report_evidence_ref` remains the persisted evidence reference authority.
- `research_report_review_log` and `human_review_record` remain current review/audit transition records for report review actions.
- `reportMeta`, raw payload, `contextSnapshot`, `generationMode`, `fallbackReason`, `reportFallbackReason` and fallback provenance are metadata unless existing Java projection has persisted selected data into a report authority object.
- `research-workbench.latestInsight`, market intelligence report fields, risk/strategy report fields, report center display rows and frontend display hydration fields are not report SoT.
- Risk warning and strategy signal facts remain separate domain facts even when report pages display risk points or strategy context.

Forbidden authority moves:

- No new report SoT may be created in Phase 009.
- No read model may become report command authority.
- No frontend-derived review status, evidence list, display summary, display highlights or display risk points may define persisted report facts.
- No workbench latest insight, report summary card or task-create source context may become report truth.
- No fallback provenance may become model-generated truth or report SoT.
- No documentation in this artifact claims report ownership has moved.

## Report Read-Model Surface Inventory

Report read-model surfaces remain under the frozen legacy /api/tasks namespace:

| Endpoint | Response envelope/type | Binding shape | Current owner |
| --- | --- | --- | --- |
| `GET /api/tasks/{taskId}/report` | `Result<TaskReportVO>` | path variable `taskId` | `ReportController` |
| `GET /api/tasks/{taskId}/report/versions` | `Result<List<ReportVersionVO>>` | path variable `taskId` | `ReportController` |
| `GET /api/tasks/{taskId}/report/versions/compare` | `Result<ReportVersionCompareVO>` | path variable `taskId`; request params `fromVersionNo`, `toVersionNo` | `ReportController` |
| `GET /api/tasks/{taskId}/report/versions/{versionNo}` | `Result<ReportVersionVO>` | path variables `taskId`, `versionNo` | `ReportController` |
| `GET /api/tasks/{taskId}/report/review-logs` | `Result<List<TaskReportReviewLogVO>>` | path variable `taskId` | `ReportController` |
| `GET /api/tasks/report-center` | `Result<ReportCenterPageVO>` | query object `ReportCenterPageQueryDTO` | `ReportController` |
| `GET /api/tasks/report-center-stats` | `Result<ReportCenterStatsVO>` | none | `ReportController` |
| `GET /api/tasks/report-review-stats` | `Result<ReportReviewStatsVO>` | none | `ReportController` |

Current read-model notes:

- Read-model endpoints have no explicit `requirePermission` call in the current Phase 006 contract inventory.
- `GET /api/tasks/{taskId}/full` can include report data inside task full detail, but it is task detail composition and does not become a second report SoT.
- `GET /api/tasks/research-workbench` may display latest report insight, but it remains display-only aggregation.
- Risk, strategy and market intelligence read models may display report review fields, but those displays do not own report facts.

## Report Command Surface Inventory

The only in-scope report command surface is:

| Endpoint | Response envelope/type | Binding shape | Permission behavior | Current owner |
| --- | --- | --- | --- | --- |
| `POST /api/tasks/{taskId}/report/review` | `Result<String>` | path variable `taskId`; request body `TaskReportReviewDTO` | exactly one `PERMISSION_REPORT_REVIEW` check through `RoleAccessConfigService.requirePermission` | `ReportController` and `TaskReportService` |

Current command notes:

- Report review remains a transition-host command inside `ai-orchestration-service`.
- Phase 009 does not change review status behavior, revised summary/highlights/risk points behavior, review comment behavior, audit behavior, permission behavior or response shape.
- Report review command output may be displayed by frontend report pages and report workbenches, but frontend state does not become review authority.
- Any future move of the report review command requires a later Window 0 decision, human approval, a route/permission plan and focused behavior verification.

## Report Version, Evidence And Review-Audit Inventory

Versioning:

- `ReportVersionService.createSnapshot` creates snapshots for AI projection and report review sources.
- `ReportVersionService.listVersions`, `getVersion` and `compareVersions` serve the existing version read contracts.
- `research_report_version` remains the authority for stored version snapshots.
- Version comparison output is a read model, not an authority object.

Evidence:

- `AiResultDomainProjectionService` persists report evidence refs from `reportMeta.evidenceItems` and `reportMeta.evidenceRefs` into `report_evidence_ref`.
- `ReportQueryServiceImpl` reads `report_evidence_ref` and `research_report_section` to hydrate `TaskReportVO` evidence and section display.
- `ReportVersionServiceImpl` includes sections and evidence refs in snapshots and comparison data.
- `report_evidence_ref` is the persisted evidence reference authority. Raw `evidenceRefs` and `evidenceItems` in Python/AI payloads are projection inputs or provenance until persisted.

Review/audit:

- `TaskReportService.reviewReport` updates the report, writes `research_report_review_log`, writes `human_review_record`, updates section review fields where applicable and creates a report version snapshot.
- `TaskReportService.listReviewLogs` and `ReportQueryService.listReviewLogs` serve review log read-model output.
- `human_review_record` is tied to related object type `REPORT` in the current read-model hydration path.
- Audit compliance pages may display report review information, but audit dashboard display is not report command authority.

## AI Projection Dependency

Current projection path:

1. `quant-ai-engine` emits AI task result data to Kafka topic `ai.task.result`.
2. Java result consumers in `ai-orchestration-service` hand result payloads to existing projection logic.
3. `AiResultDomainProjectionService` persists report facts and related report evidence/sections from selected payload metadata.
4. `ReportVersionService.createSnapshot` records report snapshots after AI projection and report review.

`AiResultDomainProjectionService` is a dependency, not a moved or redesigned owner in Phase 009. It currently crosses report, evidence, risk and strategy projection boundaries, so any future split must preserve Kafka topic contracts, idempotency, audit visibility and fallback provenance boundaries.

Projection authority rules:

- `ai.task.result` is an input contract, not the final report SoT.
- Raw payload and `reportMeta` are not report SoT by themselves.
- Existing Java projection determines what becomes persisted report authority.
- Fallback provenance metadata must remain visible and non-authoritative across projection and read-model hydration.
- Downstream `report.generated` remains a listed downstream topic/placeholder in the contract map, not a Phase 009 behavior change.

## Frontend Report Consumer Section

Stable frontend routes:

- `/tasks/:taskId/report`
- `/reports/center`
- `/reports/pending`
- `/reports/approved`
- `/reports/rejected`
- `/research-workbench` as display-only aggregation

Stable frontend API functions:

- `fetchTaskReport`
- `reviewTaskReport`
- `fetchReportCenter`
- `fetchReportCenterStats`
- `fetchReportReviewStats`
- `fetchTaskReportReviewLogs`
- `fetchTaskReportVersions`
- `fetchTaskReportVersion`
- `compareTaskReportVersions`
- `fetchResearchWorkbench` as display-only aggregation
- `fetchTaskFullDetail` as task detail composition that may include report data

Stable frontend report types and display shapes:

- `TaskReport`
- `TaskReportMeta`
- `TaskReportContextSnapshot`
- `TaskReportEvidenceItem`
- `TaskReportSection`
- `TaskReportHumanReviewRecord`
- `TaskReportReviewLog`
- `ReportVersion`
- `ReportVersionCompare`
- `ReportCenterListItem`
- `ReportCenterPageData`
- `ReportCenterStats`
- `ReportReviewStats`
- `ResearchWorkbenchInsight` only as display aggregation

Frontend authority rules:

- `TaskReportView.vue` and `TaskReportCard.vue` may render `contextSnapshot`, `reportMeta`, fallback provenance, evidence, sections and review fields.
- `contextSnapshot` and fallback provenance are display/audit metadata only.
- `reportMeta.reportId` and workbench latest insight IDs may be used for navigation or existing source-context prefill, not report authority.
- `displaySummary`, `displayHighlights`, `displayRiskPoints` and similar hydration fields are display choices, not persisted report facts.
- `executeTaskReportReview` may call the existing review command, but command permission and persistence remain backend contract behavior.
- `resolveTaskReportActionAccess` may determine UI affordance visibility, but it is not report review authority.

## Python Report Generation And Fallback Provenance Touchpoints

Python touchpoints observed in read-only inventory:

- `report_generation_agent.py`
- `langchain_report_service.py`
- `kafka_producer.py`
- `message_models.py`
- `market_data_service.py` as context/fallback input
- planner, intent, financial and risk agents that contribute generation metadata

Current metadata and provenance surfaces:

- `reportMeta`
- `contextSnapshot`
- `generationMode`
- `fallbackReason`
- `reportFallbackReason`
- `planningGenerationMode`
- `planningFallbackReason`
- `intentGenerationMode`
- `intentFallbackReason`
- `financialGenerationMode`
- `financialFallbackReason`
- `riskGenerationMode`
- `riskFallbackReason`
- `evidenceRefs`
- `evidenceItems`

Python boundary rules:

- Python produces execution output and provenance metadata; it does not own final report business facts.
- Rule fallback and model-assisted generation remain auditable through fallback provenance and generation mode metadata.
- Python fallback output must not hide fallback status or become report SoT without Java projection into authority objects.
- Phase 009 does not change Python workflow, fallback behavior, payload shape, Kafka topic usage or provenance fields.

## Related Display-Only Surfaces

Workbench:

- `research-workbench.latestInsight` may display report summary, highlights, risk points, review status, confidence and navigation identifiers.
- It remains display aggregation and existing task-create source-context prefill support only.
- It must not feed report command authority, report projection authority or report SoT.

Risk and strategy:

- Risk warning and strategy signal pages may display report IDs, report review status, review comments, report summary, risk points or strategy context.
- Those display fields do not move report authority into risk or strategy domains.
- Risk facts remain under `risk_warning` and `risk_warning_detail`.
- Strategy facts remain under `strategy_signal` and `strategy_signal_factor`.

Market intelligence:

- Market intelligence rows may display latest report context, but market intelligence display does not own report facts.

Audit compliance:

- Audit compliance pages may display report review and revision information, but audit dashboard display is not report command authority.

Task full detail:

- `GET /api/tasks/{taskId}/full` may include task report composition, but task detail composition does not become an alternate report SoT.

## Stable URL And API Contract Table

All report contracts below must remain stable unless a later approved phase explicitly accepts a migration or breaking change:

| HTTP | Path | Classification | Controller owner | Response envelope/type | Permission behavior |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/tasks/report-center` | report center read model | `ReportController` | `Result<ReportCenterPageVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/report-center-stats` | report center stats read model | `ReportController` | `Result<ReportCenterStatsVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/report-review-stats` | report review stats read model | `ReportController` | `Result<ReportReviewStatsVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/{taskId}/report` | task report read model | `ReportController` | `Result<TaskReportVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/{taskId}/report/versions` | report version read model | `ReportController` | `Result<List<ReportVersionVO>>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/{taskId}/report/versions/compare` | report version comparison read model | `ReportController` | `Result<ReportVersionCompareVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/{taskId}/report/versions/{versionNo}` | report version detail read model | `ReportController` | `Result<ReportVersionVO>` | no explicit report read permission check in current contract |
| GET | `/api/tasks/{taskId}/report/review-logs` | report review log read model | `ReportController` | `Result<List<TaskReportReviewLogVO>>` | no explicit report read permission check in current contract |
| POST | `/api/tasks/{taskId}/report/review` | report review command | `ReportController` | `Result<String>` | `PERMISSION_REPORT_REVIEW` |

Stable contract constraints:

- URL paths and HTTP methods stay unchanged.
- `ReportController` remains owner for listed report endpoints.
- `Result<T>` response envelopes stay unchanged.
- Request bindings stay unchanged, including path variables, query DTOs, request params and request body usage.
- Existing absence of explicit permission checks on report read-model endpoints stays unchanged in Phase 009.
- `POST /api/tasks/{taskId}/report/review` keeps `PERMISSION_REPORT_REVIEW`.
- Frontend route paths, API function names, endpoint strings, call signatures and TypeScript shapes stay unchanged.
- DTO, VO, entity, mapper, database table, Redis key, Kafka topic, Kafka payload and Python payload shapes stay unchanged.

## Current Guardrails Inherited

Phase 003:

- Backend workbench aggregation is display-only.
- Workbench output must not write domain facts or feed backend command/projection authority.

Phase 004:

- Python fallback provenance is visible in existing metadata surfaces.
- Fallback output must not appear as model-generated truth.
- Java projection preserves provenance metadata without using it as business authority.

Phase 005:

- `ai-orchestration-service` remains a modular monolith only for the current governance horizon.
- The service remains a transition host, not final architecture.
- No service extraction, route migration or permanent modular decision is approved.

Phase 006:

- Legacy /api/tasks report routes are frozen as transitional contracts.
- Backend contract tests guard path, method, controller owner, response envelope, binding shape and permission behavior.
- Domain namespace aliases such as `/api/reports` are not approved.

Phase 007:

- Frontend workbench output remains display/navigation/source-context prefill only.
- Frontend `contextSnapshot`, `reportMeta`, fallback provenance, `generationMode`, `fallbackReason` and related fields are display/audit metadata only.
- The existing `authority-boundary-check.mjs` guard prevents current report provenance and workbench fields from feeding command authority.

Phase 008:

- The transition-host exit criteria inventory defines the common readiness gate template.
- Report exit requires report read, evidence, versioning, review command, review audit, projection writer and frontend report consumers to have a single approved target contract or explicitly retained transition path before any ownership move.

## Extraction And Route-Migration Blockers

Current blockers before any report extraction, route migration or permanence decision:

- Report read models, report review command, report center, review stats and version paths still live under frozen legacy /api/tasks routes.
- `ReportController` owns both read and command report surfaces in the transition host.
- `AiResultDomainProjectionService` writes report, evidence, risk and strategy facts from one result projection path.
- Report evidence and sections are projected from AI payload metadata and then persisted through current Java projection.
- Report review command writes review logs, human review records and report version snapshots inside the same service boundary.
- Frontend report consumers are centralized in `quant-ui/src/api/task.ts`, `quant-ui/src/types/task.ts`, report views and report workbench routes.
- `contextSnapshot`, `reportMeta`, raw payload and fallback provenance must remain non-authoritative across any future move.
- Current permission behavior depends on header-based demo auth and JSON role access config, not a production gateway/auth architecture.
- Route migration would need a breaking-change or compatibility decision and Phase 006 inventory update.

## Report-Specific Readiness Gates

Before any future report extraction, route migration or permanent modular monolith decision, a later phase must satisfy these gates:

1. Belongs gate: report facts, report read models, report review command, report evidence, report versions and review audit records have one approved host or an explicitly retained transition path.
2. Authority gate: `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, `research_report_review_log` and `human_review_record` stay the named report authority objects unless a later approved phase changes them.
3. Projection gate: `AiResultDomainProjectionService` ownership is explicitly retained, split or moved with Kafka topic, idempotency, audit and fallback provenance behavior preserved.
4. Contract gate: all current report URLs, HTTP methods, request bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions and TypeScript shapes are inventoried and either preserved or given an approved migration plan.
5. Consumer gate: task report pages, report center pages, pending/approved/rejected report workbenches, workbench latest insight, risk/strategy report displays, market intelligence report fields, audit compliance displays and task full detail consumers are mapped to the approved contract.
6. Fallback provenance gate: `reportMeta`, raw payload, `contextSnapshot`, `generationMode`, `fallbackReason`, `reportFallbackReason`, `evidenceRefs` and `evidenceItems` remain metadata/projection input unless persisted by approved projection logic into authority objects.
7. Permission gate: report review permission behavior is explicitly preserved or replaced by an approved auth/gateway/role authority decision.
8. Verification gate: backend contract tests or static guards cover any approved behavior, route or consumer risk; frontend and Python verification are added only if a later phase changes those areas.
9. Rollback/exit gate: any temporary compatibility path has an owner, retirement trigger and review point.

## Deferred Decisions

The following decisions are deferred to later Window 0 selection plus human approval:

- report-service extraction
- report route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation
- breaking change acceptance
- gateway/auth/JWT implementation
- config-store migration from JSON files
- database schema, entity, mapper, DTO or VO migration
- Kafka topic or payload migration
- Python workflow, fallback, provenance or report-generation behavior change
- frontend route, API function or TypeScript shape reshaping
- splitting `AiResultDomainProjectionService`
- declaring legacy /api/tasks paths final architecture
- declaring `ai-orchestration-service` permanent modular monolith architecture
- new report feature, new agent or new review workflow

## Stop Rules For Later Phases

Stop and return to Window 0/user decision if a later phase requires:

- changing URL paths, HTTP methods, request binding, response envelope, response type, TypeScript shape or permission behavior without approval
- creating route aliases, compatibility bridges, gateway proxies, adapters, fallbacks, wrappers or frontend truth resolvers
- moving report code out of `ai-orchestration-service` without an approved extraction phase
- using workbench latest insight, frontend display fields, raw payload or fallback provenance as report SoT
- using fallback provenance as model-generated truth
- treating `ai-orchestration-service` or legacy /api/tasks as final report architecture
- closing D001 or D002 without preserving later human approval gates
- modifying Java, Python, frontend, database, Kafka, config, dependency, build or deployment files outside an approved file scope
- changing business behavior to make a governance document true
- adding a new feature to justify a governance move

