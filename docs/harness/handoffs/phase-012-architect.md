# Phase 012 Architect Handoff

## Status

Window: Window 1 - Phase Architect.

Phase: Phase 012 - Config Store Decision Boundary.

This handoff is architecture planning only. It does not authorize implementation. Window 2 may start only after the user explicitly approves this file.

## Inputs Read

Required harness files:

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
- `docs/harness/handoffs/steering-decision-phase-012.md`

Additional current-state and previous-phase inputs:

- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- `docs/harness/handoffs/steering-decision-phase-011.md`
- `docs/harness/handoffs/phase-011-architect.md`
- `docs/harness/handoffs/phase-011-implementation.md`
- `docs/harness/handoffs/phase-011-review.md`
- `docs/harness/handoffs/phase-011-final.md`

Read-only planning inspection:

- `quant-ai-platform/ai-config/agent-configs.json`
- `quant-ai-platform/ai-config/workflow-configs.json`
- `quant-ai-platform/ai-config/model-strategies.json`
- `quant-ai-platform/ai-config/event-source-configs.json`
- `quant-ai-platform/ai-config/event-auto-trigger-configs.json`
- `quant-ai-platform/ai-config/event-ingest-histories.json`
- `quant-ai-platform/ai-config/config-change-audits.json`
- `quant-ai-platform/ai-config/role-access-configs.json`
- `quant-ai-platform/prompt-templates/*.txt`
- `ModelAgentConfigController.java`
- Java config services and implementations for agent, workflow, model strategy, prompt template, event source, event auto-trigger, role access, config audit and ingest history
- `research-task-service` role-access reader and create-task permission path
- `application-local.yml` config path properties in `ai-orchestration-service` and `research-task-service`
- `quant-ui/src/api/task.ts`
- `quant-ui/src/types/task.ts`
- `quant-ui/src/router/index.ts`
- `quant-ui/src/views/report/ModelAgentConfigCenterView.vue`
- `quant-ui/src/utils/roleAccess.ts`
- `quant-ui/src/utils/auth.ts`
- `quant-ui/src/utils/requestHeaders.ts`
- `quant-ui/src/utils/taskActionAccess.ts`
- `quant-ui/scripts/authority-boundary-check.mjs`
- Python config readers under `quant-ai-platform/quant-ai-engine/app/services`
- Python `settings.py` and `local.yml`

Phase 012 is not Phase 001, so the Phase 001 special Java reading list is not the controlling implementation scope for this handoff.

## 1. Phase Goal

Produce a docs-only config-store decision boundary artifact:

- Required durable artifact: `docs/harness/16-config-store-decision-boundary.md`.
- Clarify current config belongs, authority, contract and behavior boundaries for JSON-backed runtime config before any config-store migration, gateway/auth work, service extraction, route migration or permanent architecture decision.
- Inventory current config facts: agent config, workflow config, model strategy config, prompt template files, event source config, event auto-trigger config, role access config, config change audit and event ingest history.
- Classify Java readers/mutators, Python readers, frontend consumers, role-access inputs, audit facts and market/data-ingest config dependencies.
- Record the next-governance-horizon store decision without runtime change. Acceptable decision forms are:
  - JSON files remain the current transition store, with DB/Nacos/hybrid deferred; or
  - DB, Nacos or hybrid is selected only as a future migration target, while JSON remains the current runtime store until a later Window 0 decision and human approval.
- Preserve Phase 005 modular-monolith horizon policy, Phase 006 legacy `/api/tasks/*` contract freeze, Phase 007 frontend authority guardrails, Phase 008 transition-host inventory, Phase 009 report readiness gates, Phase 010 market/data-ingest readiness gates and Phase 011 risk/strategy readiness gates.

Window 2 must evaluate every statement in this order:

```text
belongs -> authority -> contract -> behavior
```

This phase is docs-only by default. It must not mutate config files, add tests, add static guards, change code or perform migration.

## 2. Belongs

Current belongs baseline:

