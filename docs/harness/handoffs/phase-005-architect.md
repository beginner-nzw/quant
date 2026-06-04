# Phase 005 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 005 - Decide Service Split or Continue Modular Monolith.

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
- `docs/harness/handoffs/steering-decision-phase-005.md`

Additional handoffs read to preserve frozen constraints:

- `docs/harness/handoffs/phase-006-final.md`
- `docs/harness/handoffs/phase-007-final.md`

Phase 005 is not Phase 001, so the Phase 001 special Java/frontend code reading list is not the controlling scope for this handoff.

## 1. Phase Goal

Decide the next governance-horizon service boundary policy for `ai-orchestration-service` after Phase 001, Phase 002, Phase 003, Phase 004, Phase 006 and Phase 007 reduced the most immediate controller, read-model, authority and contract drift.

The bounded goal is:

- Choose one explicit architecture policy outcome for D001: continue modular monolith, prepare a later domain-service extraction, add gateway/auth first, or block with a no-decision rationale.
- Record why the selected outcome follows `belongs -> authority -> contract -> behavior`.
- Preserve the Phase 006 legacy `/api/tasks/*` contract freeze.
- Preserve the Phase 007 frontend authority-boundary guardrails.
- Define follow-up phase candidates without implementing them.

This is a docs-only architecture/policy phase. It is not a service extraction phase, not a route migration phase, not a backend/frontend/Python implementation phase and not a feature phase.

Window 2 must evaluate every decision in this order:

```text
belongs -> authority -> contract -> behavior
```

Passing a writing/checklist review is not enough if the decision blurs SoT, weakens the Phase 006 contract freeze, or treats a transition host as final architecture without explicit rationale and follow-up constraints.

## 2. Belongs

Current belongs baseline:

- `research-task-service` remains the formal host for task creation and task dispatch outbox.
- `ai-orchestration-service` remains the current transition host for AI status/result/audit consumption, task runtime read models, task control, domain projections, report/risk/strategy/market/audit/config query surfaces and research workbench aggregation.
- `quant-ai-engine` remains the formal host for AI workflow execution and fallback execution provenance.
- `quant-ui` remains a consumer of backend contracts and must not define business truth.

Phase 005 may decide policy for the next governance horizon, but it must not move runtime ownership.

Allowed policy outcomes for Window 2:

1. Continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.
2. Prepare a later `report-service` extraction phase, without extracting it now.
3. Prepare a later `market-event-service` extraction phase, without extracting it now.
4. Prepare a later `risk-service` / `strategy-service` extraction phase, without extracting them now.
5. Add gateway/auth first as a prerequisite phase, without implementing gateway/auth now.
6. Block with an explicit no-decision result if the current evidence is insufficient or the decision would require forbidden changes.

Selection rules:

- Option 1 is the default safe outcome if extraction prerequisites are not clear enough to act without breaking contracts.
- Options 2-4 may be selected only as preparation for a later approved phase; they must identify prerequisites, not perform extraction.
- Option 5 may be selected only if ingress/security ownership is the primary blocker to safe service-boundary evolution.
- Option 6 must be selected if deciding would require code changes, route changes, schema changes, Kafka changes, frontend behavior changes, Python changes or unapproved breaking changes.

Forbidden belongs changes:

- Do not reclassify `ai-orchestration-service` from transition host to final architecture without explicit human approval in a later phase.
- Do not move any domain to a new runtime service in Phase 005.
- Do not create gateway/auth/config/data-ingest ownership by documentation fiat.
- Do not claim that legacy `/api/tasks/*` paths are final architecture; they remain transitional contracts.

## 3. Authority

Phase 005 must preserve the authority matrix from `02-authority-matrix.md`.

Stable authority facts:

- Task creation truth remains `research_task` in `research-task-service`.
- Task runtime truth remains `research_task.status/current_stage` plus `ai_workflow_instance` and `ai_agent_execution`.
- Report truth remains `research_report`, `research_report_version` and `research_report_section` in the current transition host.
- Risk warning truth remains `risk_warning` and `risk_warning_detail` in the current transition host.
- Strategy signal truth remains `strategy_signal` and `strategy_signal_factor` in the current transition host.
- Market event truth remains `market_event`, `market_event_relation` and `market_event_analysis` in the current transition host.
- Config truth remains current JSON config files plus audited config APIs.
- Audit truth remains `audit_record`, `task_message_log`, `ai_prompt_audit` and config change audit files.
- Research workbench remains display aggregation only.
- Fallback provenance remains non-authoritative metadata.

The Phase 005 decision artifact must explicitly state, for the selected option:

