# Phase 012 Final Handoff

## Status

Window: Window 4 - Handoff.

Phase: Phase 012 - Config Store Decision Boundary.

phase status: completed with residual risk.

Latest review: `docs/harness/handoffs/phase-012-review.md`.

Window 3 decision: approve.

Fix passes: none.

## Inputs Read

- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/state/current-state.md`
- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/handoffs/steering-decision-phase-012.md`
- `docs/harness/handoffs/phase-012-architect.md`
- `docs/harness/handoffs/phase-012-implementation.md`
- `docs/harness/handoffs/phase-012-review.md`

No Phase 012 fix implementation or review-fix handoff exists.

## Completed Scope

Phase 012 is frozen as docs-only architecture/governance work.

Completed durable artifact:

- `docs/harness/16-config-store-decision-boundary.md`

The phase completed these bounded outcomes:

- Classified current config belongs, authority, contract and behavior boundaries.
- Recorded `agent-configs.json`, `workflow-configs.json`, `model-strategies.json`, `prompt-templates/*.txt`, `event-source-configs.json`, `event-auto-trigger-configs.json`, `role-access-configs.json`, `config-change-audits.json` and `event-ingest-histories.json`.
- Recorded Java config APIs/services, `research-task-service` role-access reader, Python config readers and frontend config consumers.
- Recorded the next-governance-horizon store decision: JSON config files and prompt template files remain current runtime transition stores.
- Deferred DB, Nacos and hybrid stores to later Window 0 decisions and human approval.
- Preserved Phase 005 through Phase 011 guardrails.

No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed in Phase 012.

## Unchanged Contracts

The following config contracts remain unchanged:

- `GET /api/tasks/model-agent-config`
- `GET /api/tasks/role-access-configs`
- `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}`
- `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}`
- `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}`
- `POST /api/tasks/model-agent-config/event-sources/{sourceCode}`
- `POST /api/tasks/model-agent-config/agents/{agentCode}`
- `POST /api/tasks/model-agent-config/workflows/{workflowCode}`
- `POST /api/tasks/model-agent-config/role-access/{roleCode}`
- related market source config and ingest history read surfaces
- `POST /api/research/tasks` permission behavior through `TaskRoleAccessService`

URL paths, HTTP methods, controller owners, request bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, Java path-resolution behavior, file-backed audit behavior, ingest history behavior and Python reader paths remain stable.

## Authority And Transition State

JSON config files and prompt template files remain current runtime transition stores, not final architecture.

`ai-orchestration-service` remains the config transition host. It was not promoted to final architecture and no config-service extraction was approved.

`research-task-service` remains a role-access reader for task-create permission behavior, not a config owner.

`quant-ai-engine` remains a config reader, not a config owner or mutation host.

`quant-ui` remains a consumer/display/UI-gating host, not config or permission source of truth.

Header-based demo auth remains a transition mechanism, not production auth architecture.

## Remaining Debt

- D001 remains open: `ai-orchestration-service` still hosts multiple domains.
- D002 remains open: config and other non-task domain APIs still use frozen legacy `/api/tasks/*` paths.
- D003 remains open: future display/provenance/config-display surfaces still need non-authoritative guardrails.
- D007 remains open: JSON config and prompt template files remain transition stores, not final config architecture.
- D008 remains open: header-based demo auth and role-access headers remain transition permission inputs, not production auth.

## Latest State For Window 0

Window 0 should automatically discover:

- latest frozen phase: Phase 012 - Config Store Decision Boundary
- current phase: none approved
- current phase status: no active phase is approved
- latest final handoff: `docs/harness/handoffs/phase-012-final.md`
- durable Phase 012 artifact: `docs/harness/16-config-store-decision-boundary.md`
- open blockers: none registered

Window 0 must read this final handoff plus the matching Phase 012 steering, architect, implementation and review handoffs. It should also consume the durable Phase 008/009/010/011/012 artifacts before scoring the next candidate.

Window 0 must not skip to implementation. It must score candidates through `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.

## Recommended Candidate Inputs For Window 0

- Auth/gateway decision phase.
- Legacy route migration decision phase.
- Config-store migration target/scoping, DB/Nacos/hybrid readiness, schema/versioning or audit/rollback planning only if Window 0 and the user explicitly choose to act on Phase 012 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.

Window 4 does not select the next phase.

## Files Changed In This Handoff

- `docs/harness/state/current-state.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/handoffs/phase-012-final.md`

Business code changed by Window 4: none.
