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

Phase 012 produced `docs/harness/16-config-store-decision-boundary.md`, a static config-store decision boundary artifact that applies the governance template to agent config, workflow config, model strategy config, prompt templates, event source config, event auto-trigger config, role access config, config change audit, event ingest history, Java/Python readers, frontend config consumers, role-access/header demo auth, stable config contracts, blockers and future gates. It does not approve config-store migration, config mutation, DB/Nacos/hybrid adoption, gateway/auth, service extraction, route migration, endpoint aliases, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 013 produced `docs/harness/17-auth-gateway-permission-boundary.md`, a static auth/gateway permission boundary artifact that applies the governance template to request headers, backend request context, role-access config, backend permission services, explicit permission checks, intentional no-explicit-permission read surfaces, `research-task-service` task-create permission behavior, frontend role/header/menu/action consumers, stable permission contracts, blockers and future gates. It does not approve gateway/auth/JWT implementation, auth-service, user-service, role-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 014 produced `docs/harness/18-production-auth-gateway-target-scope.md`, a static production auth/gateway target-scope artifact that scopes future identity authority through a backend-owned ingress/auth boundary, identifies gateway/JWT as the preferred future target shape, requires future production role authority to be backend-owned, preserves demo headers as local/demo compatibility inputs, and records service-to-service propagation, audit identity, route migration and config/role-store dependencies as future requirements. It does not approve gateway/auth/JWT implementation, auth-service, user-service, role-service, role-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 015 produced `docs/harness/19-production-identity-issuer-validator-boundary.md`, a static production identity issuer/validator boundary artifact that selects backend-owned ingress/gateway JWT validation as the preferred future validator placement, defers the concrete production identity issuer, user profile source and production role authority, preserves demo headers as local/demo compatibility inputs, and records token/session, service-principal, service-to-service identity handoff, audit identity, gateway compatibility, route migration compatibility and rollback readiness gates. It does not approve gateway/auth/JWT implementation, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, role-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 016 produced `docs/harness/20-production-identity-issuer-boundary.md`, a static production identity issuer boundary artifact that selects external IdP or enterprise directory as the preferred future production identity issuer direction, preserves backend-owned ingress/gateway JWT validation as the preferred future validator placement, defers concrete issuer vendor/product, token/session semantics, claim mapping, user profile source and production role authority, preserves demo headers as local/demo compatibility inputs, and records role-authority, gateway/JWT, service-principal, service-to-service identity handoff, audit identity, route migration, config-store and role-store dependencies as future requirements. It does not approve external IdP integration, gateway/auth/JWT implementation, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 017 produced `docs/harness/21-production-role-authority-boundary.md`, a static production role authority boundary artifact that selects backend-owned application role authority as the preferred future direction for production role assignment, role-permission mapping and menu mapping, keeps external IdP or enterprise directory groups/claims as future inputs only pending later approved mapping, preserves `role-access-configs.json` as the current transition role/menu/permission input, and records user profile, token/session, claim mapping, service-principal, service-to-service role handoff, audit identity, route migration, config-store and role-store dependencies as future requirements. It does not approve concrete role authority host creation, external group/claim mapping, gateway/auth/JWT implementation, external IdP integration, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