- `ai-orchestration-service` is the transition host for model/agent/workflow/prompt/event-source/event-trigger/role-access config dashboard and update APIs.
- `quant-ai-platform/ai-config/*.json` is the current file-backed transition store for JSON runtime config facts.
- `quant-ai-platform/prompt-templates/*.txt` is the current file-backed transition store for prompt template config facts.
- `ConfigChangeAuditService` writes current config-change audit entries to `config-change-audits.json`. This is a config audit transition path, not a general audit architecture.
- `MarketEventIngestHistoryService` writes current ingest history to `event-ingest-histories.json`. This is a transition ingest history fact, not a production data-ingest ledger.
- `role-access-configs.json` plus request headers are the current demo/runtime permission inputs. They are not production auth architecture.
- `research-task-service` reads `role-access-configs.json` for task-create permission behavior. It is a role config reader, not the config-store owner.
- `quant-ai-engine` reads agent config, workflow config, model strategy config and prompt template files for AI execution. It is a config reader and fallback producer, not the final config authority or mutation host.
- `quant-ui` consumes config APIs, displays config data, sends update commands and caches role-access data for UI/menu behavior. It is a consumer/display host, not config or permission source of truth.
- `application-local.yml`, Python `local.yml` and private local overrides are environment/runtime settings. They are reader context for this phase, not the target of config-store migration.

In-scope config surfaces:

- model/agent config center read model
- role access config read model
- prompt template update command
- model strategy update command
- event auto-trigger update command
- event source config update command
- agent config update command
- workflow config update command
- role access update command
- config change audit display rows
- market event source config read model
- event ingest history read model
- Java and Python path-resolution behavior for current config files
- frontend config center, role utility and request-header consumers

Context-only dependencies:

- report, market, risk and strategy behaviors that consume config values
- market source sync, preview, diagnose and auto-trigger behavior that depends on event source or trigger config
- task create permission behavior in `research-task-service`
- header-based demo auth and frontend local selected role
- Python model/runtime settings and fallback behavior

Explicitly excluded:

- config-store migration implementation
- DB, Nacos or hybrid store implementation
- gateway/auth/JWT implementation
- service extraction or new config service
- route migration, aliases, endpoint rename, endpoint deletion or endpoint consolidation
- config file mutation
- prompt template mutation
- schema/version migration, migration runner, rollback runner or config bridge
- Java, Python, frontend, database, Kafka, Redis, deployment or dependency changes
- new product feature or new agent work

## 3. Authority

Current config authority objects for this phase:

| Authority object | Current meaning | Current host classification |
| --- | --- | --- |
| `agent-configs.json` | current agent enablement, timeout and agent metadata config facts | JSON transition store |
| `workflow-configs.json` | current workflow selection and task-type workflow config facts | JSON transition store |
| `model-strategies.json` | current model strategy/scenario config facts | JSON transition store |
| `prompt-templates/*.txt` | current prompt template text facts | file-backed prompt transition store |
| `event-source-configs.json` | current event source config facts | JSON transition store shared with market/data-ingest boundary |
| `event-auto-trigger-configs.json` | current market event auto-trigger config dependency | JSON transition store |
| `role-access-configs.json` | current role/menu/permission config input | JSON transition store plus request-header demo runtime |
| `config-change-audits.json` | current config change audit file | file-backed config audit transition path |
| `event-ingest-histories.json` | current ingest history file-backed fact | JSON/file transition path, not a production ingest ledger |

Authority rules:

- JSON files and prompt template files remain the current runtime store after Phase 012.
- A future DB, Nacos or hybrid target may be documented only as a deferred migration target. It must not become current runtime authority in Phase 012.
- Frontend defaults, localStorage role access cache, route metadata, menu gating and selected demo user role are not config or permission source of truth.
- Request headers remain demo/runtime permission inputs together with `role-access-configs.json`; they are not production identity or auth source of truth.
- Python repository fallbacks, default prompts and `settings.model` fallback values remain execution fallback/context. They do not replace file-backed config facts.
- Config read models and dashboard displays do not become command authority.
- Config change audit rows do not become the config source of truth.
- Event ingest history does not become event source config, market event SoT or production ingestion ledger.

Forbidden authority changes:

- No second current config source of truth may be introduced.
- No DB table, Nacos namespace, cache key, frontend cache, Python default or Java fallback may be documented as current config authority.
- No config read model may become the command source.
- No frontend local role or local role-access default may define backend permission truth.
- No Python fallback/default prompt may become prompt-template authority.
- No documentation may claim JSON config files, prompt templates, legacy `/api/tasks/*` paths, header-based demo auth or `ai-orchestration-service` are final architecture.
- No documentation may close D001, D002, D003, D007 or D008.

