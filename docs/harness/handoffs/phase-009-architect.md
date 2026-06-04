# Phase 009 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 009 - Report Boundary Readiness.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

## Inputs Read

Required harness files:

- `docs/harness/00-project-charter.md`
- `docs/harness/01-current-architecture.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/handoffs/steering-decision-phase-009.md`

Additional Phase 008 state read because Phase 009 applies the Phase 008 readiness template:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/handoffs/steering-decision-phase-008.md`
- `docs/harness/handoffs/phase-008-architect.md`
- `docs/harness/handoffs/phase-008-implementation.md`
- `docs/harness/handoffs/phase-008-review.md`
- `docs/harness/handoffs/phase-008-final.md`

Read-only planning inspection:

- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportQueryService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/TaskReportService.java`
- `quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportVersionService.java`
- report-related service/projection/test references found by `rg`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- report-related frontend view/component references found by `rg`
- `quant-ui/scripts/authority-boundary-check.mjs`

Phase 009 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only report boundary readiness artifact that applies the Phase 008 readiness template to the report domain in more detail.

The bounded goal is:

- Clarify report-domain belongs, authority, contract and behavior boundaries before any later report-service extraction, route migration, permanent modular-monolith claim or report contract reshaping is considered.
- Inventory report detail, report versions, report version comparison, report center, review logs, review stats, report evidence, report review command, review audit records, AI projection dependency, fallback provenance metadata and frontend report consumers.
- Preserve Phase 005 modular-monolith horizon policy: `ai-orchestration-service` remains a transition host, not final report architecture.
- Preserve Phase 006 legacy `/api/tasks/*` contract freeze.
- Preserve Phase 007 frontend authority guardrails for workbench and fallback/report provenance consumers.
- Define report-specific future readiness gates, blockers and stop rules without choosing or implementing extraction.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only by design. Existing backend and frontend guards already cover the most immediate report route and frontend provenance risks. Window 2 must document those guards and define any later guardrail needs, not add new code or tests in this phase.

## 2. Belongs

Current belongs baseline:

- `ai-orchestration-service` currently hosts report facts, report read models, report versioning, report review command handling and report/evidence projection as a transition host.
- `AiResultDomainProjectionService` is in scope only as the current AI-result projection dependency that writes report sections, evidence refs and report version snapshots. Phase 009 must not redesign or move it.
- `TaskReportService` is in scope only as the current report review command handler and review-log producer.
- `ReportVersionService` is in scope only as the current report version snapshot/read/compare service.
- `ReportQueryService` is in scope only as the current report read-model service.
- `quant-ai-engine` remains the AI execution and report-generation producer. Python report output and fallback provenance are execution output and metadata, not report SoT.
- `quant-ui` remains a contract consumer and display host for report pages, report center pages, report workbenches, evidence display and navigation/prefill convenience.
- `research-task-service` remains the formal host for task creation. Phase 009 must not move report-related task-create prefill into report authority.

In-scope report surfaces:

- task report read model
- report center list and stats
- report review stats
- report review logs
- report review command
- report versions, version detail and version comparison
- report evidence refs and report sections as projected/persisted report facts
- review-related records in `research_report_review_log` and `human_review_record`
- `reportMeta.contextSnapshot`, `reportMeta.evidenceRefs`, fallback provenance and raw payload only as non-authoritative metadata/input to projection
- frontend report consumers that display, navigate or submit the existing review command

Context-only dependencies:

- task runtime/detail APIs, including report data embedded in task full-detail responses
- risk and strategy projections that share `AiResultDomainProjectionService`
- risk/strategy/workbench displays that show report review fields
- audit compliance dashboard
- model, agent, workflow and prompt config used by report generation
- Kafka `ai.task.result`
- downstream placeholder topic `report.generated`

Explicitly excluded:

- report-service extraction
- route migration or aliases
- gateway/auth/JWT work
- database schema or entity reshaping
- Kafka topic or payload migration
- Python report generation behavior changes
- frontend route/API/type reshaping
- new report features, new agents or new review workflows

## 3. Authority

Stable report authority objects:

- `research_report`
- `research_report_version`
- `research_report_section`
- `report_evidence_ref`
- `research_report_review_log`
- `human_review_record`

