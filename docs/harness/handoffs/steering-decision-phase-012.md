# Steering Decision - Phase 012

## Status

Window: Window 0 - Steering.

Decision: Phase 012 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 012 architect handoff before any implementation window can start.

## Inputs Read

Fixed harness artifacts:

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
- `docs/harness/handoffs/phase-000-harness-baseline.md`

Handoff directory was listed.

Latest completed phase from `current-state.md`:

- Phase 011 - Risk/Strategy Projection Ownership Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-011-final.md`

Matching Phase 011 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-011.md`
- `docs/harness/handoffs/phase-011-architect.md`
- `docs/harness/handoffs/phase-011-implementation.md`
- `docs/harness/handoffs/phase-011-review.md`
- `docs/harness/handoffs/phase-011-final.md`

Durable readiness artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`

Missing matching handoff files:

- None for Phase 011.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 011 - Risk/Strategy Projection Ownership Boundary.
- Latest final handoff: `phase-011-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 011 are completed and frozen by Window 4.

## Current State Summary

- Phase 011 completed docs-only risk/strategy projection ownership boundary readiness.
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md` is now the durable risk/strategy readiness artifact.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- `AiResultDomainProjectionService` remains a current shared projection dependency, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- JSON files under `quant-ai-platform/ai-config` remain mutable runtime config transition facts.
- Header-based demo auth remains a transition mechanism.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: D007 remains open because runtime config facts are JSON/file-backed and read by both Java and Python. Role access config also participates in current permission behavior with request headers.
- Contract ambiguity: config APIs and JSON file schemas are mutable runtime contracts, but no target store, versioning, rollback, reader contract or migration decision is approved.
- Transition host reduction: service extraction and route migration are still blocked by config/auth/contract decisions.
- Eval/test coverage: useful, but lower order than config authority and contract clarification.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 012 - Config Store Decision Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| Auth/Gateway Permission Authority Decision | 1 | 2 | 1 | 1 | 1 | 1 | 7 | Fallback |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Risk-service extraction, strategy-service extraction, projection-split, route or Kafka planning | 1 | 1 | 1 | 2 | 0 | 1 | 6 | Defer |
| Market-service extraction, data-ingest-service extraction, market route or market config planning | 1 | 1 | 2 | 2 | 0 | 0 | 6 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 2 | 2 | 0 | 0 | 6 | Defer |
| Direct permanent modular-monolith declaration | 1 | 1 | 1 | 1 | 0 | 0 | 4 | Defer |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break notes:

- Config store wins because it directly improves authority and contract clarity before lower-order transition reduction.
- Auth/gateway beats generic eval as fallback because it addresses authority, while generic eval is lower in the state-machine decision order.
- Route migration and extraction candidates carry high behavior risk and require prior config/auth/contract decisions plus explicit human approval.

## Primary Candidate

Phase 012 - Config Store Decision Boundary.

Bounded goal:

- Clarify current config belongs, authority, contract and behavior boundaries for JSON-backed runtime config before any config-store migration, gateway/auth work, data-ingest split, route migration, service extraction or permanent architecture decision.
- Inventory current config authority facts, including:
  - `agent-configs.json`
  - `workflow-configs.json`
  - `model-strategies.json`
  - prompt template config files
  - `event-source-configs.json`
  - `event-auto-trigger-configs.json`
  - `role-access-configs.json`
  - `config-change-audits.json`
  - `event-ingest-histories.json`
- Clarify Java and Python reader contracts and current audited mutation behavior.
- Decide, for the next governance horizon only, whether JSON config continues as the transition store or whether DB, Nacos or a hybrid store should be selected as a future migration target.
- Produce a durable docs artifact, expected as `docs/harness/16-config-store-decision-boundary.md`.

Why this is the next bounded step:

- Phase 008 identified config as a transition-host domain with unresolved store, versioning, audit, rollback and Java/Python reader contracts.
- Phase 010 depends on event source config, ingest history and auto-trigger config remaining clearly bounded.
- Phase 011 depends on role access config and header-based demo auth staying transition-only.
- Gateway/auth, route migration and service extraction all become riskier if config and role authority remain unsettled.
- This phase is smaller than a real config migration. It should classify and decide boundaries without changing config files, APIs, runtime behavior or deployment.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No production behavior change.
- No JSON file mutation.
- No DB/Nacos migration.
- No gateway/auth/JWT implementation.
- No route migration, endpoint alias or breaking change.
- No Java, Python, frontend, database, Kafka, Redis, deployment or dependency change unless a later Window 1 explicitly proposes a narrow read-only/static verification scope and the user approves it.

## Fallback Candidate

Auth/Gateway Permission Authority Decision.

Fallback condition:

- Use this if the user rejects config-store decision work and wants permission authority clarified first.

Bounded fallback goal:

- Clarify the boundary between header-based demo auth, `role-access-configs.json`, frontend local role selection, backend `UserContext`, existing `requirePermission` calls and any future gateway/JWT/auth-service direction.
- Decide whether current header/config auth continues as demo-only for the next governance horizon or whether a later gateway/auth phase should be selected.
- Preserve current permission behavior unless a later approved phase explicitly allows auth implementation.

Why it is not primary:

- The current permission model depends on JSON role access config, so config-store authority should be clarified before replacing or reshaping auth.
- Auth/gateway work can quickly imply deployment, session, JWT, route, frontend and permission behavior changes.
- No production auth requirement or main path breakage is registered in current harness state.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 011 are completed and frozen.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs; migration requires breaking-change or compatibility approval and an updated contract inventory.
- Report extraction or report route migration is deferred because Phase 009 created readiness documentation only; projection, auth, route and config blockers remain.
- Market-service extraction or data-ingest-service extraction is deferred because Phase 010 created readiness documentation only; event source config, ingest history, auto-trigger config and real ingest ownership remain unsettled.
- Risk-service extraction, strategy-service extraction, projection split, route migration or Kafka downstream planning is deferred because Phase 011 created readiness documentation only; shared projection, auth, route and config blockers remain.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase instead of replacing config authority work.
- Direct permanent modular-monolith declaration is too far because the transition host is explicitly not final architecture.
- New feature and new agent work remain ineligible while authority, contract and transition-host lifecycle work remain open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact config scope and whether `docs/harness/16-config-store-decision-boundary.md` is the required durable artifact.
- Current config authority objects and file-backed facts:
  - agent, workflow, model strategy, prompt template, event source, event auto-trigger, role access, config audit and ingest history files.
- Which files are config facts, audit facts, ingest history facts, role authority inputs, Java reader inputs, Python reader inputs, frontend display data or transition-only demo inputs.
- Current stable backend config API inventory to preserve:
  - `GET /api/tasks/model-agent-config`
  - `GET /api/tasks/role-access-configs`
  - `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}`
  - `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}`
  - `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}`
  - `POST /api/tasks/model-agent-config/event-sources/{sourceCode}`
  - `POST /api/tasks/model-agent-config/agents/{agentCode}`
  - `POST /api/tasks/model-agent-config/workflows/{workflowCode}`
  - `POST /api/tasks/model-agent-config/role-access/{roleCode}`
- Current frontend config center consumers, API functions, TypeScript shapes and local role utilities that must remain consumers rather than permission truth.
- Current Java config services, config dashboard query services, `RoleAccessConfigService` and `ConfigChangeAuditService` boundaries.
- Current Python config readers and settings files that must be preserved as reader contracts.
- Whether Phase 012 only records a decision boundary or also selects a future target store for a later migration phase.
- Required acceptance gates for belongs, authority, contract, transition lifetime and behavior.
- Verification commands appropriate for docs-only work, such as read-only inventory checks for config files, config APIs, Java services, Python readers and frontend consumers.
- Stop rules if Window 2 discovers that the phase requires config file mutation, DB/Nacos work, gateway/auth work, route changes, permission behavior changes, schema/version migrations, Java/Python/frontend changes or business behavior changes.

Default constraints Window 1 must carry forward:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.
- No config file mutation.
- No DB/Nacos/config-store migration implementation.
- No gateway/auth/JWT implementation.
- Expected Window 2 type: docs-only by default.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, dependency, build, deployment or runtime behavior change by default.
- No JSON config mutation.
- No migration to DB, Nacos or hybrid store.
- No gateway/auth/JWT implementation.
- No service extraction.
- No route migration, route alias, endpoint rename, endpoint deletion or endpoint consolidation.
- No breaking change.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, frontend type or Python payload change.
- No new helper, adapter, bridge, fallback, wrapper, resolver, proxy, compatibility endpoint or temporary config-store bridge.
- No reclassification of `ai-orchestration-service`, JSON config files, legacy `/api/tasks/*` paths, header-based demo auth, workbench aggregation or fallback provenance as final architecture.
- No new product feature or new agent work.

## Human Approval Result

User approved the proposed next phase.

- Primary: Phase 012 - Config Store Decision Boundary.
- Fallback: Auth/Gateway Permission Authority Decision.

Approval constraints:

- No breaking changes.
- URL paths remain stable.
- No business behavior change.
- No new feature work.
- No config mutation or config-store migration implementation.
- Window 2 will be docs-only by default after Window 1 planning is separately approved.

Window 0 stops here. Window 1 is allowed to start Phase 012 architecture planning, but no implementation is approved.