## 4. Contract

Stable backend config API inventory:

| Endpoint | Classification | Current owner | Stable permission behavior |
| --- | --- | --- | --- |
| `GET /api/tasks/model-agent-config` | config dashboard read model | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_VIEW` |
| `GET /api/tasks/role-access-configs` | role access read model | `ModelAgentConfigController` | no explicit `requirePermission` |
| `POST /api/tasks/model-agent-config/prompt-templates/{templateCode}` | prompt template update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/model-strategies/{strategyCode}` | model strategy update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/event-auto-trigger-rules/{ruleCode}` | event auto-trigger update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/event-sources/{sourceCode}` | event source update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/agents/{agentCode}` | agent config update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/workflows/{workflowCode}` | workflow config update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |
| `POST /api/tasks/model-agent-config/role-access/{roleCode}` | role access update command | `ModelAgentConfigController` | `PERMISSION_MODEL_AGENT_CONFIG_EDIT` |

Related stable config-dependent API surfaces:

- `GET /api/tasks/market-event-source-configs`
- `GET /api/tasks/market-events/ingest-history`
- existing market source sync, preview and diagnose endpoints that depend on event source config
- `POST /api/research/tasks` permission behavior that depends on `role-access-configs.json` through `research-task-service`

Stable backend contract details:

- URL paths and HTTP methods stay unchanged.
- Controller owners stay unchanged.
- `Result<T>` response envelopes stay unchanged.
- Request bindings stay unchanged, including path variables and request bodies for update commands.
- Response types stay unchanged, including `ModelAgentConfigCenterVO`, `RoleAccessConfigItemVO` and `Result<String>` update outputs.
- Permission behavior stays unchanged exactly as inventoried above.
- Config path-resolution behavior, audit append behavior, file read/write behavior and validation behavior stay unchanged.

Stable frontend routes, functions and consumers:

- `/model-agent-config`
- `fetchModelAgentConfigCenter`
- `fetchRoleAccessConfigs`
- `updatePromptTemplate`
- `updateModelStrategy`
- `updateEventAutoTriggerRule`
- `updateEventSourceConfig`
- `updateAgentConfig`
- `updateWorkflowConfig`
- `updateRoleAccessConfig`
- `ModelAgentConfigCenterView.vue`
- `roleAccess.ts`
- `auth.ts`
- `requestHeaders.ts`
- `taskActionAccess.ts`

Stable Python reader contracts:

- `AgentConfigRepository` reads `agent-configs.json`.
- `WorkflowConfigRepository` reads `workflow-configs.json`.
- `ModelStrategyRepository` reads `model-strategies.json` and falls back to `settings.model`.
- `PromptTemplateRepository` reads `prompt-templates/*.txt` and falls back to built-in prompts.
- `settings.py` reads `local.yml` and optional `local.private.yml`; this remains runtime settings, not config-store migration.

## 5. Allowed File Scope

Window 2 may modify only Phase 012 documentation files.

Required output files:

- `docs/harness/16-config-store-decision-boundary.md`
- `docs/harness/handoffs/phase-012-implementation.md`

Optional output file, only if the inventory becomes too large for the primary document:

- `docs/harness/handoffs/phase-012-config-inventory.md`

Allowed read-only inspection areas:

- `docs/harness/**`
- `quant-ai-platform/ai-config/**`
- `quant-ai-platform/prompt-templates/**`
- config-related Java controller/service/DTO/VO/test/resource files under `ai-orchestration-service`
- role-access-related Java files under `research-task-service`
- config-related frontend API/type/router/view/utility files under `quant-ui`
- Python config readers and settings files under `quant-ai-engine/app`

Window 2 must not write to those read-only inspection areas.

## 6. Forbidden File Scope

Window 2 must not modify:

- any Java production or test file under `quant-ai-platform/quant-services/**`
- any Python file under `quant-ai-platform/quant-ai-engine/**`
- any frontend file under `quant-ui/**`
- any file under `quant-ai-platform/ai-config/**`
- any file under `quant-ai-platform/prompt-templates/**`
- `application-*.yml`, `local.yml`, `local.private.yml` or other runtime settings files
- database migration, schema, SQL, seed, entity, mapper, DTO or VO files
- Redis key constants or cache behavior files
- Kafka topic constants, producers, consumers, message DTOs or listener code
- Maven, npm, Vite, TypeScript, Docker, deployment, gateway, Nacos, Sentinel or service-discovery files
- dependency or lock files
- `docs/harness/state/current-state.md`
- `docs/harness/02-authority-matrix.md`
- `docs/harness/03-host-ownership.md`
- `docs/harness/04-contract-map.md`
- `docs/harness/05-transition-lifetime.md`
- `docs/harness/06-debt-register.md`
- `docs/harness/07-phase-backlog.md`
- `docs/harness/08-eval-checklist.md`
- `docs/harness/09-window-protocol.md`
- `docs/harness/10-steering-state-machine.md`
- `docs/harness/12-transition-host-exit-criteria.md`
- `docs/harness/13-report-boundary-readiness.md`
- `docs/harness/14-market-data-ingest-boundary-readiness.md`
- `docs/harness/15-risk-strategy-projection-boundary-readiness.md`
- prior phase handoffs

Window 4 may later update state, debt, backlog or transition lifetime after Window 3 approval. Window 2 must not do that in Phase 012.

If satisfying Phase 012 appears to require any forbidden file, Window 2 must stop as blocked.

## 7. Must Stay Stable

Stable URL / API / behavior:

- Every config endpoint listed in this handoff keeps the same path, HTTP method, controller owner, request binding, response envelope, response type and permission behavior.
- No config URL moves to `/api/config`, `/api/model-agent-config`, `/api/admin/config`, `/api/config-store`, `/api/roles` or any other new namespace.
- No route alias, compatibility endpoint, gateway proxy, bridge, wrapper, resolver or frontend adapter is added.
- No endpoint is deleted, renamed, consolidated or split.
- No frontend route, API function name, endpoint string, call signature, TypeScript shape, local role utility behavior or request-header behavior changes.
- No JSON file name, JSON schema, prompt template file name, prompt file content, file path resolution, audit append behavior, ingest history append behavior or validation behavior changes.
- No current config mutation command is disabled, enabled, expanded or narrowed.
- No DB table, Nacos namespace, Redis key, Kafka topic, deployment profile, service discovery behavior or migration runner is introduced.
- No Python config reader path, fallback prompt behavior, model strategy fallback behavior, settings load behavior or AI execution behavior changes.

Stable architecture:

- JSON config files remain the current transition runtime store after Phase 012.
- Prompt template files remain the current prompt template store after Phase 012.
- `ai-orchestration-service` remains the config transition host, not final config architecture.
- `research-task-service` remains a role-access reader for task create permission, not a config owner.
- `quant-ai-engine` remains a config reader, not a config mutation host.
- `quant-ui` remains a config consumer/display host, not config or permission authority.
- Header-based demo auth remains transition behavior, not production auth architecture.
- Legacy `/api/tasks/*` remains a frozen transition namespace, not final route architecture.
- Phase 012 does not close D001, D002, D003, D007 or D008.

## 8. Allowed New Class / Method Types

No business-code classes or methods are allowed.

Allowed documentation-only structures:

- Markdown sections and tables.
- Config authority inventory.
- Config API inventory.
- Java reader/mutator inventory.
- Python reader inventory.
- Frontend consumer inventory.
- Role-access and header-demo-auth boundary table.
- Current JSON/prompt file inventory.
- Store-decision outcome section.
- Future migration prerequisite checklist.
- Belongs/authority/contract/behavior gate checklists.
- Deferred-decision lists.
- Stop-rule lists.

Allowed scripts/classes/methods:

- None.

Window 2 must not add Java test classes, source files, frontend scripts, Python scripts, build steps, runtime code or config files in this phase.

## 9. Forbidden Helpers / Adapters / Fallbacks / Bridges

Do not add or approve:

- any production `*Helper`, `*Adapter`, `*Bridge`, `*Facade`, `*Fallback`, `*Resolver`, `*Router`, `*Mapper`, `*Provider` or compatibility layer
- any test helper or static guard script
- any config-store bridge
- any DB/Nacos/hybrid adapter
- any migration runner, sync job, dual-write path, rollback tool or compatibility wrapper
- any frontend API adapter or frontend config truth resolver
- any gateway or proxy bridge
- any role/auth compatibility bridge
- any Python config fallback bridge or new fallback provenance field
- any Kafka, Redis or database compatibility bridge
- any route alias or endpoint compatibility path
- any temporary config-service wrapper

