# Transition Lifetime

本文件登记当前允许存在的过渡态、退出条件和禁止事项。

## T1: ai-orchestration-service as Multi-Domain Transition Host

Current:

`ai-orchestration-service` hosts task read-model, AI control, reports, risks, strategy signals, market events, audit dashboard and config APIs.

Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controller classes inside this same transition host. URL paths were intentionally preserved.

Phase 002 split non-task read paths out of `TaskQueryServiceImpl` into internal domain query services inside this same transition host. External contracts and legacy URL paths were intentionally preserved.

Allowed because:

- Current project is still in convergence phase.
- Java business domain tables and UI already depend on this service.
- Immediate microservice split would create too much churn before authority and contract are frozen.

Exit criteria:

1. Completed in Phase 001: `TaskQueryController` split into domain-specific controllers inside the same service.
2. Completed in Phase 002: `TaskQueryServiceImpl` split into internal domain query services.
3. Pending: Contract map stable for task/report/risk/strategy/market/audit/config.
4. Pending: Each domain has clear SoT and read-model.
5. Pending: Only then decide whether to extract independent microservices.

Forbidden:

- Adding new unrelated domains into `TaskQueryController`.
- Adding new unrelated domains into the Phase 001 split controllers without updating the contract map and phase handoff.
- Treating the current service layout as final architecture.

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

Allowed because:

- Useful for demo and local testing.
- Real data ingestion service is not implemented yet.

Exit criteria:

1. Real source sync path is stable.
2. Data ingest ownership is decided.
3. Mock ingest is marked demo/test only in UI and docs.

Forbidden:

- Treating mock ingest as production data source.
- Using mock source as risk/strategy authority.

## T4: Python Rule Fallback

Current:

Python agents build fallback results when model calls fail, are disabled, or return incomplete output.

Allowed because:

- It keeps asynchronous workflows terminal and auditable.
- It supports local demo without guaranteed model availability.

Exit criteria:

1. Every fallback output carries fallback reason or equivalent audit signal.
2. Java result projection preserves evidence of fallback path.
3. Eval checklist can detect missing fallback reason.

Forbidden:

- Returning fallback as if it were model-generated truth.
- Using fallback financial snapshots as real financial data.

## T5: Workbench Aggregation

Current:

Research workbench aggregates task, report, risk, strategy and market event information.

Phase 002 moved workbench aggregation to `ResearchWorkbenchQueryServiceImpl` and removed copied risk, strategy, report center and market-intelligence read-model entrypoints from the workbench host after review.

Allowed because:

- It is a useful product surface.

Exit criteria:

1. Pending: Workbench clearly documented as display-only in contract-level tests/comments.
2. Pending: No backend command depends on workbench output as SoT, guarded by regression tests.
3. Pending: No Python workflow uses workbench as the only authoritative source for domain facts.

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