Phase 018 produced `docs/harness/22-remaining-governance-closure.md`, a static remaining-governance closure artifact that consolidates future-only or deferred gates for concrete role authority host family and mapping, user profile source, service-to-service propagation, audit identity semantics, gateway/JWT prerequisites, demo-header compatibility, config-store and role-store migration readiness, route migration readiness and future implementation sequencing. It does not approve gateway/auth/JWT implementation, external IdP integration, directory integration, concrete role authority host creation, user profile source implementation, service-to-service propagation implementation, audit field changes, route migration, endpoint aliases, permission behavior change, config mutation, role-store migration, config-store migration, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanence or behavior change.

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
9. Completed in Phase 012 for the config-store boundary: Config-specific belongs, authority objects, prompt-template storage, role-access/header demo auth boundaries, config audit, ingest history, Java/Python readers, frontend config consumers, stable config contracts, blockers and readiness gates are documented.
10. Completed in Phase 013 for the auth/gateway permission boundary: Request headers, backend request context, role-access config, backend permission services, explicit permission checks, intentional no-explicit-permission read surfaces, task-create permission behavior, frontend permission consumers, stable permission contracts, blockers and readiness gates are documented.
11. Completed in Phase 014 for production auth/gateway target scoping: Future identity authority direction, future role authority direction, demo-header compatibility rules, service-to-service propagation requirements, audit identity semantics and route/config/role-store dependencies are documented.
12. Completed in Phase 015 for production identity issuer/validator scoping: Future validator placement, issuer deferral, user profile deferral, token/session readiness, service-principal requirements, service-to-service identity handoff requirements, audit identity requirements, gateway compatibility, route migration compatibility and rollback constraints are documented.
13. Completed in Phase 016 for production identity issuer selection: External IdP or enterprise directory is selected as the preferred future production identity issuer direction, while concrete vendor/product, token/session semantics, claim mapping, user profile source and production role authority remain deferred.
14. Completed in Phase 017 for production role authority selection: Backend-owned application role authority is selected as the preferred future role direction, while concrete role authority host, external group/claim mapping, user profile source, role-store migration and config-store migration remain deferred.
15. Completed in Phase 018 for remaining governance closure: Role-host, profile-source, service-propagation, audit-identity, gateway/JWT, demo-header compatibility, config-store/role-store migration, route migration and later implementation sequencing gates are consolidated as future-only or deferred requirements.
16. Pending: Use the Phase 008 inventory and Phase 009/010/011/012/013 readiness artifacts plus the Phase 014 target-scope artifact, Phase 015 identity issuer/validator artifact, Phase 016 identity issuer artifact, Phase 017 role authority artifact and Phase 018 remaining-governance closure artifact to decide whether to extract independent microservices, split projection, config or permission ownership, keep modular-monolith permanence, or sequence gateway/auth, data-ingest, config-store migration, role-store migration, Kafka and route migration work through later Window 0 decisions and human approval.

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
- Treating Phase 012 as approval for config-store migration, config mutation, DB/Nacos/hybrid adoption, config-service extraction, config route migration, endpoint aliases, gateway/auth implementation, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 013 as approval for gateway/auth/JWT implementation, auth-service, user-service, role-store migration, permission behavior change, route migration, endpoint aliases, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 014 as approval for gateway/auth/JWT implementation, auth-service, user-service, role-service, role-store migration, permission behavior change, route migration, endpoint aliases, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 015 as approval for gateway/auth/JWT implementation, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, role-store migration, permission behavior change, route migration, endpoint aliases, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 016 as approval for external IdP integration, gateway/auth/JWT implementation, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, permission behavior change, route migration, endpoint aliases, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 017 as approval for concrete role authority host creation, external group/claim mapping, gateway/auth/JWT implementation, external IdP integration, auth-service, user-service, role-service, login/session, OAuth, SSO, role-store migration, config-store migration, permission behavior change, route migration, endpoint aliases, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.
- Treating Phase 018 as approval for gateway/auth/JWT implementation, external IdP integration, directory integration, concrete role authority host creation, user profile source implementation, service-to-service propagation implementation, audit field changes, demo-header retirement, role-store migration, config-store migration, route migration, endpoint aliases, permission behavior change, config mutation, service extraction, frontend/Python reshaping, Kafka/database/Redis changes or permanent modular-monolith architecture.

## T2: JSON Config as Runtime Configuration Store

Current:

Agent, workflow, model strategy, event source, auto trigger and role access configs are JSON files under `ai-config`.

Prompt templates are files under `prompt-templates`.

Config change audit and event ingest history are file-backed transition facts under `ai-config`.

Phase 012 produced `docs/harness/16-config-store-decision-boundary.md` and selected the conservative next-governance-horizon decision: JSON config files and prompt template files remain the current runtime transition stores. DB, Nacos and hybrid stores are deferred future migration targets only, requiring later Window 0 selection and human approval.

Phase 013 recorded `role-access-configs.json` as the current role/menu/permission config input under the Phase 012 JSON transition-store policy. It did not approve role-store migration, role DB adoption, config mutation or config-store migration.

Phase 014 kept `role-access-configs.json` as the current transition role/menu/permission input while scoping future production role authority as backend-owned. It did not approve role-store migration, role DB adoption, config mutation or config-store migration.

Phase 015 deferred production role authority while selecting only the future production identity validator placement. It kept `role-access-configs.json` as the current transition role/menu/permission input and did not approve role-store migration, role DB adoption, config mutation or config-store migration.

Phase 016 selected external IdP or enterprise directory as the preferred future production identity issuer direction, but it did not select production role authority. `role-access-configs.json` remains the current transition role/menu/permission input and no role-store migration, role DB adoption, config mutation or config-store migration is approved.

