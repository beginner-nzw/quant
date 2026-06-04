# Current Harness State

## Bootstrap Status

Bootstrap Harness Window completed the pre-Window-0 setup.

This file is the starting state for Window 0.

## Current Phase

None approved.

Latest frozen phase: Phase 015 - Production Identity Issuer/Validator Selection Boundary.

## Current Phase Status

Phase 015 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-015-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-015-final.md`.

Phase 015 produced the durable production identity issuer/validator boundary artifact in `docs/harness/19-production-identity-issuer-validator-boundary.md`. It selects the preferred future production identity validator placement as a backend-owned ingress/gateway JWT validation boundary, while deferring the concrete identity issuer, user profile source and production role authority to later Window 0 decisions and human approval.

Phase 015 was docs-only governance work. It did not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, external IdP integration, route migration, endpoint aliases, permission behavior changes, config mutation, config-store migration, role-store migration, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanent modular-monolith status, business behavior change or new feature work.

Phase 015 preserves demo headers as local/demo compatibility inputs, keeps `UserContext` as runtime context rather than production identity authority, keeps `role-access-configs.json` as the current transition role/menu/permission input, and records future readiness gates for token/session semantics, service-principal validation, service-to-service identity handoff, audit identity, gateway compatibility, route migration compatibility and rollback.

Phase 014 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-014-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-014-final.md`.

Phase 014 produced the durable production auth/gateway target-scope artifact in `docs/harness/18-production-auth-gateway-target-scope.md`. It selects a future-only governance direction: production identity should come through a backend-owned ingress/auth boundary, with gateway/JWT as the preferred target shape, and production role authority must be backend-owned. It keeps `X-User-Id` and `X-User-Role` as local/demo compatibility inputs only, keeps `role-access-configs.json` as the current transition role/menu/permission config input, and records service-to-service propagation, audit identity, demo-header compatibility, route migration and role/config-store dependencies as future requirements.

Phase 014 was docs-only governance work. It did not implement or approve gateway/auth/JWT, auth-service, user-service, role-service, login/session, OAuth, SSO, role DB, external IdP integration, route migration, endpoint aliases, permission behavior changes, config mutation, config-store migration, role-store migration, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanent modular-monolith status, business behavior change or new feature work.

Phase 013 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-013-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-013-final.md`.

Phase 013 produced the durable auth/gateway permission boundary artifact in `docs/harness/17-auth-gateway-permission-boundary.md`. It records `role-access-configs.json` as the current role/menu/permission config input under the Phase 012 JSON transition-store policy, records `X-User-Id` and `X-User-Role` as demo/runtime inputs rather than production identity or role authority, inventories current backend permission checks and no-explicit-permission read surfaces, and documents frontend route/menu/action gating as UI affordance only.

Phase 013 was docs-only governance work. It did not approve gateway/auth/JWT implementation, auth-service, user-service, login/session, role DB, role-store migration, route migration, route aliases, endpoint rename/deletion/consolidation, permission behavior change, config mutation, config-store migration, service extraction, frontend/Python reshaping, Kafka/database/Redis changes, permanent modular-monolith status, business behavior change or new feature work.

Phase 012 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-012-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-012-final.md`.

Phase 012 produced the durable config-store decision boundary artifact in `docs/harness/16-config-store-decision-boundary.md`. It records JSON config files under `quant-ai-platform/ai-config` and prompt template files under `quant-ai-platform/prompt-templates` as the current runtime transition stores for the next governance horizon.

Phase 012 was docs-only governance work. It did not approve config-store migration, config mutation, DB/Nacos/hybrid adoption, gateway/auth/JWT work, service extraction, route migration, route aliases, endpoint rename/deletion/consolidation, frontend/Python reshaping, Kafka/database/Redis changes, permanent modular-monolith status, business behavior change or new feature work.

Phase 005 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-005-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-005-final.md`.

Phase 005 selected the conservative architecture policy: continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon. This is not a permanent final-architecture declaration, does not approve service extraction, and does not approve route migration or breaking contract changes.

Phase 006 remains completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-006-review-fix-3.md`.

Phase 007 remains completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-007-review.md`.

Window 0 resolved the stale `Last Completed Phase` ordering note by consuming `docs/harness/handoffs/phase-005-final.md`, `docs/harness/handoffs/phase-006-final.md` and `docs/harness/handoffs/phase-007-final.md`.

Phase 008 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-008-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-008-final.md`.

