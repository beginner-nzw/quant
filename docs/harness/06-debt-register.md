# Debt Register

| ID | Severity | Area | Debt | Evidence | Recommended Next Action | Status |
| --- | --- | --- | --- | --- | --- | --- |
| D001 | High | Service boundary | `ai-orchestration-service` hosts too many domains | Phase 001 split controller classes; Phase 002 split internal query service ownership; Phase 005 selected next-governance-horizon modular monolith, but the service host still contains multiple domains | Define transition-host exit criteria and per-domain readiness gates before any extraction, route migration or permanence claim | Open - Phase 005 chose bounded modular-monolith policy, transition-host debt remains |
| D002 | High | Contract | Non-task domains exposed under `/api/tasks/*` | risk, strategy, market, report, config APIs keep legacy `/api/tasks/*` paths in split controllers; Phase 006 froze the approved transitional inventory with focused backend contract tests | Later introduce domain URL groups only with explicit breaking-change approval; until then keep the Phase 006 inventory and guards in sync with any approved contract change | Open - Phase 006 mitigated drift, legacy namespace remains transition debt |
| D003 | Medium | Authority | Future `research-workbench` or fallback metadata surfaces can be mistaken as SoT | Phase 003 added Java backend display-only contract notes and boundary tests that keep workbench out of command/projection authority and guard it from domain writes; Phase 004 added Python fallback provenance for planner, intent, financial, risk, report and market fallback paths; Phase 007 added frontend authority notes and a focused static guard for current workbench/fallback consumers | Keep fallback/workbench metadata display-only, extend equivalent guardrails to future surfaces, and do not promote provenance into command or projection authority | Open - current known backend, Python and frontend surfaces mitigated |
| D004 | High | Read model | `TaskQueryServiceImpl` is too large and mixed | Phase 002 moved non-task read paths to internal domain query services and Window 3 approved after Fix Pass 1 | Keep boundary tests; do not reintroduce non-task methods into `TaskQueryServiceImpl` | Closed - Phase 002 completed |
| D005 | Medium | AI workflow | LangGraph workflow is linear | `workflow_builder.py` | Defer complex branching until contracts are stable | Open |
| D006 | Medium | Agent coverage | Missing planned agents | no event/industry/strategy/audit agent files | Add only after current workflow contract is frozen | Open |
| D007 | Medium | Config governance | Runtime config stored in JSON files | `ai-config/*.json` | Keep audited; decide DB/Nacos target later | Open |
| D008 | Medium | Security | Header-based demo auth | `UserContextFilter`, frontend localStorage role | Keep demo only; decide auth-service/JWT later | Open |
| D009 | Medium | Ingestion | No independent data-ingest-service | mock/source sync under ai-orchestration-service | Mark mock as test/demo; define ingestion ownership | Open |
| D010 | Medium | Deployment | Docker compose lacks gateway/Nacos/Sentinel/service containers | compose has MySQL/Redis/Kafka/Zookeeper only | Defer until architecture boundary is stable | Open |
| D011 | Medium | Eval | Limited frontend tests; Python pytest unavailable in current env | Phase 004 added Python unittest fallback provenance coverage; Phase 007 added a focused frontend static guard and `npm run build` passed; `pytest` is unavailable | Add broader frontend/e2e coverage and install or replace pytest path when that becomes a phase goal | Open |
| D012 | Low | Namespace | Report/review/config APIs mixed with task namespace | `/api/tasks/...`; Phase 006 guards the approved mixed-namespace inventory | Do not add new mixed endpoints without approved phase handoff and contract inventory update | Open - guarded by Phase 006 |

## Debt Priority

Phase 001 completed the controller-surface part of D001 and reduced `TaskQueryController` drift.

Phase 002 completed the D004 internal read-model split and further reduced D001 by separating internal query service ownership inside `ai-orchestration-service`.

Phase 003 hardened D003 for the Java backend by documenting workbench display-only authority and adding source-level tests against backend command/projection use and domain writes.

Phase 004 hardened D003 for Python fallback auditability by preserving fallback provenance in existing result metadata and adding focused Python regression tests.

Phase 007 hardened D003 for current frontend consumer boundaries by documenting workbench/fallback provenance as display-only metadata and adding a focused static guard against command-authority usage.

Phase 006 hardened D002 by freezing the approved legacy non-task `/api/tasks/*` endpoint inventory with backend tests for path, method, owner, response envelope, request binding and permission behavior. The legacy namespace remains transition debt because Phase 006 preserved paths rather than migrating them.

Phase 005 addressed the immediate D001 policy question by choosing to continue `ai-orchestration-service` as a modular monolith for the next governance horizon. This does not close D001: the service remains a transition host, not final architecture, and extraction/permanence still require later human-approved phases.

Next work should start from D001 follow-through: define transition-host exit criteria, per-domain boundary readiness and prerequisites for any later extraction or permanence claim, while preserving the Phase 006 legacy contract freeze.

Do not begin large feature expansion until D001 follow-through has explicit exit criteria or readiness gates and any later D002 route migration is explicitly approved as a breaking-change phase.
