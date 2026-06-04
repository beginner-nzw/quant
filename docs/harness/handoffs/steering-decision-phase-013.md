# Steering Decision - Phase 013

## Status

Window: Window 0 - Steering.

Decision: Phase 013 approved by user for Window 1 architecture planning.

Human approval status: approved.

This file does not approve implementation. Window 1 is now allowed to start architecture planning and must produce a Phase 013 architect handoff before any implementation window can start.

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

- Phase 012 - Config Store Decision Boundary.

Highest final handoff discovered:

- `docs/harness/handoffs/phase-012-final.md`

Matching Phase 012 handoffs consumed:

- `docs/harness/handoffs/steering-decision-phase-012.md`
- `docs/harness/handoffs/phase-012-architect.md`
- `docs/harness/handoffs/phase-012-implementation.md`
- `docs/harness/handoffs/phase-012-review.md`
- `docs/harness/handoffs/phase-012-final.md`

Durable readiness artifacts consumed:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/16-config-store-decision-boundary.md`

Missing matching handoff files:

- None for Phase 012.

## Startup Recovery Result

`docs/harness/state/current-state.md` and the handoff directory agree:

- Current active phase: none approved.
- Last completed phase: Phase 012 - Config Store Decision Boundary.
- Latest final handoff: `phase-012-final.md`.
- Open blockers: none registered.

Bootstrap Phase 001 is not current fact because Phase 001 through Phase 012 are completed and frozen by Window 4.

## Current State Summary

- Phase 012 completed docs-only config-store decision-boundary work.
- `docs/harness/16-config-store-decision-boundary.md` is now the durable config boundary artifact.
- JSON config files under `quant-ai-platform/ai-config` and prompt template files under `quant-ai-platform/prompt-templates` remain the current runtime transition stores for the next governance horizon.
- DB, Nacos and hybrid config stores remain deferred future migration targets only.
- Header-based demo auth and role-access headers remain transition permission inputs, not production auth architecture.
- `ai-orchestration-service` remains a multi-domain transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006.
- Open debt remains for D001, D002, D003, D007 and D008.
- No main path breakage is registered.

## Latest Completed Or Blocked Phase

Latest completed phase:

- Phase 012 - Config Store Decision Boundary.

Latest blocked phase:

- None registered.

Handoff files read for the latest completed phase:

- `steering-decision-phase-012.md`
- `phase-012-architect.md`
- `phase-012-implementation.md`
- `phase-012-review.md`
- `phase-012-final.md`

## Decision Order Result

Decision order from `docs/harness/10-steering-state-machine.md`:

```text
main path breakage -> authority ambiguity -> contract ambiguity -> transition host reduction -> eval/test coverage -> new feature work
```

Evaluation:

- Main path breakage: none registered.
- Authority ambiguity: D008 remains open because current permission behavior still depends on header-based demo auth, request role context, `role-access-configs.json`, frontend local role selection and service-local permission checks.
- Contract ambiguity: D002 remains open because legacy non-task `/api/tasks/*` paths are frozen transitional contracts, but route migration requires explicit approval and is higher behavior risk.
- Transition host reduction: D001 remains open, but extraction, projection split and route migration are blocked by auth, config, route and contract readiness gates.
- Eval/test coverage: useful, but lower order than authority and contract decisions.
- New feature work: not eligible while authority, contract and transition-host lifecycle debt remains.

## Candidate Score Table

Scores are 0-2. Behavior risk is inverted: 2 means low risk.

| Candidate | Main path protection | Authority clarity | Contract clarity | Transition host reduction | Behavior risk | Verification feasibility | Total | Steering result |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| Phase 013 - Auth/Gateway Permission Authority Boundary | 1 | 2 | 2 | 1 | 2 | 2 | 10 | Primary |
| Config-store schema/versioning and audit/rollback readiness | 1 | 1 | 2 | 1 | 2 | 2 | 9 | Fallback |
| Generic eval/static guard expansion | 1 | 1 | 1 | 0 | 2 | 2 | 7 | Defer by decision order |
| Legacy route migration decision phase | 0 | 1 | 2 | 2 | 0 | 1 | 6 | Defer |
| Market-service or data-ingest-service extraction planning | 1 | 1 | 1 | 2 | 0 | 1 | 5 | Defer |
| Risk-service, strategy-service or projection-split planning | 1 | 1 | 1 | 2 | 0 | 1 | 5 | Defer |
| Report extraction or report route-migration planning | 1 | 1 | 1 | 2 | 0 | 1 | 5 | Defer |
| Direct permanent modular-monolith declaration | 1 | 1 | 1 | 1 | 0 | 0 | 4 | Defer |
| New product feature or new agent work | 0 | 0 | 0 | 0 | 0 | 0 | 0 | Not eligible |

Tie-break and ordering notes:

- Auth/gateway permission authority is selected because it addresses the highest-order remaining authority ambiguity after Phase 012.
- Config-store schema/versioning is a strong fallback, but Phase 012 already selected JSON/prompt files as current runtime transition stores for the next governance horizon. Its remaining work is future migration readiness rather than the next authority source question.
- Route migration and service extraction remain lower-order and higher-risk because they require auth, contract and compatibility decisions first.

## Primary Candidate

Phase 013 - Auth/Gateway Permission Authority Boundary.

Bounded goal:

- Clarify the current permission authority boundary before gateway/auth, route migration, service extraction or production security work is considered.
- Inventory the current permission inputs and consumers:
  - `role-access-configs.json`
  - request headers such as `X-User-Id` and `X-User-Role`
  - `SecurityUtils.currentUserRole()`
  - `RoleAccessConfigService`
  - `TaskRoleAccessService`
  - frontend `auth.ts`, `requestHeaders.ts`, `roleAccess.ts` and `taskActionAccess.ts`
  - existing backend `requirePermission` calls and intentional no-explicit-permission read surfaces
- Define whether header-based demo auth continues for the next governance horizon or whether a later gateway/auth/JWT target phase should be proposed.
- Produce a durable docs artifact, expected as `docs/harness/17-auth-gateway-permission-boundary.md`.

Why this is the next bounded step:

- Phase 012 clarified config-store authority but left D008 open: header-based demo auth and role-access headers remain transition permission inputs.
- Report, market, risk, strategy, config and route-migration readiness artifacts all cite auth/gateway or role authority as a blocker before extraction or route reshaping.
- Permission authority is earlier in the state-machine order than route migration, service extraction, projection split, Kafka redesign or new features.
- A docs-only boundary phase can reduce authority and contract ambiguity without changing runtime behavior.

Expected phase shape:

- Docs-only architecture/governance work by default.
- No production auth implementation.
- No gateway, JWT, session, login, user-service, auth-service, role DB or route proxy work.
- No permission behavior change.
- No URL, frontend route, API, DTO/VO/entity, database, Redis, Kafka, config file, Python payload or business behavior change.
- No reclassification of header-based demo auth as production security.

## Fallback Candidate

Config-store schema/versioning and audit/rollback readiness.

Fallback condition:

- Use this if the user rejects auth/gateway permission authority work and wants to continue acting on Phase 012 readiness gates first.

Bounded fallback goal:

- Define future migration prerequisites for config schema/versioning, single-writer rules, audit retention, rollback, Java/Python reader compatibility and prompt-template rollout.
- Keep JSON config and prompt templates as the current runtime stores.
- Do not select or implement DB, Nacos or hybrid migration unless a later Window 0 decision and human approval explicitly choose that target.

Why it is not primary:

- Phase 012 already selected JSON config and prompt files as the next-governance-horizon runtime transition stores.
- Auth/gateway permission authority remains the more immediate unresolved authority boundary because current command permission behavior depends on headers and role config.
- Config migration readiness can be sequenced after permission authority is made explicit, especially because role-access config interacts with both config and auth.

## Why Other Phases Are Not Selected

- Phase 001 through Phase 012 are completed and frozen.
- Legacy route migration is deferred because Phase 006 intentionally froze current URLs. Migration requires explicit breaking-change or compatibility approval and an updated contract inventory.
- Report extraction or route migration is deferred because Phase 009 produced readiness documentation only. Report ownership movement still depends on route, auth, projection and contract decisions.
- Market-service or data-ingest-service extraction is deferred because Phase 010 produced readiness documentation only. Event source config, ingest history, source preview/diagnose, mock ingest, CNINFO proxy, auth and route blockers remain.
- Risk-service, strategy-service or projection-split planning is deferred because Phase 011 produced readiness documentation only. Shared projection, generated-event, auth, route, Redis and contract blockers remain.
- Generic eval/static guard expansion is deferred by decision order. It should attach to a bounded authority or contract phase instead of replacing permission authority work.
- Direct permanent modular-monolith declaration is too far because the transition host is explicitly not final architecture.
- New product features and new agent work remain ineligible while authority, contract and transition-host lifecycle debt remains open.

## Window 1 Must Define

Window 1 must turn the primary candidate into an implementation-ready handoff and define:

- Exact Phase 013 scope and whether `docs/harness/17-auth-gateway-permission-boundary.md` is the required durable artifact.
- Current permission authority objects and transition inputs:
  - `role-access-configs.json`
  - request headers `X-User-Id` and `X-User-Role`
  - frontend local selected role and role cache
  - backend request user context
- Current backend permission service inventory:
  - `RoleAccessConfigService` in `ai-orchestration-service`
  - `TaskRoleAccessService` in `research-task-service`
  - `SecurityUtils.currentUserRole()`
  - existing `requirePermission` call sites and intentional no-explicit-permission surfaces from Phase 006 inventories
- Current frontend permission consumers:
  - `auth.ts`
  - `requestHeaders.ts`
  - `roleAccess.ts`
  - `taskActionAccess.ts`
  - route/menu visibility and command button gating
- Stable API and behavior contracts that must remain unchanged, including config, report review, market create/import/sync, strategy commands and task-create permission behavior.
- Whether Phase 013 only records a permission boundary or also selects a future gateway/auth/JWT target for a later phase.
- Belongs, authority, contract, transition-lifetime and behavior acceptance gates.
- Verification commands appropriate for docs-only work, such as read-only inventories of permission call sites, role config readers, frontend header utilities and existing Phase 006/007 guards.
- Stop rules if Window 2 discovers that the phase requires gateway/auth implementation, permission behavior changes, route changes, config mutation, frontend behavior changes, Java/Python changes, database changes, JWT/session work or business behavior changes.

Default constraints Window 1 must carry forward:

- No breaking changes.
- Keep URL paths stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Expected Window 2 type: docs-only by default.

## Explicitly Out Of Scope

- No business code change from Window 0.
- No Window 2 implementation before Window 1 and human approval.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build, deployment or runtime behavior change by default.
- No gateway, auth-service, user-service, JWT, session, login flow, OAuth, SSO or production identity implementation.
- No route migration, route alias, endpoint rename, endpoint deletion, endpoint consolidation or gateway proxy.
- No permission behavior change, permission widening or permission narrowing.
- No config-store migration, DB/Nacos/hybrid adoption, config mutation or role-store migration.
- No service extraction.
- No DTO, VO, entity, mapper, schema, Kafka topic, Kafka payload, Redis key, frontend type or Python payload change.
- No new helper, adapter, bridge, fallback, wrapper, resolver, proxy, compatibility endpoint or temporary auth bridge.
- No reclassification of `ai-orchestration-service`, JSON config files, role-access headers, header-based demo auth, legacy `/api/tasks/*` paths, workbench aggregation or fallback provenance as final architecture.
- No new product feature or new agent work.

## Human Approval Result

User approved the proposed next phase.

- Primary: Phase 013 - Auth/Gateway Permission Authority Boundary.
- Fallback: Config-store schema/versioning and audit/rollback readiness.

Approval constraints:

- No breaking changes.
- URL paths remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Window 1 may start Phase 013 architecture planning only.

Window 0 stops here. Window 1 is allowed to start Phase 013 architecture planning, but no implementation is approved.