Phase 008 produced the durable transition-host exit criteria inventory in `docs/harness/12-transition-host-exit-criteria.md`. It defined per-domain SoT, read-model, command, route dependency, guardrail, blocker, exit criteria and readiness-gate facts for report, market, risk, strategy, audit, config and workbench responsibilities inside `ai-orchestration-service`.

Phase 008 was docs-only governance work. It did not approve service extraction, route migration, gateway/auth, config-store migration, data-ingest split, permanent modular-monolith status, business behavior change or new feature work.

Phase 009 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-009-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-009-final.md`.

Phase 009 produced the durable report boundary readiness artifact in `docs/harness/13-report-boundary-readiness.md`. It applies the Phase 008 readiness template to the report domain and clarifies report facts, report evidence, report versions, report review commands, review audit, AI projection dependency, fallback provenance metadata and frontend report consumers before any later report extraction, route migration or permanence decision is considered.

Phase 009 was docs-only governance work. It did not approve report-service extraction, route migration, route aliases, endpoint rename/deletion/consolidation, gateway/auth, config-store migration, Python behavior change, frontend reshaping, Kafka/database changes, permanent modular-monolith status, business behavior change or new feature work.

Phase 010 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-010-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-010-final.md`.

Phase 010 produced the durable market/data-ingest boundary readiness artifact in `docs/harness/14-market-data-ingest-boundary-readiness.md`. It applies the Phase 008 readiness template to market event and data-ingest responsibilities and clarifies market facts, relations, analysis, market intelligence, source sync, source preview, source diagnose, CNINFO proxy, mock/demo ingest, batch import, ingest history, event source config, auto-trigger dependencies, frontend market consumers, Python market context and fallback provenance before any later market-service extraction, data-ingest-service extraction, route migration, config-store migration or permanence decision is considered.

Phase 010 was docs-only governance work. It did not approve market-service extraction, data-ingest-service extraction, route migration, route aliases, endpoint rename/deletion/consolidation, gateway/auth, config-store migration, database/schema changes, Kafka changes, frontend reshaping, Python behavior change, permanent modular-monolith status, business behavior change or new feature work.

Phase 011 is completed with residual risk after Window 3 approved `docs/harness/handoffs/phase-011-review.md`.

Window 4 froze the result in `docs/harness/handoffs/phase-011-final.md`.

Phase 011 produced the durable risk/strategy projection boundary readiness artifact in `docs/harness/15-risk-strategy-projection-boundary-readiness.md`. It applies the Phase 008 readiness template to risk warning and strategy signal responsibilities and clarifies risk facts, risk details, strategy facts, strategy factors, read models, strategy commands, `AiResultDomainProjectionService`, generated risk/strategy event publication, frontend consumers and Python risk/strategy context before any later risk-service extraction, strategy-service extraction, projection split, route migration, Kafka redesign or permanence decision is considered.

Phase 011 was docs-only governance work. It did not approve risk-service extraction, strategy-service extraction, projection splitting, route migration, route aliases, endpoint rename/deletion/consolidation, gateway/auth, config-store migration, database/schema changes, Redis changes, Kafka topic/payload changes, frontend reshaping, Python behavior change, permanent modular-monolith status, business behavior change or new feature work.

## Last Completed Phase

Phase 015 - Production Identity Issuer/Validator Selection Boundary.

## Open Blockers

None registered.

## Completed Phase 001 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- Window 3 reviewed and approved the implementation.

## Completed Phase 002 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- Window 3 first required a fix pass, then approved `phase-002-review-fix-1.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`.

## Completed Phase 003 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Backend-only implementation.
- No executable production logic changed; Phase 003 added production contract comments and source-level backend boundary tests.
- Window 3 reviewed and approved `phase-003-review.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`.

## Completed Phase 004 Constraints

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Mixed Python/backend scope was approved, but implementation changed only Python production/test files plus the implementation handoff.
- Fallback provenance was added only inside existing Python dictionaries and `reportMeta.contextSnapshot` map metadata.
- No Java production, frontend, DTO/VO/entity, database schema, Kafka topic or top-level Kafka payload field changed.
- Window 3 reviewed and approved `phase-004-review.md`.
- `python -m compileall app`, `python -m unittest discover -s tests` and `mvn -q test` passed.
- `python -m pytest` was unavailable because `pytest` is not installed in the current environment.

## Completed Phase 005 Constraints

