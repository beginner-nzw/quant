# Full Project Completion Roadmap

## Status

This roadmap records the remaining phases needed to complete the full project, not just an MVP.

It is based on the current code shape and harness state after Phase 018:

- Java business services currently implemented: `research-task-service` and `ai-orchestration-service`.
- Frontend currently implemented: Vue pages for task center, task creation/detail/report, dashboard, market event/intelligence, research workbench, risk warning, strategy signal, report center/review workbenches, audit compliance and model/agent config.
- Python AI engine currently implemented: linear LangGraph workflow with planner, intent, evidence collection, financial analysis, risk review and report generation agents.
- Middleware compose currently includes MySQL, Redis, Kafka and Zookeeper only.
- Config currently uses JSON files under `quant-ai-platform/ai-config` and prompt templates under `quant-ai-platform/prompt-templates`.
- Harness governance Phase 018 has already consolidated the remaining docs-only governance closure.

This roadmap does not approve implementation by itself. Each phase still needs Window 0 selection, Window 1 planning, user approval, implementation, review and handoff unless the user explicitly changes the process.

## Harness Governance Remainder

There should be no more repeated standalone harness-governance phases.

If a cleanup is needed, keep it to one phase only:

| Phase | Name | Goal | Notes |
| --- | --- | --- | --- |
| Optional H1 | Harness Roadmap And State Sync | Align `current-state`, backlog and debt register with this full-project roadmap after user approval. | Optional. No business code. This is the only acceptable remaining harness-governance cleanup phase. Skip if the user accepts this roadmap as advisory only. |

All phases below are product, platform, AI, data, security, deployment or extraction work. Handoff files may still be written as process records, but documentation is not the phase goal.

## Completion Target

Full project completion means:

- Production-grade identity, role, permission and audit boundary.
- Real data ingestion and market event pipeline, not only mock/demo ingest.
- AI workflow with the planned domain agents, conditional routing, checkpoint/recovery and human review loops.
- Report, risk, strategy, audit and market domains have clear service ownership and stable contracts.
- Config and role stores are no longer uncontrolled JSON transition files unless deliberately accepted as a final local/demo-only mode.
- Legacy `/api/tasks/*` non-task routes are migrated or compatibility-wrapped with approved contracts.
- Deployment includes gateway, service discovery/config, observability, health checks and repeatable startup.
- Frontend supports complete production workflows, not only display aggregation.
- Tests cover backend contracts, frontend flows, Python workflow behavior, async messaging and core e2e paths.

## Current Gap Summary

| Area | Current state | Full-project gap |
| --- | --- | --- |
| Gateway/auth | Header-based demo auth with `X-User-Id` and `X-User-Role`; no gateway/JWT runtime. | Implement production identity ingress, JWT validation, demo compatibility, failure behavior and rollback. |
| Role/profile | `role-access-configs.json` is transition input; role authority/profile source not implemented. | Implement backend-owned role authority, profile source and role/profile auditability. |
| Service-to-service identity | AI callbacks and async paths do not carry production service-principal semantics. | Define and implement service principal, delegated actor, original actor and system actor propagation. |
| Config | JSON files and prompt files are runtime transition stores. | Decide and implement config-store/role-store migration or explicitly final local-mode boundaries. |
| Routes/contracts | Non-task domains still expose legacy `/api/tasks/*` routes. | Migrate or compatibility-wrap domain routes after auth and ownership are stable. |
| Data ingest | Market source sync/mock/CNINFO proxy live in `ai-orchestration-service`; no independent data-ingest service. | Implement production-grade source adapters, scheduler, normalization, provenance, retry/deadletter and ownership. |
| AI workflow | Linear LangGraph workflow; planned agents missing. | Add event extraction, industry research, strategy reasoning, audit/compliance, conditional routing, checkpoint/retry and human review nodes. |
| Domain ownership | Report, market, risk, strategy, audit, config and workbench are still in `ai-orchestration-service` transition host. | Extract or permanently modularize domains with contracts, events and data ownership. |
| Frontend | Many pages exist, but workflows are still tied to transition contracts and demo auth. | Add production auth UX, role-aware flows, operational dashboards, data-source operations, review/correction loops and e2e states. |
| Deployment | Compose has MySQL, Redis, Kafka, Zookeeper only. | Add gateway, config/service discovery, Java services, Python workers, observability, health checks and deployment profiles. |
| Tests/eval | Maven tests, Python unittest and focused frontend guard exist; frontend/e2e coverage limited. | Add broader unit, integration, contract, workflow, e2e, load and failure-mode coverage. |

