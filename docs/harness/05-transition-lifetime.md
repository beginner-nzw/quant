# Transition Lifetime

本文件登记当前允许存在的过渡态、退出条件和禁止事项。

## T1: ai-orchestration-service as Multi-Domain Transition Host

Current:

`ai-orchestration-service` hosts task read-model, AI control, reports, risks, strategy signals, market events, audit dashboard and config APIs.

Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controller classes inside this same transition host. URL paths were intentionally preserved.

Phase 002 split non-task read paths out of `TaskQueryServiceImpl` into internal domain query services inside this same transition host. External contracts and legacy URL paths were intentionally preserved.

Phase 006 froze the approved legacy non-task `/api/tasks/*` contract inventory with backend tests. URL paths were intentionally preserved, and the legacy namespace remains transitional rather than final architecture.

Phase 005 selected continuing as a modular monolith inside `ai-orchestration-service` for the next governance horizon. This keeps the current transition host in place as policy, but it does not make the host final architecture and does not approve extraction, route migration or breaking contract changes.

Phase 008 produced `docs/harness/12-transition-host-exit-criteria.md`, a static per-domain inventory of current transition-host responsibilities, SoT/read-model placement, command surfaces, legacy route dependencies, guardrails, extraction blockers, exit criteria and readiness gates for report, market, risk, strategy, audit, config and workbench. It does not approve extraction, route migration, permanence, gateway/auth, config-store migration, data-ingest split or behavior change.

Phase 009 produced `docs/harness/13-report-boundary-readiness.md`, a static report-domain readiness artifact that applies the Phase 008 template to report facts, evidence, versions, review commands, review audit, AI projection dependency, fallback provenance metadata and frontend report consumers. It does not approve report-service extraction, route migration, endpoint aliases, gateway/auth, config-store migration, frontend/Python reshaping, Kafka/database changes, permanence or behavior change.

Phase 010 produced `docs/harness/14-market-data-ingest-boundary-readiness.md`, a static market/data-ingest readiness artifact that applies the Phase 008 template to market facts, relations, analysis, event source config, ingest history, market read models and commands, source sync, source preview/diagnose, CNINFO proxy, mock/demo ingest, market intelligence, Kafka `market.event.standardized`, auto-trigger context, frontend market consumers and Python market context/fallback provenance. It does not approve market-service extraction, data-ingest-service extraction, route migration, endpoint aliases, gateway/auth, config-store migration, frontend/Python reshaping, Kafka/database changes, permanence or behavior change.

Phase 011 produced `docs/harness/15-risk-strategy-projection-boundary-readiness.md`, a static risk/strategy projection ownership readiness artifact that applies the Phase 008 template to risk warning facts, risk details, strategy signal facts, strategy factors, risk/strategy read models, strategy commands, `AiResultDomainProjectionService`, generated `risk.warning.generated` and `strategy.signal.generated` publication, frontend risk/strategy consumers and Python risk/strategy context/fallback provenance. It does not approve risk-service extraction, strategy-service extraction, projection splitting, route migration, endpoint aliases, gateway/auth, config-store migration, frontend/Python reshaping, Redis changes, Kafka/database changes, permanence or behavior change.

Allowed because:

- Current project is still in convergence phase.
- Java business domain tables and UI already depend on this service.
- Immediate microservice split would create too much churn before follow-up domain ownership, route, gateway/auth, config-store and data-ingest decisions are selected by Window 0 and approved by the user.

Exit criteria:

1. Completed in Phase 001: `TaskQueryController` split into domain-specific controllers inside the same service.
2. Completed in Phase 002: `TaskQueryServiceImpl` split into internal domain query services.
3. Completed in Phase 006 for legacy paths: non-task `/api/tasks/*` contracts are documented and guarded for path, method, owner, response envelope, binding and permission drift.
4. Completed in Phase 005 for the current governance horizon: continue as modular monolith without declaring final architecture.
5. Completed in Phase 008: Each in-scope domain has clear SoT, read-model, command surface, route dependency, guardrail, extraction blocker, exit criteria and readiness-gate inventory.
6. Completed in Phase 009 for the report domain: Report-specific belongs, authority objects, read-model and command surfaces, version/evidence/review-audit records, AI projection dependency, fallback provenance, frontend consumers, blockers and readiness gates are documented.
7. Completed in Phase 010 for the market/data-ingest boundary: Market/data-ingest-specific belongs, authority objects, event source config, ingest history, read-model and command surfaces, mock/demo ingest, source sync/preview/diagnose, CNINFO proxy, market intelligence, Kafka context, auto-trigger context, frontend consumers, Python market context/fallback provenance, blockers and readiness gates are documented.
8. Completed in Phase 011 for the risk/strategy boundary: Risk/strategy-specific belongs, authority objects, read-model and command surfaces, shared projection dependency, generated-event publication, frontend consumers, Python risk/strategy context/fallback provenance, blockers and readiness gates are documented.
9. Pending: Use the Phase 008 inventory and Phase 009/010/011 readiness artifacts to decide whether to extract independent microservices, split projection ownership, keep modular-monolith permanence, or sequence gateway/auth, data-ingest, config-store, Kafka and route migration work through later Window 0 decisions and human approval.

Forbidden:

- Adding new unrelated domains into `TaskQueryController`.
- Adding new unrelated domains into the Phase 001 split controllers without updating the contract map and phase handoff.
- Adding, moving, deleting or aliasing legacy non-task `/api/tasks/*` endpoints without updating the Phase 006 contract inventory through an approved phase.
- Treating the current service layout as final architecture.
- Treating Phase 005 as approval for service extraction, route migration, gateway/auth implementation or permanent modular-monolith architecture.
- Treating Phase 008 as approval for service extraction, route migration, gateway/auth implementation, config-store migration, data-ingest split or permanent modular-monolith architecture.
- Treating Phase 009 as approval for report-service extraction, report route migration, endpoint aliases, gateway/auth implementation, config-store migration, frontend/Python reshaping, Kafka/database changes or permanent modular-monolith architecture.
- Treating Phase 010 as approval for market-service extraction, data-ingest-service extraction, market route migration, endpoint aliases, gateway/auth implementation, config-store migration, frontend/Python reshaping, Kafka/database changes, source adapter/CNINFO proxy redesign or permanent modular-monolith architecture.
- Treating Phase 011 as approval for risk-service extraction, strategy-service extraction, projection splitting, risk/strategy route migration, endpoint aliases, gateway/auth implementation, config-store migration, frontend/Python reshaping, Redis changes, Kafka/database changes or permanent modular-monolith architecture.

## T2: JSON Config as Runtime Configuration Store

Current:

Agent, workflow, model strategy, prompt template, event source, auto trigger and role access configs are JSON files under `ai-config`.

Allowed because:

- It keeps demo and local iteration lightweight.
- Both Java and Python already read these files.

Exit criteria:

1. Config change audit is stable.
2. Config schema is documented.
3. Decide whether target store is DB, Nacos, or hybrid.

Forbidden:

- Silent config mutation without audit.
- Letting frontend defaults become config truth.

## T3: Mock Ingest

Current:

Market event center supports mock ingest.

Phase 010 documented mock/demo ingest as a transition/demo mechanism. Mock/demo payloads may create market event records only through existing approved commands, and mock/demo source behavior remains non-production and non-authoritative for risk, strategy and report facts by itself.

Allowed because:

- Useful for demo and local testing.
- Real data ingestion service is not implemented yet.

Exit criteria:

1. Real source sync path is stable.
2. Completed in Phase 010 for governance docs: mock/demo ingest, source sync, source preview/diagnose, CNINFO proxy, event source config and ingest history are classified as transition responsibilities with readiness gates.
3. Pending: Data ingest ownership is decided.
4. Pending: Mock ingest is marked demo/test only in UI and user-facing docs if a later approved phase changes that surface.