- No breaking changes.
- URL paths and HTTP methods remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/policy implementation.
- Selected option: continue as a modular monolith inside `ai-orchestration-service` for the next governance horizon.
- `ai-orchestration-service` remains a transition host, not final architecture.
- Legacy non-task `/api/tasks/*` paths remain frozen transitional contracts under Phase 006, not final architecture.
- No service extraction, route migration, route alias, endpoint rename, gateway/auth implementation, config-store migration, data-ingest split or feature work was approved.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config or deployment file changed.
- Phase 003, Phase 004, Phase 006 and Phase 007 guardrails remain in force.
- Window 3 reviewed and approved `phase-005-review.md`.
- Maven, npm and Python verification were not required because Phase 005 changed documentation only.

## Completed Phase 006 Constraints

- No breaking changes.
- URL paths and HTTP methods remained stable.
- No business behavior change.
- No new feature work.
- Backend-focused contract/test implementation only.
- Java production code, controller runtime annotations and executable behavior remained unchanged.
- No frontend, Python, DTO/VO/entity, mapper, database schema, Kafka, `ai-config`, dependency or build-config file changed.
- The approved non-task legacy `/api/tasks/*` endpoint inventory is documented and guarded in backend tests.
- Focused tests guard endpoint path, HTTP method, controller owner, `Result<T>` response envelope, declared generic response type, request binding shape, request-param required/default behavior, explicit permission calls and intentional absence of explicit permission calls.
- Focused tests guard against unapproved `/api/tasks` endpoint additions across controller mapping shapes, including GET, POST, PUT, DELETE, PATCH and method-level `@RequestMapping`.
- Window 3 required three fix passes, then approved `phase-006-review-fix-3.md`.
- `mvn -q test` passed from `quant-ai-platform/quant-services`; output included the existing simulated `kafka down` warning stack trace from `TaskOutboxPublisherServiceTests.publishPendingOnceShouldMarkFailedWhenKafkaSendFails`.

## Completed Phase 007 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No user-visible business behavior change.
- No new feature work.
- Frontend-focused implementation only.
- Production source changes were comments/JSDoc-style authority notes; the new guard script is not imported by production code.
- `ResearchWorkbenchData` is documented as display-only aggregation and guarded away from command APIs.
- `TaskReportContextSnapshot`, `reportMeta`, `generationMode`, `fallbackReason` and related fallback provenance are documented as display/audit metadata only.
- Workbench output remains display, navigation and existing task-create source-context prefill only.
- Existing frontend API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes remained unchanged.
- No Java, Python, database, Kafka, `ai-config`, package/dependency or build-config file changed.
- Window 3 reviewed and approved `phase-007-review.md`.
- `node scripts/authority-boundary-check.mjs` and `npm run build` passed from `quant-ui`.

## Completed Phase 008 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/12-transition-host-exit-criteria.md` is now the durable inventory for report, market, risk, strategy, audit, config and workbench transition-host responsibilities.
- The inventory records per-domain SoT, current host classification, read-model surfaces, command surfaces, aggregation/display surfaces, legacy route dependencies, storage/config/Kafka dependencies, frontend consumers, Python touchpoints, guardrails, extraction blockers, exit criteria and later readiness gates.
- Task runtime/control, AI status/result/audit consumers, `market.event.standardized` consumption and `AiResultDomainProjectionService` are documented as context dependencies, not as Phase 008 extraction targets.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze and Phase 003/004/007 workbench/fallback authority guardrails remain in force.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-008-review.md`.
- Maven, npm and Python runtime verification were not required because Phase 008 changed documentation only.

## Completed Phase 009 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/13-report-boundary-readiness.md` is now the durable report-domain readiness artifact.
- The artifact records report belongs, authority objects, read-model surfaces, command surfaces, version/evidence/review-audit inventories, AI projection dependency, frontend consumers, Python/fallback provenance touchpoints, related display-only surfaces, stable URL/API contracts, inherited guardrails, extraction blockers, route-migration blockers, readiness gates, deferred decisions and stop rules.
- Stable report authority objects remain `research_report`, `research_report_version`, `research_report_section`, `report_evidence_ref`, `research_report_review_log` and `human_review_record`.
- `reportMeta`, raw payload, `contextSnapshot`, fallback provenance, workbench latest insight and frontend display fields remain metadata/display/projection input only, not report source of truth.
- `AiResultDomainProjectionService` remains a current projection dependency and was not moved, split or redesigned.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails and Phase 008 transition-host readiness template remain in force.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-009-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 009 changed documentation only.