## Proposed Remaining Phases

Estimated remaining full-project phases: 36 core phases plus the optional single harness sync phase.

The count is intentionally larger than an MVP plan because the target is full completion, including productionization, service ownership, data ingestion, AI workflow depth, frontend completeness and deployment/observability.

### Track A - Security, Identity, Role And Audit

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P019 | Gateway/JWT Implementation Design With Demo-Header Compatibility Policy | Concrete implementation design for backend-owned ingress/JWT validation, demo-header compatibility, failure behavior and rollback. |
| P020 | Gateway/JWT Minimal Runtime Implementation | Add the minimal gateway/JWT validation path while preserving local/demo compatibility and current permission behavior. |
| P021 | Production Role Authority And User Profile Implementation Plan | Select concrete role authority host, role mapping, profile source and migration path. |
| P022 | Role Authority And Profile Minimal Implementation | Implement backend-owned role/profile read authority and compatibility adapter from current role config. |
| P023 | Service-To-Service Identity And Audit Semantics Implementation | Add service principal, original/delegated actor and audit identity propagation for AI callbacks and async dispatch. |
| P024 | Permission Hardening And Negative Authorization Tests | Add permission regression tests, denial behavior, unauthenticated behavior and no-permission-widening checks. |

### Track B - Config, Role Store And Runtime Governance

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P025 | Config-Store And Role-Store Migration Design | Decide DB/Nacos/hybrid target, schema/versioning, single-writer rules and rollback. |
| P026 | Config-Store Minimal Migration | Move agent/workflow/model/prompt/event config read path behind a versioned backend store or approved config service boundary. |
| P027 | Role-Store Migration | Move role/permission/menu mapping from JSON transition input to approved backend-owned store. |
| P028 | Config Audit, Rollback And Compatibility Cutover | Add audited config changes, rollback, migration validation and Java/Python/frontend compatibility checks. |

### Track C - Data Ingestion And Market Intelligence

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P029 | Data-Ingest Service Boundary And Runtime Skeleton | Create or select data-ingest host and move ingest scheduling/source ownership out of transition scope. |
| P030 | Production Market Source Adapters | Implement real source adapters for announcements/news/reports/policy feeds with provenance and failure handling. |
| P031 | Market Event Normalization Pipeline | Standardize raw input into `market_event` facts with dedupe, relation extraction and confidence/provenance. |
| P032 | Ingest Retry, Deadletter And Observability | Add retry, deadletter, source health, ingest metrics and operator-facing diagnostics. |
| P033 | Event Auto-Trigger Productionization | Turn market event auto-trigger into a governed production path with throttling, audit and permissions. |

### Track D - AI Engine And Multi-Agent Workflow

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P034 | Event Extraction Agent | Add event extraction agent and integrate it into configured workflows. |
| P035 | Industry Research Agent | Add industry research agent with source grounding and report contribution contract. |
| P036 | Strategy Reasoning Agent | Add strategy reasoning agent that produces strategy signal candidates with provenance and confidence. |
| P037 | Audit And Compliance Agent | Add audit/compliance agent for policy, evidence and report-review support. |
| P038 | Conditional LangGraph Routing | Upgrade from linear workflow to conditional routing by task type, evidence quality, risk level and review outcome. |
| P039 | Workflow Checkpoint, Resume And Rerun | Add checkpoint/recovery, node rerun, failure resume and explicit workflow state restoration. |
| P040 | Human-In-The-Loop Review Nodes | Add human review/correction nodes for report review, risk escalation and compliance approval. |
| P041 | AI Evaluation Harness | Add task-level eval datasets, expected output checks, hallucination/provenance checks and regression scoring. |