Forbidden:

- Treating mock ingest as production data source.
- Using mock source as risk/strategy authority.

## T4: Python Rule Fallback

Current:

Python agents build fallback results when model calls fail, are disabled, or return incomplete output.

Phase 003 documented Java-side display hydration boundaries but did not change Python fallback execution or metadata.

Phase 004 added in-scope fallback provenance for planner, intent, financial, risk, report and market fallback paths using existing Python dictionaries and `reportMeta.contextSnapshot` map metadata. Java production projection remains unchanged and must not use fallback provenance as business authority.

Phase 007 documented current frontend fallback provenance consumers as display/audit metadata only and added a focused static guard so `contextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related provenance fields do not feed frontend command or review authority.

Phase 010 documented Python market context, backend overlays, `dataSource: fallback`, fallback market snapshots, `liveMarketEvents`, `marketIntelligence` and market fallback provenance as execution/display/provenance context only, not market data SoT.

Phase 011 documented Python risk/strategy context, `riskWarnings`, `strategySignals`, `latestRiskWarningSummary`, `latestStrategySignalSummary`, risk fallback output and fallback provenance as execution/display/provenance context only, not risk or strategy data SoT.

Allowed because:

- It keeps asynchronous workflows terminal and auditable.
- It supports local demo without guaranteed model availability.

Exit criteria:

1. Completed in Phase 004 for in-scope fallback paths: fallback output carries fallback reason or equivalent audit signal.
2. Completed in Phase 004 for existing result metadata: Java result projection preserves evidence of fallback path through raw/report metadata without using it as authority.
3. Completed in Phase 004 for current Python tests: focused regression coverage detects missing fallback reason or provenance metadata.
4. Completed in Phase 007 for current frontend consumers: fallback provenance is documented and guarded as display/audit metadata only.
5. Completed in Phase 010 for market context docs: Python market fallback and backend overlay surfaces are documented as non-authoritative context.
6. Completed in Phase 011 for risk/strategy context docs: Python risk/strategy fallback, backend client context and generated result surfaces are documented as non-authoritative context.
7. Pending: Any future fallback surface must carry equivalent non-authoritative provenance before it is accepted as transition behavior.

Forbidden:

- Returning fallback as if it were model-generated truth.
- Using fallback financial snapshots as real financial data.

## T5: Workbench Aggregation

Current:

Research workbench aggregates task, report, risk, strategy and market event information.

Phase 002 moved workbench aggregation to `ResearchWorkbenchQueryServiceImpl` and removed copied risk, strategy, report center and market-intelligence read-model entrypoints from the workbench host after review.

Phase 003 documented the Java backend workbench service as display-only aggregation and added source-level tests so backend command/projection code cannot depend on workbench output as authority and workbench aggregation cannot write domain facts.

Phase 007 documented current frontend workbench consumers as display-only aggregation and added a focused static guard so workbench output does not call retry, cancel, report review, strategy, market or config command APIs.

Allowed because:

- It is a useful product surface.

Exit criteria:

1. Completed in Phase 003: Workbench clearly documented as display-only in contract-level tests/comments.
2. Completed in Phase 003: No backend command/projection depends on workbench output as SoT, guarded by regression tests.
3. Completed in Phase 007: Current frontend consumers use workbench output only for display, navigation and existing task-create source-context prefill.
4. Pending: No Python workflow uses workbench as the only authoritative source for domain facts.

Forbidden:

- Promoting workbench fields to domain truth.

## T6: Header-Based Demo Auth

Current:

Frontend sets `X-User-Id` and `X-User-Role`; backend builds UserContext from headers.

Allowed because:

- It keeps current demo flow simple.

Exit criteria:

1. Decide whether to implement auth-service/JWT.
2. Decide role storage SoT.
3. Add real login/session flow if production-grade access is required.

Forbidden:

- Treating header-based role selection as production security.