## Completed Phase 010 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/14-market-data-ingest-boundary-readiness.md` is now the durable market/data-ingest boundary readiness artifact.
- The artifact records market/data-ingest belongs, authority objects, event source config and ingest history facts, market read-model and command surfaces, source sync/preview/diagnose and CNINFO proxy boundaries, mock/demo ingest boundaries, market intelligence display/read-model boundaries, Kafka `market.event.standardized` context, auto-trigger context, frontend market consumers, Python market context and fallback provenance, related display-only surfaces, stable URL/API contracts, inherited guardrails, blockers, readiness gates, deferred decisions and stop rules.
- Stable market/data-ingest authority objects remain `market_event`, `market_event_relation`, `market_event_analysis`, `event-source-configs.json` and `event-ingest-histories.json`.
- Market intelligence, source preview/diagnose output, CNINFO proxy output, mock/demo source payloads, Python fallback snapshots, workbench fields and frontend display/import-preview state remain display, preview, diagnostic, provenance or execution context only unless selected data is persisted through existing approved market authority paths.
- `market.event.standardized`, `MarketEventStandardizedPublisherService`, `MarketEventStandardizedConsumer` and market event auto-trigger behavior remain current context dependencies and were not moved, split or redesigned.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host readiness template and Phase 009 report readiness gates remain in force.
- No Java, Python, frontend, database, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-010-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 010 changed documentation only.

## Completed Phase 011 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md` is now the durable risk/strategy projection ownership boundary readiness artifact.
- The artifact records risk/strategy belongs, authority objects, read-model surfaces, strategy command surfaces, AI result projection dependency, generated domain-event publication, frontend risk/strategy consumers, Python risk/strategy context and fallback provenance, stable URL/API contracts, inherited guardrails, blockers, readiness gates, deferred decisions and stop rules.
- Stable risk authority objects remain `risk_warning` and `risk_warning_detail`.
- Stable strategy authority objects remain `strategy_signal` and `strategy_signal_factor`.
- `AiResultDomainProjectionService` remains the current shared projection dependency for report, evidence, risk and strategy, and was not split, moved, renamed or redesigned.
- `TaskDomainEventPublisherService`, `risk.warning.generated` and `strategy.signal.generated` remain current generated-event dependencies and were not redesigned or promoted to source of truth.
- Report risk points, report highlights, workbench summaries, market intelligence rows, Python risk/strategy context, fallback provenance, generated Kafka messages, frontend local state and dashboard cards remain context/display/provenance unless selected data is persisted through existing approved projection or command paths.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host readiness template, Phase 009 report readiness gates and Phase 010 market/data-ingest readiness gates remain in force.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-011-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 011 changed documentation only.

## Completed Phase 012 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/16-config-store-decision-boundary.md` is now the durable config-store decision boundary artifact.
- The artifact records agent config, workflow config, model strategy config, prompt templates, event source config, event auto-trigger config, role access config, config change audit and event ingest history boundaries.
- JSON config files and prompt template files remain the current runtime transition stores for the next governance horizon.
- DB, Nacos and hybrid storage are deferred future migration targets only; any target selection or migration requires later Window 0 scoring and explicit human approval.
- Frontend defaults/localStorage, request headers, Python fallbacks/defaults, config read models, audit rows and ingest history rows remain non-authoritative relative to current config stores.
- Existing config URLs, HTTP methods, controller owners, request bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, Java path-resolution behavior, file-backed audit behavior and Python reader paths remained unchanged.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates and Phase 011 risk/strategy readiness gates remain in force.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-012-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 012 changed documentation only.

## Completed Phase 013 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/17-auth-gateway-permission-boundary.md` is now the durable auth/gateway permission boundary artifact.
- The artifact records request headers, backend request context, role-access config, backend permission services, explicit backend permission checks, intentional no-explicit-permission read surfaces, frontend role/header/menu/action consumers and task-create permission behavior.
- `role-access-configs.json` remains the current role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `X-User-Id` and `X-User-Role` remain demo/runtime request inputs, not production identity or production role authority.
- The current default backend request context remains `guest` and `USER`.
- Current coarse access-role and business-role mappings were documented but not changed.
- Header-based demo auth remains current transition behavior for the next governance horizon. Production gateway/auth/JWT, auth-service, user-service, role-store, login/session and route proxy work remain deferred future decisions.
- Existing URLs, HTTP methods, controller owners, request bindings, response envelopes, response types, permission keys, explicit permission checks, no-explicit-permission read surfaces, frontend routes, frontend API functions, TypeScript shapes, localStorage behavior, request-header behavior, menu gating and action gating remained unchanged.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates, Phase 011 risk/strategy readiness gates and Phase 012 config-store boundary remain in force.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-013-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 013 changed documentation only.