Report authority rules:

- `research_report` remains the report fact root for current persisted task report output and review status.
- `research_report_version` remains the report version snapshot authority.
- `research_report_section` remains persisted section authority and also contributes report evidence/section display.
- `report_evidence_ref` remains persisted report evidence reference authority.
- `research_report_review_log` and `human_review_record` remain current review/audit transition records for report review actions.
- `reportMeta`, `rawPayload`, `contextSnapshot`, `generationMode`, `fallbackReason`, `reportFallbackReason` and Python fallback provenance remain metadata only unless projected into an authoritative report object by existing Java projection behavior.
- `research-workbench.latestInsight` and report summary fields displayed on workbench/risk/strategy pages are display aggregation, not report SoT.
- Frontend display hydration fields such as `displaySummary`, `displayHighlights` and `displayRiskPoints` are display choices, not new authority objects.
- Risk warning and strategy signal facts remain separate domains even when report pages display risk points or strategy context.

Forbidden authority changes:

- No new report SoT may be created.
- No read model may become report command authority.
- No workbench/latest insight/display field may become report truth.
- No fallback/provenance metadata may become model-generated truth or report SoT.
- No frontend-derived review status, evidence list or report summary may define persisted report facts.
- No documentation may claim report ownership has moved.

## 4. Contract

Stable report URL/API inventory:

| Endpoint | Classification | Current host |
| --- | --- | --- |
| `GET /api/tasks/{taskId}/report` | report read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/{taskId}/report/versions` | report version read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/{taskId}/report/versions/compare` | report version comparison read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/{taskId}/report/versions/{versionNo}` | report version detail read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/{taskId}/report/review-logs` | report review log read model | `ai-orchestration-service` transition host |
| `POST /api/tasks/{taskId}/report/review` | report review command | `ai-orchestration-service` transition host |
| `GET /api/tasks/report-center` | report center read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/report-center-stats` | report center stats read model | `ai-orchestration-service` transition host |
| `GET /api/tasks/report-review-stats` | report review stats read model | `ai-orchestration-service` transition host |

Related stable display/consumer contracts:

- `GET /api/tasks/research-workbench` may display latest report insight but remains display-only aggregation.
- `GET /api/tasks/{taskId}/full` may expose report data inside task detail, but task full detail does not become a separate report SoT.
- risk and strategy list/detail views may display report review fields, but those fields do not move report authority into risk or strategy.

Stable frontend routes and functions:

- `/tasks/:taskId/report`
- `/reports/center`
- `/reports/pending`
- `/reports/approved`
- `/reports/rejected`
- `/research-workbench` as display aggregation only
- `fetchTaskReport`
- `reviewTaskReport`
- `fetchReportCenter`
- `fetchReportCenterStats`
- `fetchReportReviewStats`
- `fetchTaskReportReviewLogs`
- `fetchTaskReportVersions`
- `fetchTaskReportVersion`
- `compareTaskReportVersions`

Stable backend contract details:

- URL paths and HTTP methods stay unchanged.
- `ReportController` remains the owner of the listed report endpoints.
- `Result<T>` response envelopes stay unchanged.
- Request binding stays unchanged, including path variables, query DTOs, request params and request body usage.
- `POST /api/tasks/{taskId}/report/review` keeps `PERMISSION_REPORT_REVIEW`.
- Existing absence of explicit permission checks on report read-model endpoints stays unchanged in this phase.
- DTO, VO, entity, mapper, database table, Redis cache, Kafka topic, Kafka payload and TypeScript shapes stay unchanged.

Stable Kafka/config/Python contracts:

- `ai.task.result` remains the Java projection input.
- `report.generated` remains only a downstream placeholder/listed topic unless a later approved phase changes it.
- JSON config files remain the current transition config store.
- Python report output and fallback provenance stay within existing payload/metadata surfaces.

## 5. Allowed File Scope

Window 2 may modify only Phase 009 documentation files.

Required output files:

- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/handoffs/phase-009-implementation.md`