Window 2 may document existing Java services, Python readers, frontend consumers, file-backed audit behavior and existing guards as current facts, but must not create new paths or approve them as target architecture.

## 10. Required Config Boundary Artifact Shape

`docs/harness/16-config-store-decision-boundary.md` must include:

- Status and scope.
- Inputs and read-only inspection sources.
- Config belongs analysis.
- Config authority object inventory.
- Prompt template boundary.
- Role-access and header-based demo auth boundary.
- Config change audit boundary.
- Event source, event auto-trigger and ingest history boundary.
- Java config API and service inventory.
- `research-task-service` role-access reader inventory.
- Python config reader inventory.
- Frontend config consumer inventory.
- Stable URL/API contract table.
- Current guardrails inherited from Phase 005, Phase 006, Phase 007, Phase 008, Phase 009, Phase 010 and Phase 011.
- Store-decision outcome for the next governance horizon.
- Migration blockers and prerequisites before any DB, Nacos or hybrid migration.
- Gateway/auth, route-migration, service-extraction and data-ingest dependencies.
- Deferred decisions.
- Stop rules for later phases.

The artifact must explicitly state that it does not implement or approve config-store migration, config mutation, DB/Nacos adoption, service extraction, route migration, endpoint aliases, DTO/VO/entity/schema changes, Redis changes, Kafka changes, frontend changes, Python changes, gateway/auth work, business behavior changes or new feature work.

## 11. Acceptance Conditions

Phase 012 is acceptable only if all conditions hold:

- `docs/harness/16-config-store-decision-boundary.md` exists and is the primary durable config-store decision boundary artifact.
- `docs/harness/handoffs/phase-012-implementation.md` records exact files changed and verification outcomes.
- The artifact covers agent config, workflow config, model strategy config, prompt templates, event source config, event auto-trigger config, role access config, config change audit and event ingest history.
- The artifact names the current file-backed authority objects listed in this handoff.
- The artifact records that JSON and prompt files remain the current runtime stores after Phase 012.
- The artifact records any DB, Nacos or hybrid target only as a future/deferred migration target requiring later Window 0 selection and human approval.
- The artifact states that frontend defaults/localStorage, request headers, Python fallbacks/defaults, config read models, audit rows and ingest history rows do not become replacement config source of truth.
- The artifact preserves all config URLs, methods, bindings, response envelopes, response types, permission behavior, frontend routes, frontend API functions, TypeScript shapes, Python reader paths, Java path-resolution behavior and file-backed audit behavior.
- The artifact preserves Phase 005 through Phase 011 constraints.
- The artifact does not choose or implement service extraction, route migration, endpoint aliases, endpoint deletion/rename/consolidation, gateway/auth/JWT, config mutation, config-store migration, Python behavior change, frontend reshaping, Kafka/database/Redis changes, permanent modular-monolith architecture or new feature work.
- The artifact defines readiness gates for any later config-store migration, role/auth migration, route migration, data-ingest split or service extraction.
- No Java, Python, frontend, database, Redis, Kafka, `ai-config`, prompt-template, dependency, build-config, deployment, state, debt, backlog, transition lifetime, durable Phase 008/009/010/011 artifact or prior handoff file changes occur.
- Any future-phase recommendation is explicitly labeled as deferred and requiring a later Window 0 decision plus human approval.
- `git diff --name-only` shows only allowed Phase 012 documentation files as Window 2 changes, aside from pre-existing unrelated dirty files that are clearly excluded from the Window 2 claim.

## 12. Required Verification Commands

Window 2 must run from `D:\projects\bussiness` before edits:

```powershell
git status --short --untracked-files=all
```

Expected result: record pre-existing dirty/untracked files and exclude them from the Window 2 change claim.

Window 2 must run from `D:\projects\bussiness` after edits:

```powershell
git diff --name-only
```

Expected result: only allowed Phase 012 docs should be part of the Window 2 change claim. Existing unrelated dirty files must not be reverted or bundled into the Phase 012 implementation claim.

Window 2 must run:

```powershell
Test-Path docs/harness/16-config-store-decision-boundary.md
```

Expected result: `True`.

Window 2 must run:

```powershell
rg -n "agent-configs|workflow-configs|model-strategies|prompt-templates|event-source-configs|event-auto-trigger-configs|role-access-configs|config-change-audits|event-ingest-histories|ModelAgentConfigController|RoleAccessConfigService|TaskRoleAccessService|ConfigChangeAuditService|AgentConfigRepository|WorkflowConfigRepository|ModelStrategyRepository|PromptTemplateRepository|fetchModelAgentConfigCenter|fetchRoleAccessConfigs|MODEL_AGENT_CONFIG|X-User-Role|JSON transition store|readiness gate|Phase 005|Phase 006|Phase 007|Phase 008|Phase 009|Phase 010|Phase 011" docs/harness/16-config-store-decision-boundary.md docs/harness/handoffs/phase-012-implementation.md
```

Expected result: the config boundary artifact and implementation handoff contain the required config facts, readers, consumers, role/auth boundary and inherited guardrail references.

Window 2 must run:

```powershell
rg -n "DB|Nacos|hybrid|config-store migration|service extraction|route migration|route alias|breaking change|gateway/auth|JWT|database schema|Redis|Kafka|frontend reshaping|Python behavior|business code|new feature|permanent modular|config mutation" docs/harness/16-config-store-decision-boundary.md docs/harness/handoffs/phase-012-implementation.md
```

Expected result: any matches are in out-of-scope, blocker, deferred-decision, prerequisite, future-target or no-change sections, not in completed implementation claims.

Window 2 must run or record these read-only inventory checks from `D:\projects\bussiness`:

```powershell
rg --files quant-ai-platform/ai-config
```

```powershell
rg --files quant-ai-platform/prompt-templates
```

```powershell
rg -n "@RequestMapping|@GetMapping|@PostMapping|requirePermission|model-agent-config|role-access-configs|PERMISSION_MODEL_AGENT_CONFIG_VIEW|PERMISSION_MODEL_AGENT_CONFIG_EDIT" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/ModelAgentConfigController.java
```

```powershell
rg -n "quant\\.ai\\.(agent-config|workflow-config|model-strategy-config|prompt-template-dir|event-source-config|event-auto-trigger-config|event-ingest-history|role-access-config|config-audit)|Files\\.readString|Files\\.writeString|appendAudit|resolve.*Path" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/resources/application-local.yml
```

```powershell
rg -n "TaskRoleAccessService|role-access-config|PERMISSION_TASK_CREATE|requirePermission|currentUserRole|role-access-configs" quant-ai-platform/quant-services/quant-business/research-task-service/src/main/java quant-ai-platform/quant-services/quant-business/research-task-service/src/main/resources/application-local.yml
```

```powershell
rg -n "ModelAgentConfigController|/api/tasks/model-agent-config|/api/tasks/role-access-configs|PromptTemplateUpdateDTO|ModelStrategyUpdateDTO|EventAutoTriggerRuleUpdateDTO|EventSourceConfigUpdateDTO|AgentConfigUpdateDTO|WorkflowConfigUpdateDTO|RoleAccessConfigUpdateDTO|PERMISSION_MODEL_AGENT_CONFIG" quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/LegacyTaskApiContractFreezeTest.java quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskControllerMappingTest.java
```

```powershell
rg -n "fetchModelAgentConfigCenter|fetchRoleAccessConfigs|updatePromptTemplate|updateModelStrategy|updateEventAutoTriggerRule|updateEventSourceConfig|updateAgentConfig|updateWorkflowConfig|updateRoleAccessConfig|/model-agent-config|MODEL_AGENT_CONFIG|RoleAccessConfig|X-User-Id|X-User-Role" quant-ui/src/api/task.ts quant-ui/src/types/task.ts quant-ui/src/router/index.ts quant-ui/src/views/report/ModelAgentConfigCenterView.vue quant-ui/src/utils/roleAccess.ts quant-ui/src/utils/auth.ts quant-ui/src/utils/requestHeaders.ts quant-ui/src/utils/taskActionAccess.ts quant-ui/scripts/authority-boundary-check.mjs
```