## Completed Phase 014 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/18-production-auth-gateway-target-scope.md` is now the durable production auth/gateway target-scope artifact.
- The artifact scopes production identity as a future backend-owned ingress/auth boundary, with gateway/JWT as the preferred target shape and the concrete issuer or validator deferred to a later phase.
- The artifact scopes production role authority as future backend-owned authority while keeping `role-access-configs.json` as the current transition role/menu/permission config input under the Phase 012 JSON transition-store policy.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only, not production identity or production role authority.
- Backend explicit `requirePermission` calls remain current enforcement points for checked endpoints, intentional no-explicit-permission read surfaces remain unchanged, and frontend route/menu/action gating remains UI affordance only.
- Service-to-service propagation, audit identity semantics, demo-header compatibility or retirement, gateway/JWT implementation, identity issuer/validator selection, role authority migration, route migration and service extraction remain future requirements or deferred decisions.
- Existing URLs, HTTP methods, controller owners, request bindings, response envelopes, response types, permission keys, menu keys, role codes, header names/defaults, frontend routes, frontend API functions, TypeScript shapes, localStorage behavior, request-header behavior, menu gating and action gating remained unchanged.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates, Phase 011 risk/strategy readiness gates, Phase 012 config-store boundary and Phase 013 permission inventory remain in force.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-014-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 014 changed documentation only.

## Completed Phase 015 Constraints

- No breaking changes.
- URL paths and frontend routes remained stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- Docs-only architecture/governance implementation.
- `docs/harness/19-production-identity-issuer-validator-boundary.md` is now the durable production identity issuer/validator boundary artifact.
- The artifact selects backend-owned ingress/gateway JWT validation as the preferred future production identity validator placement.
- The concrete production identity issuer remains deferred to later Window 0 decision and human approval.
- User profile source and production role authority remain deferred.
- `X-User-Id` and `X-User-Role` remain local/demo compatibility inputs only, not production identity or production role authority.
- `UserContext` remains runtime context, not production identity authority.
- Backend explicit `requirePermission` calls remain current enforcement points for checked endpoints, intentional no-explicit-permission read surfaces remain unchanged, and frontend route/menu/action gating remains UI affordance only.
- Existing URLs, HTTP methods, controller owners, request bindings, response envelopes, response types, permission keys, menu keys, role codes, header names/defaults, frontend routes, frontend API functions, TypeScript shapes, localStorage behavior, request-header behavior, menu gating and action gating remained unchanged.
- Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates, Phase 011 risk/strategy readiness gates, Phase 012 config-store boundary, Phase 013 permission inventory and Phase 014 target-scope artifact remain in force.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment or business runtime file changed.
- Window 3 reviewed and approved `phase-015-review.md`.
- `node scripts/authority-boundary-check.mjs` passed from `quant-ui`; Maven, npm build and Python runtime verification were not required because Phase 015 changed documentation only.

## Open Architecture Drift

- `ai-orchestration-service` remains a transition host for multiple domains originally planned as separate services. Phase 005 keeps this as the next-governance-horizon modular-monolith policy, not final architecture.
- Phase 008 now documents per-domain exit criteria and readiness gates for the current transition-host responsibilities, but it does not close D001 or approve an ownership move.
- Phase 009 now documents report-specific readiness gates and blockers, but it does not close D001 or approve report ownership movement, extraction, route migration or permanence.
- Phase 010 now documents market/data-ingest-specific readiness gates and blockers, but it does not close D001, D009 or approve market ownership movement, data-ingest ownership movement, extraction, route migration, config-store migration or permanence.
- Phase 011 now documents risk/strategy-specific readiness gates and blockers, but it does not close D001 or approve risk ownership movement, strategy ownership movement, projection split, extraction, route migration, Kafka redesign, config-store migration or permanence.
- Phase 012 now documents config-store boundaries and the next-governance-horizon store decision, but it does not close D001, D007 or approve config ownership movement, config-store migration, DB/Nacos/hybrid adoption, gateway/auth work, route migration, service extraction or permanence.
- Phase 013 now documents auth/gateway permission boundaries and future readiness gates, but it does not close D001, D008 or approve production gateway/auth/JWT, route migration, service extraction, role-store migration or permanence.
- Phase 014 now scopes the future production auth/gateway target direction and identity/role authority prerequisites, but it does not close D001, D008 or approve gateway/auth/JWT implementation, service extraction, route migration, role-store migration, config-store migration or permanence.
- Phase 015 now selects future backend-owned ingress/gateway JWT validation as the preferred validator placement, but it does not close D001, D008 or approve gateway/JWT implementation, issuer implementation, service extraction, route migration, role-store migration, config-store migration or permanence.
- Production gateway/auth/config/service discovery architecture from the original plan is not implemented.