### Track E - Domain Services And Contract Migration

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P042 | Legacy Route Migration Plan And Compatibility Inventory | Plan migration from non-task `/api/tasks/*` to domain routes with compatibility/breaking-change approval. |
| P043 | Report Service Extraction Or Final Modular Boundary | Extract report service or make a final modular boundary with route contracts, events and data ownership. |
| P044 | Risk Service Extraction Or Final Modular Boundary | Extract risk warning ownership or make a final modular boundary with route/contracts/events. |
| P045 | Strategy Service Extraction Or Final Modular Boundary | Extract strategy signal ownership or make a final modular boundary with route/contracts/events. |
| P046 | Market Service Extraction Or Final Modular Boundary | Extract market event ownership or make a final modular boundary aligned with data-ingest. |
| P047 | Audit Service Extraction Or Final Modular Boundary | Establish audit/compliance service ownership, audit query contracts and retention behavior. |
| P048 | Workbench Aggregation Recomposition | Rebuild workbench as a pure aggregation consumer of stable domain contracts. |
| P049 | Legacy Route Cutover | Execute approved route aliases/migration/deprecation and update frontend/API clients safely. |

### Track F - Frontend Product Completion

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P050 | Production Auth Frontend Flow | Add login/session/JWT-aware frontend flow or approved enterprise SSO integration UX. |
| P051 | Role-Aware Navigation And Action Hardening | Make route/menu/action states reflect backend authority and denial behavior without becoming truth. |
| P052 | Data Source Operations UI | Complete source config, sync, diagnose, ingest history, deadletter and retry operations. |
| P053 | AI Workflow Operations UI | Add workflow trace, node rerun, checkpoint resume, human review queues and failure recovery screens. |
| P054 | Report/Risk/Strategy Review UX Completion | Complete review, compare, version diff, evidence trace, approval/rejection and export flows. |
| P055 | Executive And Risk Dashboards | Add production dashboard metrics for tasks, risk exposure, strategy signals, source health and audit. |

### Track G - Deployment, Observability And Reliability

| Phase | Name | Primary outcome |
| --- | --- | --- |
| P056 | Deployment Compose Expansion | Add gateway, Java services, Python engine workers, config/discovery components and profile-based startup. |
| P057 | Observability Baseline | Add structured logs, trace ids, metrics, health checks, dashboards and alert rules. |
| P058 | Resilience And Backpressure | Add rate limiting, circuit breaker, Kafka lag handling, Redis degradation and workflow timeout policies. |
| P059 | End-To-End Test Suite | Add e2e tests for task creation -> AI workflow -> report/risk/strategy -> review/audit. |
| P060 | Load, Failure And Recovery Testing | Add load tests, chaos/failure cases, restart recovery, idempotency and replay tests. |
| P061 | Production Readiness Review And Release Packaging | Final release checklist, deployment docs, demo/prod profiles, seed data, runbooks and acceptance signoff. |

## Suggested Compression

The full completion roadmap has 43 listed phases if the optional harness sync is included. Some can be merged if velocity matters:

- Merge P019 and P020 only if the user accepts runtime auth implementation risk in one phase.
- Merge P021 and P022 only if the role/profile target is simple and no migration is needed.
- Merge P025 through P028 only if JSON config is explicitly accepted as final local-mode storage and no store migration is required.
- Merge P043 through P046 only if the project chooses final modular-monolith boundaries instead of physical service extraction.
- Merge P056 through P061 only for a demo-grade release; do not merge for production-grade release.

Practical full-completion estimate:

- Conservative: 36 to 43 phases.
- Compressed but still serious: 24 to 30 phases.
- Anything below 20 phases means accepting MVP-level scope or leaving production/service-extraction/testing gaps.

## Recommended Next Phase

Recommended next phase:

- P019 - Gateway/JWT Implementation Design With Demo-Header Compatibility Policy.

Reason:

- Harness governance is already closed by Phase 018.
- Header-based demo auth remains the largest production-readiness gap.
- Gateway/JWT compatibility must precede route migration, domain extraction, service-to-service propagation and production frontend auth.

## Non-Goals For Future Planning

- Do not open more standalone harness-governance phases unless the optional single Harness Roadmap And State Sync phase is explicitly approved.
- Do not treat docs handoffs as phase goals after Phase 018.
- Do not add new product features before production identity, role, data, AI workflow and ownership gaps are sequenced.
- Do not claim full project completion after only auth or AI phases; full completion also requires data ingestion, service ownership, frontend operations, deployment, observability and testing.