- Which authoritative objects stay where they are now.
- Which read models stay transitional.
- Which command contracts would be prerequisites for any later extraction.
- Which authority risks remain open after the decision.
- Which future phase would be required before any SoT moves.

Forbidden authority changes:

- No new source of truth may be created.
- No read model may become command authority.
- No aggregation view may become business truth.
- No frontend-derived state may define task, report, risk, strategy, market, audit or config truth.
- No Python fallback or provenance metadata may become model-generated truth or domain SoT.

## 4. Contract

Phase 005 must preserve all current external contracts.

Stable backend API contracts:

- `POST /api/research/tasks`
- all existing task read/control endpoints under `/api/tasks`
- all Phase 006 frozen non-task legacy `/api/tasks/*` endpoints
- all report, risk, strategy, market, market-intelligence, audit, config and workbench contracts documented in `04-contract-map.md` and guarded by Phase 006

Stable frontend contracts:

- all existing `quant-ui` routes
- all existing `quant-ui/src/api/task.ts` endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript field shapes
- Phase 007 display-only/frontend authority notes and guard intent

Stable Kafka contracts:

- `ai.task.dispatch`
- `ai.task.status`
- `ai.task.result`
- `ai.task.audit`
- `market.event.standardized`
- downstream placeholder topics already listed in `04-contract-map.md`

Stable behavior contracts:

- no user-visible business behavior change
- no endpoint move, alias, deletion, rename or method change
- no request/response shape change
- no permission behavior change
- no database, Redis, Kafka, config, fallback, projection, audit or frontend command behavior change

The Phase 005 decision may recommend future contract phases, but it must not approve or perform them.

## 5. Decision Artifact Requirements

Window 2 must write a clear decision artifact in `docs/harness/handoffs/phase-005-implementation.md`. It may also add `docs/harness/handoffs/phase-005-service-boundary-decision.md` if a separate decision record is clearer, but the implementation handoff must summarize the selected option and link or reference any separate file.

The decision artifact must include:

- selected option, exactly one from the allowed options above, or `block/no-decision`
- rejected options and concise rationale
- belongs analysis by domain
- authority analysis by domain
- contract stability analysis
- behavior and verification risk
- required follow-up phases
- explicit out-of-scope list
- blocker or residual-risk list
- exact files changed by Window 2
- verification command outcomes

If Window 2 selects an extraction-preparation option, it must state that extraction is not approved and must define the later phase gate needed before any code move.

If Window 2 selects modular monolith continuation, it must state that this is a next-governance-horizon policy, not a permanent final architecture, unless the user separately approves permanence in a later phase.

## 6. Allowed File Scope

Window 2 may modify:

- `docs/harness/handoffs/phase-005-implementation.md`
- `docs/harness/handoffs/phase-005-service-boundary-decision.md`, optional

Window 2 may modify the following core harness docs only if the change is strictly policy documentation for the selected Phase 005 outcome and does not update phase status:

- `docs/harness/03-host-ownership.md`, limited to next-governance-horizon host policy or extraction gates
- `docs/harness/05-transition-lifetime.md`, limited to T1 service-boundary policy, exit criteria or Phase 005 decision notes
- `docs/harness/04-contract-map.md`, limited to stating that existing contracts remain stable under the selected policy

Preferred shape:

- Keep the durable decision primarily in the Phase 005 handoff files.
- Let Window 4 update `current-state`, debt status and backlog after Window 3 approval.