## Open Authority Drift

- Phase 002 moved risk, strategy, report, market-intelligence, audit, config dashboard and workbench read paths out of `TaskQueryServiceImpl` into internal domain query services.
- Phase 003 documented and tested Java backend workbench boundaries: workbench remains display-only aggregation, must not write domain facts, and must not feed backend command/projection authority.
- Phase 004 made in-scope Python fallback provenance auditable for planner, intent, financial, risk, report and market fallback paths using existing metadata surfaces.
- Phase 005 preserved current source-of-truth placement and did not move runtime authority.
- Phase 007 documented and guarded frontend consumer boundaries for current workbench aggregation and fallback provenance surfaces.
- Phase 008 documented per-domain SoT, read-model, command and aggregation boundaries for report, market, risk, strategy, audit, config and workbench transition responsibilities without moving authority.
- Phase 009 documented report-specific authority objects, projection dependencies, review/audit records, fallback provenance boundaries and frontend report consumer limits without moving authority.
- Phase 010 documented market/data-ingest authority objects, source/config/ingest history facts, market read-model and command boundaries, mock/demo ingest limits, source preview/diagnose/CNINFO limits, Kafka context dependencies, frontend market consumer limits and Python market fallback provenance without moving authority.
- Phase 011 documented risk/strategy authority objects, read-model and command boundaries, shared projection dependency, generated-event dependencies, frontend risk/strategy consumer limits and Python risk/strategy context/fallback provenance without moving authority.
- Phase 012 documented config authority objects, prompt-template storage, role-access/header demo auth boundaries, config audit, ingest history, Java/Python readers and frontend config consumers without moving authority. JSON config and prompt template files remain current runtime transition stores, while DB, Nacos and hybrid stores remain deferred future targets only.
- Phase 013 documented permission authority boundaries without moving authority. `role-access-configs.json` remains the current permission config input, request headers remain demo/runtime inputs, backend explicit `requirePermission` checks remain enforcement points for checked endpoints, `TaskRoleAccessService` remains a task-create reader/checker only, and frontend route/menu/action gating remains advisory UI affordance.
- Phase 014 documented future-only production identity and role authority target directions without moving current authority. A future backend-owned ingress/auth boundary is the preferred identity target shape, future production role authority must be backend-owned, demo headers remain local/demo compatibility inputs, and service-to-service propagation plus audit identity semantics remain requirements only.
- Phase 015 documented future-only production identity validator placement without moving current authority. Backend-owned ingress/gateway JWT validation is the preferred validator placement, while concrete issuer, user profile source and role authority remain deferred. Demo headers, `UserContext`, frontend localStorage and role config remain transition/runtime inputs only.
- Fallback metadata remains provenance only and must not become model-generated truth or business SoT.
- Future frontend, backend or Python surfaces that expose workbench or fallback metadata must keep equivalent non-authoritative provenance guardrails.

## Open Contract Drift