Phase 017 selected backend-owned application role authority as the preferred future production role direction, but it did not select a concrete role authority host or migration path. `role-access-configs.json` remains the current transition role/menu/permission input and no role-store migration, role DB adoption, config mutation or config-store migration is approved.

Phase 018 consolidated config-store and role-store migration readiness gates, including target host selection, schema/versioning, single-writer rules, audit retention, rollback, Java/Python/frontend compatibility, role/menu/permission compatibility and migration validation. It did not select a target store, approve dual-read/dual-write, mutate JSON config, create a role store or config store, or approve migration.

Allowed because:

- It keeps demo and local iteration lightweight.
- Both Java and Python already read these files.
- Current frontend, Java service and Python reader contracts depend on these file-backed stores.

Exit criteria:

1. Completed in Phase 012 for current governance docs: config belongs, authority objects, Java/Python readers, frontend consumers, role-access/header demo auth, config audit, ingest history and stable config contracts are documented.
2. Completed in Phase 012 for the next governance horizon: JSON config files and prompt template files remain current runtime transition stores; DB, Nacos and hybrid are deferred future targets only.
3. Completed in Phase 013 for role-access interaction: `role-access-configs.json` is documented as the current permission config input, not final role-store architecture.
4. Completed in Phase 014 for role-authority interaction: future production role authority must be backend-owned, while `role-access-configs.json` remains the current transition input until a later approved migration.
5. Completed in Phase 015 for identity interaction: backend-owned ingress/gateway JWT validation is the preferred future validator placement, but role authority remains deferred and `role-access-configs.json` remains the current transition input.
6. Completed in Phase 016 for issuer interaction: external IdP or enterprise directory is the preferred future issuer direction, but role authority remains deferred and `role-access-configs.json` remains the current transition input.
7. Completed in Phase 017 for role-authority interaction: backend-owned application role authority is the preferred future role direction, but the concrete host and migration path remain deferred and `role-access-configs.json` remains the current transition input.
8. Completed in Phase 018 for remaining-governance docs: schema/versioning, single-writer, audit retention, rollback, Java/Python/frontend compatibility, role/menu/permission compatibility and migration validation gates are consolidated as future requirements.
9. Pending: Choose any DB, Nacos, hybrid, role-store or config-service migration target only through a later Window 0 decision and human approval.
10. Pending: Implement any config-store or role-store migration only after target selection, compatibility rules, validation and rollback are approved by a later phase.

Forbidden:

- Silent config mutation without audit.
- Letting frontend defaults become config truth.
- Treating JSON config files or prompt template files as final config architecture.
- Treating DB, Nacos or hybrid storage as current runtime authority before an approved migration phase.
- Starting config-store migration, dual-write, migration runner, rollback runner, config-service extraction, route migration or gateway/auth work from Phase 012 alone.

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

Phase 012 documented `role-access-configs.json`, request headers, frontend role utilities and `TaskRoleAccessService` as transition permission inputs/readers under the JSON config-store boundary.

Phase 013 produced `docs/harness/17-auth-gateway-permission-boundary.md` and selected the conservative next-governance-horizon decision: header-based demo auth remains the current transition permission input mechanism, `role-access-configs.json` remains the current role/menu/permission config input, backend explicit `requirePermission` checks remain current enforcement points for checked endpoints, and frontend route/menu/action gating remains UI affordance only.

Phase 014 produced `docs/harness/18-production-auth-gateway-target-scope.md` and selected a future-only target direction: production identity should be accepted through a backend-owned ingress/auth boundary, gateway/JWT is the preferred target shape, production role authority must be backend-owned, and demo headers remain local/demo compatibility inputs until a later approved compatibility or retirement phase changes them.

Phase 015 produced `docs/harness/19-production-identity-issuer-validator-boundary.md` and selected backend-owned ingress/gateway JWT validation as the preferred future validator placement. The concrete production identity issuer, user profile source and production role authority remain deferred, and demo headers remain local/demo compatibility inputs only.

Phase 016 produced `docs/harness/20-production-identity-issuer-boundary.md` and selected external IdP or enterprise directory as the preferred future production identity issuer direction. Concrete vendor/product, token/session semantics, claim mapping, user profile source and production role authority remain deferred, and demo headers remain local/demo compatibility inputs only.