```powershell
rg -n "agent-configs|workflow-configs|model-strategies|prompt-templates|local\\.yml|local\\.private\\.yml|AgentConfigRepository|WorkflowConfigRepository|ModelStrategyRepository|PromptTemplateRepository|settings\\.model|fallback_prompt" quant-ai-platform/quant-ai-engine/app/services/agent_config_repository.py quant-ai-platform/quant-ai-engine/app/services/workflow_config_repository.py quant-ai-platform/quant-ai-engine/app/services/model_strategy_repository.py quant-ai-platform/quant-ai-engine/app/services/prompt_template_repository.py quant-ai-platform/quant-ai-engine/app/config/settings.py quant-ai-platform/quant-ai-engine/app/config/local.yml
```

Window 2 must run from `D:\projects\bussiness\quant-ui`:

```powershell
node scripts/authority-boundary-check.mjs
```

Expected result: the existing Phase 007 static guard still passes. If it fails, Window 2 must record the failure as a blocker and must not patch frontend code in this phase.

Maven, npm build and Python runtime verification are not required because Phase 012 forbids Java, frontend, Python and test-code changes. If Window 2 touches any such area, it must stop as blocked instead of validating with builds.

## 13. Blocker Stop Rules

Window 2 must stop and write a blocker in `docs/harness/handoffs/phase-012-implementation.md` if any of these become necessary:

- Changing any URL path, HTTP method, endpoint owner, request binding, response envelope, response type, frontend route, frontend API function, TypeScript shape or permission behavior.
- Mutating any JSON config file or prompt template file.
- Changing Java config path resolution, Python config path resolution, config audit append behavior, ingest history append behavior or validation behavior.
- Adding a route alias, compatibility bridge, gateway proxy, frontend API adapter, config-store bridge, DB adapter, Nacos adapter, service wrapper, migration runner, dual-write path, rollback runner or sync job.
- Moving config, role-access, prompt-template, config-audit or ingest-history responsibility out of `ai-orchestration-service` without an approved extraction/migration phase.
- Moving task create permission behavior or role access reading out of `research-task-service` without an approved auth/permission phase.
- Creating or modifying Java, Python, frontend, database, Redis, Kafka, config, prompt-template, dependency, test, build or deployment files.
- Changing DTO, VO, entity, mapper, schema, topic, payload, Redis key, JSON shape, prompt-template shape, API type or frontend type shapes.
- Reclassifying frontend localStorage, frontend defaults, request headers, Python fallbacks/defaults, audit rows or ingest history rows as replacement config authority.
- Declaring `ai-orchestration-service`, JSON config files, prompt templates, header-based demo auth or legacy `/api/tasks/*` paths final architecture.
- Closing D001, D002, D003, D007 or D008.
- Selecting config-store migration, gateway/auth, service extraction, route migration, frontend reshaping, Python behavior change, Kafka/database/Redis change or permanent modular-monolith outcome as implemented work.
- Needing code behavior changes to make the config boundary artifact true.
- Finding that config authority cannot be described without changing the approved Phase 012 scope.
- Needing human approval for breaking changes, config mutation, DB/Nacos/hybrid migration, service extraction, route migration, gateway/auth implementation or new product features.
- The existing `node scripts/authority-boundary-check.mjs` guard fails and cannot be treated as an environment-only issue.

When blocked, Window 2 must:

1. Stop edits at the smallest reversible point.
2. Record the exact file, route, config object, reader, consumer, dependency or decision point that forced the blocker.
3. State which acceptance condition cannot be met.
4. State the smallest future phase or human decision required to unblock.
5. Ask for human decision instead of expanding scope.

## Window 2 Shape

Use one docs-only architecture/governance Window 2 implementation window.

Do not partition into backend, frontend or Python implementers. Do not start config-store migration, service extraction, route migration, gateway/auth work, DB/Nacos/hybrid adoption, Kafka redesign, test implementation, frontend guard edits, Python edits, config edits, prompt edits or product feature work. Do not proceed until the user approves this Phase 012 architect handoff.

## Human Approval Request

Please approve this Phase 012 architect handoff before Window 2 starts.

Default approval meaning:

- No breaking changes.
- Keep all URL paths and frontend routes stable.
- No business behavior change.
- No new feature work.
- No config mutation.
- No config-store migration implementation.
- Window 2 may perform docs-only config-store decision boundary work inside the allowed file boundaries above.