- Phase 001 split the former multi-domain `TaskQueryController` surface into domain-specific controllers.
- Non-task domain endpoints still keep legacy `/api/tasks/*` paths by approved Phase 001 constraint.
- Workbench Java backend display-only contract is guarded by Phase 003 tests/comments.
- Phase 004 preserved Kafka topics, top-level payload fields, URL paths, frontend contracts, DTO/VO/entity shapes and database schema while adding optional fallback provenance inside existing map metadata.
- Phase 005 preserved all current URLs, HTTP methods, request/response contracts, frontend routes, Kafka topics, database schema and runtime behavior.
- Phase 006 froze the approved legacy non-task `/api/tasks/*` endpoint inventory with backend contract tests. The legacy namespace remains a transition contract and must not drift without an approved phase handoff.
- Phase 007 preserved existing frontend routes, API endpoint strings, HTTP methods, function names, call signatures, response envelopes and TypeScript shapes.
- Phase 008 preserved all runtime contracts and recorded legacy route dependencies in the transition-host inventory without adding route aliases, migrations or endpoint changes.
- Phase 009 preserved all report runtime contracts and recorded the stable report URL/API inventory, frontend route/API inventory, permission behavior and response-shape boundaries without adding route aliases, migrations or endpoint changes.
- Phase 010 preserved all market/data-ingest runtime contracts and recorded stable market URL/API inventory, frontend route/API inventory, permission behavior, response-shape boundaries, Kafka context, JSON config/file facts and Python backend-client paths without adding route aliases, migrations or endpoint changes.
- Phase 011 preserved all risk/strategy runtime contracts and recorded stable risk/strategy URL/API inventory, frontend route/API inventory, permission behavior, response-shape boundaries, Kafka generated-event context, Redis cache context and Python backend-client paths without adding route aliases, migrations or endpoint changes.
- Phase 012 preserved all config runtime contracts and recorded stable config URL/API inventory, frontend route/API inventory, permission behavior, response-shape boundaries, Java path-resolution behavior, file-backed audit behavior, ingest history behavior and Python reader paths without adding route aliases, migrations, endpoint changes, config mutation or store migration.
- Phase 013 preserved all permission-related runtime contracts and recorded stable request-header names/defaults, explicit backend permission checks, intentional no-explicit-permission read surfaces, frontend route/API/header/localStorage/menu/action gating behavior and task-create permission behavior without adding route aliases, migrations, endpoint changes or permission behavior changes.
- Phase 014 preserved all auth/permission-related runtime contracts and recorded future compatibility requirements for gateway/JWT, demo headers, service-to-service propagation, route migration and role/config-store work without adding route aliases, migrations, endpoint changes or permission behavior changes.
- Phase 015 preserved all identity/auth/permission-related runtime contracts and recorded future compatibility requirements for gateway JWT validation, issuer selection, token/session semantics, service principals, service-to-service identity handoff, audit identity, route migration and rollback without adding route aliases, migrations, endpoint changes or permission behavior changes.
- Legacy non-task `/api/tasks/*` paths remain transition debt, but the current approved inventory is documented and guarded.
- Future fallback surfaces must continue preserving fallback provenance as non-authoritative metadata.

## Active Transition Hosts

- `ai-orchestration-service`, continued by Phase 005 as the next-governance-horizon modular monolith, inventoried by Phase 008, refined for report readiness by Phase 009, refined for market/data-ingest readiness by Phase 010, refined for risk/strategy readiness by Phase 011, refined for config-store boundary by Phase 012, refined for permission boundary by Phase 013, constrained by Phase 014 future auth/gateway target prerequisites and Phase 015 future identity validator placement, and still not final architecture
- Internal domain query services inside `ai-orchestration-service`
- `AiResultDomainProjectionService`, retained by Phase 011 as the current shared report/evidence/risk/strategy projection dependency and not final architecture
- Legacy `/api/tasks/*` paths for non-task domain surfaces, now frozen as approved transitional contracts by Phase 006
- Research workbench display aggregation
- JSON files under `quant-ai-platform/ai-config`, now documented by Phase 012 as current runtime transition stores, by Phase 013 for `role-access-configs.json` as the current permission config input and by Phase 014 as the current transition role/menu/permission input rather than final config or role-store architecture
- Prompt template files under `quant-ai-platform/prompt-templates`, now documented by Phase 012 as current prompt file transition stores rather than final prompt architecture
- Header-based demo auth and role access config, now documented by Phase 013 as current transition permission inputs, by Phase 014 as local/demo compatibility inputs rather than production auth architecture and by Phase 015 as not production identity issuer/validator authority
- `TaskRoleAccessService` inside `research-task-service`, now documented by Phase 013 as the task-create permission reader/checker and not the role config or auth owner
- Mock/demo ingest paths, source preview/diagnose, CNINFO proxy and market source mechanisms, now documented by Phase 010 as transition/demo/source mechanisms rather than production data-ingest architecture
- Python fallback path, now audited for Phase 004 in-scope provenance, Phase 007 current frontend consumers, Phase 010 market context/fallback consumers and Phase 011 risk/strategy context/fallback consumers but still a transition mechanism

## Candidate Next Phases

No active candidate is approved.

Recommended candidate inputs for Window 0 evaluation:

- Production identity issuer selection, such as auth-service, user-service, external IdP/directory or another backend-owned issuer, only if Window 0 and the user explicitly choose to act on Phase 015 issuer deferral gates.
- Production role authority selection, such as DB role store, config-store-backed role source, auth/user-service ownership or external role claims, only if Window 0 and the user explicitly choose to act on Phase 014 role-authority gates, Phase 015 validator placement and Phase 012 config-store constraints.
- Gateway/JWT implementation design with demo-header compatibility policy only if Window 0 and the user explicitly choose to act on Phase 014 target-scope gates and Phase 015 selected validator placement.
- Service-to-service propagation and audit identity semantics for AI callbacks, event auto task dispatch and future extracted services only if Window 0 and the user explicitly choose to act on Phase 014 propagation gates and Phase 015 service-principal/audit identity requirements.
- Config-store migration target/scoping, DB/Nacos/hybrid readiness, config schema/versioning or config audit/rollback planning only if Window 0 and the user explicitly choose to act on Phase 012 and Phase 014/015 role/config-store dependency gates.
- Legacy route migration decision phase only after Window 0 accounts for Phase 006 contract freeze and Phase 014/015 auth/gateway compatibility gates.
- Risk-service extraction, strategy-service extraction, projection-split planning, risk/strategy route migration or risk/strategy Kafka downstream planning only if Window 0 and the user explicitly choose to act on Phase 011 readiness gates.
- Report extraction or report route-migration planning only if Window 0 and the user explicitly choose to act on Phase 009 readiness gates.
- Market-service extraction, data-ingest-service extraction, market route migration or market config-store planning only if Window 0 and the user explicitly choose to act on Phase 010 readiness gates.

Phase 001, Phase 002, Phase 003, Phase 004, Phase 005, Phase 006, Phase 007, Phase 008, Phase 009, Phase 010, Phase 011, Phase 012, Phase 013, Phase 014 and Phase 015 are no longer candidates because they are completed and frozen by Window 4.

Window 0 must score candidates using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval.

## Human Approval Status

Phase 015 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-015.md`.

Phase 015 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- Frontend routes must remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No auth-service, user-service, role-service, login/session, OAuth, SSO or external IdP implementation.
- No config mutation.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Phase 015 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 014 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-014.md`.

Phase 014 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Phase 014 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 013 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-013.md`.

Phase 013 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No permission behavior change.
- No business behavior change.
- No new feature work.
- No gateway/auth/JWT implementation.
- No config mutation.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Phase 013 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 012 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-012.md`.

Phase 012 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- No config mutation or config-store migration implementation.
- Expected Window 2 type is docs-only by default after Window 1 planning is separately approved.

Phase 012 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 011 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-011.md`.

Phase 011 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type is docs-only by default, unless Window 1 justifies a narrow backend/static guard scope and the user approves it.

Phase 011 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 010 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-010.md`.

Phase 010 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type is docs-only by default, unless Window 1 justifies a narrow backend/static guard scope and the user approves it.

Phase 010 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 009 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-009.md`.

Phase 009 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type is docs-only by default, unless Window 1 justifies a narrower backend test/static guard scope and the user approves it.

Phase 009 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 008 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-008.md`.

Phase 008 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type was docs-only architecture/governance work.

Phase 008 was planned by Window 1, implemented by Window 2 as docs-only architecture/governance work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 005 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-005.md`, planned by Window 1, implemented by Window 2 as docs-only architecture/policy work, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 005 approval constraints:

- No breaking changes.
- URL paths must remain stable.
- No business behavior change.
- No new feature work.
- Expected Window 2 type was docs-only architecture/policy work.

Phase 001 was approved by the user, implemented by Window 2, reviewed by Window 3 and frozen by Window 4 as completed with residual risk.

Phase 002 was approved by the user after Window 0 steering decision, implemented by Window 2, fixed by Window 2 Fix Pass 1, reviewed and approved by Window 3 Review Fix 1, and frozen by Window 4 as completed with residual risk.

Phase 003 was approved by the user after Window 0 steering decision, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 004 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-004.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 007 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-007.md`, planned by Window 1, implemented by Window 2, reviewed and approved by Window 3, and frozen by Window 4 as completed with residual risk.

Phase 006 was approved by the user after Window 0 steering decision in `docs/harness/handoffs/steering-decision-phase-006.md`, planned by Window 1, implemented by Window 2 with three fix passes, reviewed and approved by Window 3 Review Fix 3, and frozen by Window 4 as completed with residual risk.

Next step must be Window 0. Window 0 must read `docs/harness/handoffs/phase-015-final.md`, discover the matching Phase 015 steering, architect, implementation and review handoffs, consume `docs/harness/19-production-identity-issuer-validator-boundary.md` together with the durable Phase 008/009/010/011/012/013/014 artifacts, score candidate next phases using `docs/harness/10-steering-state-machine.md`, propose exactly one primary candidate and one fallback candidate, and wait for human approval before Window 1 starts.