## 7. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- `quant-ai-platform/ai-config/**`
- database migration, schema, SQL, seed or mapper files
- Kafka topic constants, producers, consumers, message DTOs or listener code
- Maven, npm, Vite, TypeScript, Docker, deployment, gateway, Nacos, Sentinel or service-discovery files
- dependency or package-lock files
- `docs/harness/state/current-state.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- prior phase handoffs

If satisfying Phase 005 appears to require any forbidden file, Window 2 must stop as blocked.

## 8. Must Stay Stable

Stable URL / API rules:

- All existing URL paths remain unchanged.
- All existing HTTP methods remain unchanged.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts.
- No domain URL aliases are added.
- No endpoint is deleted, moved, renamed, consolidated or split.
- No request binding, response envelope, response generic type or TypeScript shape changes.
- No permission behavior changes.

Stable behavior rules:

- No service is extracted.
- No service boundary is implemented.
- No route migration is implemented.
- No gateway/auth/config-store/data-ingest/Nacos/Sentinel implementation is started.
- No database table, schema or migration changes.
- No Kafka topic, payload or consumer/producer changes.
- No frontend route, command, display or authority behavior changes.
- No Python workflow, fallback or provenance behavior changes.
- No new product feature or agent work.

Stable governance rules:

- Phase 006 remains the frozen contract inventory for legacy mixed-domain `/api/tasks/*` surfaces.
- Phase 007 remains the frontend display/metadata authority guard for current workbench and fallback provenance consumers.
- `ai-orchestration-service` remains a transition host unless a later approved phase changes that status.

## 9. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections, tables and checklists.
- Architecture option tables.
- Domain-by-domain decision matrices.
- Follow-up phase candidate lists.
- Exit criteria and stop-rule lists.

Allowed scripts/classes/methods:

- None.

Window 2 must not add test classes, source files, scripts, build steps or runtime code in this phase.

## 10. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add:

- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper` or compatibility layer
- any test helper or static guard script
- any API alias or route bridge
- any gateway or proxy bridge
- any frontend API adapter
- any Python fallback bridge
- any config-store bridge
- any temporary service wrapper
- any data migration helper

Phase 005 may describe that a future phase might need a bridge, but it must not create or approve one as implementation.

## 11. Acceptance Conditions

Phase 005 is acceptable only if all conditions hold:

- The decision artifact selects exactly one allowed policy outcome or explicitly blocks with no-decision.
- The selected outcome is justified in `belongs -> authority -> contract -> behavior` order.
- The decision preserves all current URLs, HTTP methods, request/response contracts, frontend routes, Kafka topics, database schema and runtime behavior.
- The decision does not reclassify `ai-orchestration-service` as final architecture without explicit future human approval.
- The decision does not reclassify legacy non-task `/api/tasks/*` paths as final architecture.
- The decision preserves Phase 006 legacy contract freeze and Phase 007 frontend authority guardrails.
- The decision identifies required follow-up phases before any service extraction, route migration, gateway/auth work, config-store migration, data-ingest split or product feature work.
- Any extraction-preparation option includes prerequisites and blockers, not code movement.
- Any modular-monolith continuation option includes review horizon, exit criteria and residual D001 risk.
- No business code, tests, scripts, dependencies, configs, database files, frontend files or Python files are changed.
- Any optional edits to `03-host-ownership.md`, `04-contract-map.md` or `05-transition-lifetime.md` are documentation-only and consistent with the selected outcome.
- `git diff --name-only` shows only allowed Phase 005 documentation files plus any pre-existing unrelated dirty files clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness`:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 005 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 005 implementation claim.

Window 2 must run:

```powershell
rg -n "selected option|Selected option|block/no-decision|belongs|authority|contract|behavior|/api/tasks|Phase 006|Phase 007|follow-up|out of scope" docs/harness/handoffs/phase-005-implementation.md
```

Expected result: the implementation handoff contains the selected option or block result, the required decision-order analysis, contract-stability references and follow-up/out-of-scope sections.

If Window 2 creates a separate decision record, it must also run:

```powershell
rg -n "selected option|Selected option|belongs|authority|contract|behavior|/api/tasks|transition host|follow-up" docs/harness/handoffs/phase-005-service-boundary-decision.md
```

Expected result: the separate decision record contains the selected option and the required analysis.

Window 2 must run:

```powershell
rg -n "route migration|service extraction|extract .*service|breaking change|gateway/auth|Nacos|Sentinel|database schema|Kafka|frontend|Python" docs/harness/handoffs/phase-005-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, prerequisite or future-phase sections, not in completed implementation claims.

Maven, npm and Python verification commands are not required because business code, frontend code and Python code are forbidden in this phase. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-005-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type or permission behavior.
- Adding a route alias, compatibility bridge, gateway proxy or service wrapper.
- Moving any code from `ai-orchestration-service` into another service.
- Creating or modifying Java, Python, frontend, database, Kafka, config, dependency or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload or API type shapes.
- Reclassifying a read model, workbench aggregation, frontend display field or fallback provenance as authority.
- Declaring `ai-orchestration-service` or legacy `/api/tasks/*` paths to be final architecture without a separate human-approved phase.
- Closing D001 completely without a review horizon and exit criteria.
- Selecting an extraction option without enough evidence to preserve current contracts.
- Selecting gateway/auth first because it is desirable rather than because it is a necessary prerequisite.
- Needing code inspection beyond lightweight documentation confirmation to decide safely.
- Needing human approval for breaking changes, service extraction, route migration, config-store migration, gateway/auth implementation or new product features.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact decision point or contract that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/policy Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start service extraction, route migration, gateway/auth work, config migration or product feature work. Do not proceed until the user approves this Phase 005 architect handoff.

## Human Approval Request

Please approve this Phase 005 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths stable.
- No business behavior change.
- No new feature work.
- Window 2 may perform docs-only architecture/policy work inside the allowed file boundaries above.