Phase 017 produced `docs/harness/21-production-role-authority-boundary.md` and selected backend-owned application role authority as the preferred future production role direction. Concrete role authority host, external group/claim mapping, user profile source, role-store migration and config-store migration remain deferred, and demo headers remain local/demo compatibility inputs only.

Phase 018 produced `docs/harness/22-remaining-governance-closure.md` and consolidated the future gateway/JWT prerequisites, demo-header compatibility policy shape, user profile source boundary, service-principal semantics, service-to-service identity/role handoff, audit identity semantics, route migration gates and role/config-store migration gates. Demo headers remain local/demo compatibility inputs only, and no gateway/auth/JWT, demo-header retirement, service-to-service propagation or audit identity implementation is approved.

Allowed because:

- It keeps current demo flow simple.

Exit criteria:

1. Completed in Phase 013 for current governance docs: request headers, backend request context, role-access config, backend permission services, explicit permission checks, intentional no-explicit-permission read surfaces, task-create permission behavior, frontend permission consumers, stable permission contracts, blockers and readiness gates are documented.
2. Completed in Phase 014 for target scoping: future production identity authority direction, future production role authority direction, service-to-service propagation requirements, demo-header compatibility rules and audit identity semantics are documented.
3. Completed in Phase 015 for issuer/validator boundary: backend-owned ingress/gateway JWT validation is selected as preferred future validator placement, while concrete issuer, user profile source and production role authority remain deferred.
4. Completed in Phase 016 for issuer direction: external IdP or enterprise directory is selected as the preferred future production identity issuer direction, future-only.
5. Completed in Phase 017 for role-authority direction: backend-owned application role authority is selected as the preferred future production role direction, future-only.
6. Completed in Phase 018 for remaining-governance docs: Gateway/JWT prerequisites, demo-header compatibility policy shape, user profile source boundary, service-principal semantics, service-to-service handoff, audit identity semantics, route migration gates and role/config-store migration gates are consolidated as future-only or deferred requirements.
7. Pending: Decide whether to implement gateway/auth/JWT, auth-service, user-service, role-service or session service.
8. Pending: Select the concrete external issuer vendor/product and token/session/claim semantics.
9. Pending: Select the user profile source.
10. Pending: Select the concrete production role authority host, external group/claim mapping contract and any role-store migration path.
11. Pending: Implement service-to-service user/role propagation, demo-header compatibility or retirement, audit identity semantics and frontend/backend enforcement interaction only after later Window 0 selection and human approval.
12. Pending: Add real login/session flow if production-grade access is required and explicitly approved.

Forbidden:

- Treating header-based role selection as production security.
- Treating request headers, frontend localStorage, frontend route/menu/action gating or role-access cache as production permission authority.
- Treating `role-access-configs.json` as final role-store architecture.
- Implementing gateway/auth/JWT, auth-service, user-service, role DB, login/session, demo-header retirement or permission behavior changes from Phase 013 alone.
- Implementing gateway/auth/JWT, auth-service, user-service, role-service, role DB, login/session, OAuth, SSO, external IdP integration, demo-header retirement, service-to-service propagation or permission behavior changes from Phase 014 alone.
- Implementing gateway/auth/JWT, auth-service, user-service, role-service, role DB, login/session, OAuth, SSO, external IdP integration, demo-header retirement, service-to-service propagation, issuer integration, token/session behavior, audit field changes or permission behavior changes from Phase 015 alone.
- Implementing gateway/auth/JWT, auth-service, user-service, role-service, role DB, login/session, OAuth, SSO, external IdP integration, demo-header retirement, service-to-service propagation, issuer integration, token/session behavior, claim mapping, user profile integration, audit field changes or permission behavior changes from Phase 016 alone.
- Implementing concrete role authority host creation, external group/claim mapping, gateway/auth/JWT, auth-service, user-service, role-service, role DB, login/session, OAuth, SSO, external IdP integration, demo-header retirement, service-to-service propagation, issuer integration, token/session behavior, user profile integration, role-store migration, config-store migration, audit field changes or permission behavior changes from Phase 017 alone.
- Implementing gateway/auth/JWT, auth-service, user-service, role-service, role DB, login/session, OAuth, SSO, external IdP integration, directory integration, concrete role authority host creation, user profile integration, demo-header retirement, service-to-service propagation, token/session behavior, claim mapping, route migration, role-store migration, config-store migration, audit field changes or permission behavior changes from Phase 018 alone.
