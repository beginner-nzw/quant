# Debt Register

| ID | Severity | Area | Debt | Evidence | Recommended Next Action | Status |
| --- | --- | --- | --- | --- | --- | --- |
| D001 | High | Service boundary | `ai-orchestration-service` hosts too many domains | Phase 001 split controller classes; Phase 002 split internal query service ownership, but the service host still contains multiple domains | Harden contracts and authority before deciding service extraction or modular-monolith permanence | Open - Phase 001 and Phase 002 partially mitigated |
| D002 | High | Contract | Non-task domains exposed under `/api/tasks/*` | risk, strategy, market, report, config APIs keep legacy `/api/tasks/*` paths in split controllers | Document allowed legacy contract; later introduce domain URL groups only with explicit breaking-change approval | Open |
| D003 | High | Authority | `research-workbench` can be mistaken as SoT | Phase 002 moved workbench display aggregation to `ResearchWorkbenchQueryServiceImpl` and removed copied domain read-model entrypoints; contract hardening is still pending | Add explicit display-only contract tests/comments around workbench and fallback consumers | Open - Phase 002 partially mitigated |
| D004 | High | Read model | `TaskQueryServiceImpl` is too large and mixed | Phase 002 moved non-task read paths to internal domain query services and Window 3 approved after Fix Pass 1 | Keep boundary tests; do not reintroduce non-task methods into `TaskQueryServiceImpl` | Closed - Phase 002 completed |
| D005 | Medium | AI workflow | LangGraph workflow is linear | `workflow_builder.py` | Defer complex branching until contracts are stable | Open |
| D006 | Medium | Agent coverage | Missing planned agents | no event/industry/strategy/audit agent files | Add only after current workflow contract is frozen | Open |
| D007 | Medium | Config governance | Runtime config stored in JSON files | `ai-config/*.json` | Keep audited; decide DB/Nacos target later | Open |
| D008 | Medium | Security | Header-based demo auth | `UserContextFilter`, frontend localStorage role | Keep demo only; decide auth-service/JWT later | Open |
| D009 | Medium | Ingestion | No independent data-ingest-service | mock/source sync under ai-orchestration-service | Mark mock as test/demo; define ingestion ownership | Open |
| D010 | Medium | Deployment | Docker compose lacks gateway/Nacos/Sentinel/service containers | compose has MySQL/Redis/Kafka/Zookeeper only | Defer until architecture boundary is stable | Open |
| D011 | Medium | Eval | No frontend tests; Python pytest unavailable in current env | build passes, Python compileall only | Add eval checklist first, tests later by phase | Open |
| D012 | Low | Namespace | Report/review/config APIs mixed with task namespace | `/api/tasks/...` | Do not add new mixed endpoints without contract update | Open |

## Debt Priority

Phase 001 completed the controller-surface part of D001 and reduced `TaskQueryController` drift.

Phase 002 completed the D004 internal read-model split and further reduced D001 by separating internal query service ownership inside `ai-orchestration-service`.

Next work should still start with the remaining D001-D003 contract and authority risks, especially workbench/fallback authority hardening and the approved legacy `/api/tasks/*` contract debt.

Do not begin large feature expansion until the remaining D001-D003 authority and contract risks have at least contract-level mitigation.