Optional output file, only if the report inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-009-report-boundary-inventory.md`

Allowed read-only inspection areas:

- report-related Java controller/service/projection/entity/mapper/test files under `ai-orchestration-service`
- report-related frontend API/type/router/view/component/utility files under `quant-ui/src`
- `quant-ui/scripts/authority-boundary-check.mjs`
- report-related Python files under `quant-ai-platform/quant-ai-engine/app`
- `quant-ai-platform/ai-config/**`
- existing harness docs and previous phase handoffs

Window 2 must not write to those read-only inspection areas.

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- `quant-ai-platform/ai-config/**`
- database migration, schema, SQL, seed or mapper files
- Kafka topic constants, producers, consumers, message DTOs or listener code
- Maven, npm, Vite, TypeScript, Docker, deployment, gateway, Nacos, Sentinel or service-discovery files
- dependency or lock files
- `docs/harness/state/current-state.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- prior phase handoffs

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 009.

If satisfying Phase 009 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API / behavior:

- Every report endpoint listed in this handoff keeps the same path, method, controller owner, binding, response envelope, response type and permission behavior.
- No report URL moves to `/api/reports`, `/api/report`, `/api/reviews` or any other new namespace.
- No route alias, compatibility endpoint, gateway proxy, bridge or wrapper is added.
- No endpoint is deleted, renamed, consolidated or split.
- No frontend route, API function name, endpoint string, call signature or TypeScript shape changes.
- No review status, versioning, evidence, report center filtering, report display hydration, cache, audit, retry/cancel, config or permission behavior changes.
- No database table, entity, mapper, DTO, VO, Redis key, Kafka topic, Kafka payload, Python payload or JSON config changes.

Stable architecture:

- `ai-orchestration-service` remains a report transition host, not final report architecture.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 009 does not close D001 or D002.
- Phase 009 does not approve report extraction, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping or permanent modular-monolith architecture.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Report boundary matrices.
- Report endpoint inventory.
- Report consumer inventory.
- Belongs/authority/contract/behavior gate checklists.
- Deferred-decision lists.
- Future guardrail recommendations.
- Stop-rule lists.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java test classes, source files, frontend scripts, Python scripts, build steps or runtime code in this phase.

## 9. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add or approve:

- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper` or compatibility layer
- any test helper or static guard script
- any report route alias or URL bridge
- any gateway or proxy bridge
- any frontend API adapter
- any frontend truth resolver for report data
- any Python fallback bridge or new fallback provenance field
- any config-store bridge
- any temporary report-service wrapper
- any data migration helper
- any new audit/report synchronization bridge

Window 2 may document existing fallback/provenance paths and existing guards as current facts, but must not create new paths or approve them as target architecture.

## 10. Required Report Readiness Artifact Shape

`docs/harness/13-report-boundary-readiness.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Report belongs analysis.
- Report authority object inventory.
- Report read-model surface inventory.
- Report command surface inventory.
- Report version/evidence/review-audit inventory.
- AI projection dependency section.
- Frontend report consumer section.
- Python report-generation and fallback provenance touchpoint section.
- Related display-only surfaces section for workbench, risk and strategy report fields.
- Stable URL/API contract table.
- Current guardrails inherited from Phase 003, Phase 004, Phase 006, Phase 007 and Phase 008.
- Extraction and route-migration blockers.
- Report-specific readiness gates before any future extraction, route migration or permanence decision.
- Deferred decisions.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve extraction, route migration, endpoint aliases, DTO/VO/entity/schema changes, Kafka changes, frontend changes, Python changes, config-store migration, gateway/auth work or business behavior changes.

## 11. Acceptance Conditions

Phase 009 is acceptable only if all conditions hold:

- `docs/harness/13-report-boundary-readiness.md` exists and is the primary durable report boundary readiness artifact.
- `docs/harness/handoffs/phase-009-implementation.md` records exact files changed and verification outcomes.
- The report readiness artifact covers report facts, report evidence, report versions, report review command, review logs, review stats, review audit records, AI projection dependency, fallback provenance metadata and frontend report consumers.
- The artifact names the report authority objects: `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, `research_report_review_log` and `human_review_record`.
- The artifact states that `reportMeta`, `contextSnapshot`, fallback provenance, raw payload, workbench latest insight and frontend display fields are not report SoT.
- The artifact treats `AiResultDomainProjectionService` as a current dependency, not as a moved or redesigned owner.
- The artifact preserves all report URLs, methods, bindings, response envelopes, permission behavior, frontend routes, frontend API functions and TypeScript shapes.
- The artifact preserves Phase 005, Phase 006 and Phase 007 constraints.
- The artifact does not choose report-service extraction, route migration, route aliases, endpoint deletion/rename/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes or new feature work.
- The artifact defines report-specific readiness gates for any later extraction, route migration or permanence decision.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008 inventory or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 009 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 009 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 009 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/13-report-boundary-readiness.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "report|evidence|version|review|review log|review stats|human_review_record|research_report|report_evidence_ref|AiResultDomainProjectionService|contextSnapshot|fallback provenance|frontend|readiness gate|legacy /api/tasks|Phase 005|Phase 006|Phase 007|Phase 008" docs/harness/13-report-boundary-readiness.md docs/harness/handoffs/phase-009-implementation.md
```

Expected result: the report readiness artifact and implementation handoff contain the required report-domain coverage and inherited guardrail references.

Window 2 must run:

```powershell
rg -n "service extraction|route migration|route alias|breaking change|gateway/auth|config-store|database schema|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular" docs/harness/13-report-boundary-readiness.md docs/harness/handoffs/phase-009-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, deferred-decision, prerequisite or future-phase sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|report-center|report-review|/report" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ReportController.java
```

```powershell
rg -n "ReportController|/api/tasks/.+report|report-center|report-review|TaskReportVO|ReportVersionVO|ReportVersionCompareVO|TaskReportReviewDTO|PERMISSION_REPORT_REVIEW" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java
```

```powershell
rg -n "reviewReport|listReviewLogs|createSnapshot|listVersions|getVersion|compareVersions|saveReportEvidenceRefs|saveReportSections|reportMetaValue|ResearchReportReviewLog|HumanReviewRecord|ReportEvidenceRef|ResearchReportSection" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl
```

```powershell
rg -n "fetchTaskReport|reviewTaskReport|fetchReportCenter|fetchReportCenterStats|fetchReportReviewStats|fetchTaskReportReviewLogs|fetchTaskReportVersions|fetchTaskReportVersion|compareTaskReportVersions|/tasks/:taskId/report|/reports/center|/reports/pending|/reports/approved|/reports/rejected" quant-ui/src/api/task.ts quant-ui/src/types/task.ts quant-ui/src/router/index.ts
```

```powershell
rg -n "contextSnapshot|reportMeta|fallbackReason|reportFallbackReason|latestInsight|executeTaskReportReview|resolveTaskReportActionAccess" quant-ui/src/views/task/TaskReportView.vue quant-ui/src/components/task/TaskReportCard.vue quant-ui/src/views/report/ResearchWorkbenchView.vue quant-ui/src/utils quant-ui/scripts/authority-boundary-check.mjs
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Maven, npm build and Python runtime verification are not required because Phase 009 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-009-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter or service wrapper.
- Moving report code from `ai-orchestration-service` into another service.
- Creating or modifying Java, Python, frontend, database, Kafka, config, dependency, test or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key or API type shapes.
- Reclassifying a read model, workbench latest insight, frontend display field, raw payload or fallback provenance as report authority.
- Declaring `ai-orchestration-service` or legacy `/api/tasks/*` paths to be final report architecture.
- Closing D001 or D002.
- Selecting report-service extraction, route migration, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database change or permanent modular-monolith outcome.
- Needing code behavior changes to make the report readiness artifact true.
- Finding that report authority cannot be described without changing the approved Phase 009 scope.
- Needing human approval for breaking changes, service extraction, route migration, config-store migration, gateway/auth implementation or new product features.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, domain object, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start service extraction, route migration, gateway/auth work, config migration, data-ingest split, test implementation, frontend guard edits, Python edits or product feature work. Do not proceed until the user approves this Phase 009 architect handoff.

## Human Approval Request

Please approve this Phase 009 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No business behavior change.
- No new feature work.
- Window 2 may perform docs-only report boundary readiness work inside the allowed file boundaries above.
