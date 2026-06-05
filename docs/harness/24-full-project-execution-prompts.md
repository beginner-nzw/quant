# Full Project Execution Prompts

## Purpose

This file replaces the Window-heavy governance flow with a practical execution plan for completing the full project.

The project should no longer open standalone harness-governance phases. Harness rules remain constraints, not phase goals.

Each phase below has two prompts:

- Implementation prompt: use in a fresh implementation window.
- Acceptance prompt: use in a fresh review/acceptance window.

If acceptance requires fixes, open a fresh implementation window for the same phase with the acceptance findings included.

## Global Rules For Every Phase

Every implementation and acceptance window must follow:

```text
belongs -> authority -> contract -> behavior
```

Stable rules:

- Read `docs/harness/00-project-charter.md` through `docs/harness/10-steering-state-machine.md`.
- Read `docs/harness/state/current-state.md`.
- Read `docs/harness/22-remaining-governance-closure.md`.
- Read `docs/harness/23-full-project-completion-roadmap.md`.
- Read the latest relevant handoffs under `docs/harness/handoffs`.
- Do not treat frontend state, workbench aggregation, fallback metadata, audit metadata, demo headers or JSON config as production authority unless the phase explicitly implements a new approved authority.
- Keep current contracts stable unless the phase explicitly says route migration or breaking/compatibility change is in scope.
- Preserve or document compatibility for existing URLs, DTO/VO/entity shapes, Kafka topics, Redis keys, Python payloads and frontend types.
- Run relevant verification:
  - Java changed: `mvn -q test` from `quant-ai-platform/quant-services`.
  - Frontend changed: `npm run build` from `quant-ui`.
  - Python changed: `python -m compileall app` and available tests from `quant-ai-platform/quant-ai-engine`.
- Write an implementation handoff or acceptance handoff for the phase.

Stop and ask the user if the phase needs:

- removing a compatibility path
- widening permissions
- deleting existing data or routes
- irreversible migration
- external IdP vendor lock-in
- production secret material
- destructive git commands

## Phase 1 - Gateway/JWT And Demo Compatibility

Goal:

Implement a production identity ingress foundation with JWT validation design/runtime support while preserving local/demo header compatibility.

### Implementation Prompt

```text
你是 Phase 1 Implementation：Gateway/JWT And Demo Compatibility。

目标：实现生产身份入口基础能力，同时保留当前 demo header 兼容。

必须先读：
- docs/harness/00-project-charter.md 到 docs/harness/10-steering-state-machine.md
- docs/harness/17-auth-gateway-permission-boundary.md
- docs/harness/18-production-auth-gateway-target-scope.md
- docs/harness/19-production-identity-issuer-validator-boundary.md
- docs/harness/20-production-identity-issuer-boundary.md
- docs/harness/21-production-role-authority-boundary.md
- docs/harness/22-remaining-governance-closure.md
- docs/harness/23-full-project-completion-roadmap.md

实现要求：
- 在 Java 后端实现 JWT 验证边界或 gateway/security filter 基础能力。
- 保留 `X-User-Id`、`X-User-Role`、`X-Trace-Id` 的 local/demo 兼容模式。
- 明确 production 模式不能信任前端 header 作为 identity/role authority。
- 将验证后的身份映射到现有 `UserContext`，但不要改变现有权限语义。
- 加入失败行为：缺 token、无效 token、过期 token、demo 模式 fallback。
- 增加测试覆盖 JWT/demo header 兼容、失败行为、当前权限不变。
- 写 phase-001-implementation.md，记录变更、兼容策略、测试结果。

禁止：
- 不要接入真实外部 IdP。
- 不要删除 demo header 兼容。
- 不要改变业务接口路径。
- 不要扩大或收窄权限。
- 不要创建完整 auth-service/user-service/role-service。
```

### Acceptance Prompt

```text
你是 Phase 1 Acceptance：Gateway/JWT And Demo Compatibility。

按 belongs -> authority -> contract -> behavior 审查实现。

验收重点：
- JWT 验证边界是否属于 backend-owned ingress/security host。
- demo header 是否只在 local/demo 兼容模式生效。
- 前端 header 是否没有被升级为 production authority。
- `UserContext` 行为是否兼容现有服务。
- URL、DTO/VO、权限行为是否稳定。
- 缺 token、无效 token、过期 token、demo fallback 是否有测试。
- `mvn -q test` 是否通过；如前端改动，`npm run build` 是否通过。

输出：
- approve / require fixes / block
- findings 按严重程度排序
- 必要时给出 fix implementation prompt
```

## Phase 2 - Role Authority, Profile Source And Role Store

Goal:

Implement backend-owned role authority and user profile source, replacing JSON role config as production authority while preserving compatibility.

### Implementation Prompt

```text
你是 Phase 2 Implementation：Role Authority, Profile Source And Role Store。

目标：实现 backend-owned role authority、user profile source 和最小 role store。

必须先读 Phase 1 handoff/review，以及：
- docs/harness/16-config-store-decision-boundary.md
- docs/harness/17-auth-gateway-permission-boundary.md
- docs/harness/21-production-role-authority-boundary.md
- docs/harness/22-remaining-governance-closure.md

实现要求：
- 建立后端角色/权限/菜单 mapping 的生产 authority。
- 建立最小 user profile source，至少包含 userId、displayName、status、roles。
- 保留 `role-access-configs.json` 作为迁移/本地兼容输入，不再作为 production final authority。
- 实现从 JWT identity/userId 到 profile/roles 的查询路径。
- 保持现有 role code、permission key、menu key 兼容。
- 增加迁移/seed 或兼容加载逻辑，禁止权限无意扩大。
- 增加权限正向/反向测试、禁用用户测试、未知用户测试。
- 写 phase-002-implementation.md。

禁止：
- 不要删除当前 JSON config 文件。
- 不要改变现有 permission key/menu key 名称。
- 不要让 frontend localStorage 成为 role truth。
```

### Acceptance Prompt

```text
你是 Phase 2 Acceptance。

验收重点：
- role/profile authority 是否在 backend-owned host。
- JSON role config 是否降级为兼容/迁移输入，而不是 production SoT。
- permission key/menu key/role code 是否保持兼容。
- JWT -> profile -> role -> permission 链路是否清楚。
- 未知/禁用用户是否不能获得权限。
- 前端 gating 是否仍是 UI affordance。
- Java 测试是否覆盖 no-permission-widening。

输出 approve / require fixes / block。
```

## Phase 3 - Service-To-Service Identity And Audit Identity

Goal:

Implement service principal, original actor, delegated actor and audit identity propagation for async and AI callback flows.

### Implementation Prompt

```text
你是 Phase 3 Implementation：Service-To-Service Identity And Audit Identity。

目标：为 Kafka、AI callback、event auto dispatch 和未来服务调用实现 service-to-service identity 与审计身份。

实现要求：
- 定义 service principal、system actor、original actor、delegated actor。
- 在 AI task dispatch/status/result/audit、market auto trigger、retry/cancel 等链路保留 actor provenance。
- 更新 audit records/message logs，记录 identity source、role source、service principal。
- 不改变 Kafka topic 名称；如 payload 扩展，保持向后兼容。
- 添加 Java/Python 兼容测试，确保旧消息仍可消费。
- 写 phase-003-implementation.md。

禁止：
- 不要用前端 header 伪造 service principal。
- 不要破坏现有 Kafka payload 兼容。
```

### Acceptance Prompt

```text
你是 Phase 3 Acceptance。

验收重点：
- service principal 和 human actor 是否区分。
- audit identity 是否可追溯 original/delegated/system actor。
- Kafka payload 是否向后兼容。
- AI engine 是否能处理新旧消息。
- 审计字段是否没有变成权限 authority。
- Maven/Python tests 是否通过。

输出 approve / require fixes / block。
```

## Phase 4 - Config Store, Role Store Migration And Rollback

Goal:

Move runtime config and role mappings behind governed backend store with audit and rollback.

### Implementation Prompt

```text
你是 Phase 4 Implementation：Config Store, Role Store Migration And Rollback。

目标：将 agent/workflow/model/prompt/event/role config 从裸 JSON transition store 迁移到可治理 store。

实现要求：
- 选择并实现 DB-backed 或 approved backend config store。
- 支持版本、single-writer、audit、rollback、兼容读取。
- Java 和 Python 都通过稳定 API/reader 读取配置。
- 前端配置页面改为消费后端 authority，不自行定义默认 truth。
- 保留本地/demo seed 和回滚路径。
- 添加迁移测试、rollback 测试、Java/Python/frontend compatibility 测试。
- 写 phase-004-implementation.md。

禁止：
- 不要静默改变现有配置语义。
- 不要丢失 config-change audit。
```

### Acceptance Prompt

```text
你是 Phase 4 Acceptance。

验收重点：
- config/role store authority 是否单一。
- JSON 文件是否不再是 production SoT。
- 迁移、回滚、审计是否可验证。
- Java/Python/frontend 读取是否兼容。
- 无权限扩大、无配置静默变更。
- Maven、Python、frontend build 是否通过。

输出 approve / require fixes / block。
```

## Phase 5 - Data Ingest Service And Source Adapters

Goal:

Create production data ingestion ownership and real source adapters.

### Implementation Prompt

```text
你是 Phase 5 Implementation：Data Ingest Service And Source Adapters。

目标：建立 data-ingest ownership，替代 mock/demo ingest 作为主路径。

实现要求：
- 创建或明确 data-ingest service/module。
- 实现公告、新闻、研报、政策等 source adapter 基础能力。
- 保留 source provenance、fetch status、raw payload ref、retry/deadletter。
- mock ingest 只能保留为 local/demo/test。
- 标准化输出到 `market.event.standardized` 或 approved market event contract。
- 增加 source health、ingest history、失败重试测试。
- 写 phase-005-implementation.md。

禁止：
- 不要把 mock payload 当 production data authority。
- 不要直接让前端导入数据成为 SoT。
```

### Acceptance Prompt

```text
你是 Phase 5 Acceptance。

验收重点：
- data-ingest ownership 是否脱离 transition ambiguity。
- raw/provenance/standardized event 是否分清 authority。
- mock ingest 是否只用于 demo/test。
- retry/deadletter/source health 是否可见。
- Kafka/database contract 是否稳定。

输出 approve / require fixes / block。
```

## Phase 6 - Market Event Normalization And Auto Trigger

Goal:

Productionize market event normalization, relation analysis and AI task auto-trigger.

### Implementation Prompt

```text
你是 Phase 6 Implementation：Market Event Normalization And Auto Trigger。

目标：完善 market_event 标准化、去重、关联、分析和自动触发 AI task。

实现要求：
- 实现 dedupe、event relation、event analysis、confidence/provenance。
- 自动触发任务必须有权限/限流/审计/幂等。
- 触发上下文不得来自 workbench/display-only surface。
- 前端增加运营视图：触发原因、来源、失败、重试。
- 增加自动触发、重复事件、失败补偿测试。
- 写 phase-006-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 6 Acceptance。

验收重点：
- market_event authority 是否清楚。
- auto-trigger 是否幂等、可审计、可限流。
- workbench/display fields 是否没有成为 trigger truth。
- 前端是否只展示/操作 approved commands。
- 测试是否覆盖重复、失败、重试。
```

## Phase 7 - Event And Industry Research Agents

Goal:

Add event extraction and industry research agents with grounded outputs.

### Implementation Prompt

```text
你是 Phase 7 Implementation：Event And Industry Research Agents。

目标：为 Python AI engine 增加 event_extraction_agent 和 industry_research_agent。

实现要求：
- 新 agent 接入 agent config、workflow config、prompt templates。
- 输出必须包含 evidence/provenance/fallback reason。
- Java projection 只消费 approved payload fields，不把 fallback 当 truth。
- 更新 LangGraph workflow sequence 和测试。
- 前端展示新增 agent trace。
- 写 phase-007-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 7 Acceptance。

验收重点：
- 新 agents 是否属于 AI execution host。
- 输出 provenance/fallback 是否完整。
- Java/Frontend 是否没有把 fallback 升级为业务 truth。
- Python compile/tests、Maven tests、frontend build 是否按改动范围通过。
```

## Phase 8 - Strategy And Audit Agents

Goal:

Add strategy reasoning and audit/compliance agents.

### Implementation Prompt

```text
你是 Phase 8 Implementation：Strategy And Audit Agents。

目标：增加 strategy_reasoning_agent 和 audit_compliance_agent。

实现要求：
- strategy agent 产出 strategy signal candidate、factor、confidence、evidence。
- audit agent 产出 policy/evidence/report-review 支持，不直接审批业务。
- Java projection 必须保持 risk/strategy/report/audit authority 边界。
- 前端展示 trace、evidence、review suggestion。
- 增加 Python/Java/frontend 测试。
- 写 phase-008-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 8 Acceptance。

验收重点：
- strategy/audit agent 是否没有越权成为业务审批者。
- strategy signal projection 是否保留 authority rules。
- audit suggestion 是否不等于 human approval。
- 测试覆盖 fallback/provenance/projection。
```

## Phase 9 - Conditional Workflow, Checkpoint And Resume

Goal:

Upgrade from linear LangGraph to conditional workflow with checkpoint/resume/rerun.

### Implementation Prompt

```text
你是 Phase 9 Implementation：Conditional Workflow, Checkpoint And Resume。

目标：将线性 LangGraph 升级为条件分支、checkpoint、resume、node rerun。

实现要求：
- 按 taskType、evidence quality、risk level、review result 决定分支。
- 持久化 workflow checkpoint 和 node state。
- 支持失败恢复、节点重跑、timeout 后恢复。
- Java task/workflow read-model 展示真实状态。
- 前端提供 trace/resume/rerun 操作。
- 增加 Python workflow tests、Java control tests、frontend build。
- 写 phase-009-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 9 Acceptance。

验收重点：
- workflow authority 是否在 AI engine/runtime read-model。
- checkpoint/resume 是否可追溯、幂等。
- 前端是否只调用 approved commands。
- 失败、timeout、rerun 是否有测试。
```

## Phase 10 - Human Review Loops And AI Evaluation

Goal:

Add human-in-the-loop review nodes and AI evaluation harness.

### Implementation Prompt

```text
你是 Phase 10 Implementation：Human Review Loops And AI Evaluation。

目标：实现 report/risk/compliance human review loop 和 AI eval harness。

实现要求：
- 增加人工复核队列、打回、修订、重新运行。
- AI workflow 支持等待 human decision。
- eval harness 覆盖 hallucination、evidence grounding、fallback、report quality。
- 前端完成 review/correction UX。
- 增加 e2e-ish tests 或 focused integration tests。
- 写 phase-010-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 10 Acceptance。

验收重点：
- human approval 是否仍由 human/review command 产生。
- AI suggestion 是否没有成为审批 truth。
- eval harness 是否能稳定运行。
- review loop 是否可恢复、可审计。
```

## Phase 11 - Report Domain Ownership

Goal:

Extract report service or finalize report modular boundary with stable contracts.

### Implementation Prompt

```text
你是 Phase 11 Implementation：Report Domain Ownership。

目标：将 report domain 从 transition host 中抽离，或建立 final modular boundary。

实现要求：
- 明确 report SoT、version、section、evidence、review log ownership。
- 迁移或重组 report APIs，保留 legacy compatibility。
- report.generated event contract 稳定。
- 前端 report center/review pages 使用 stable report contract。
- 增加 contract tests 和 migration/compat tests。
- 写 phase-011-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 11 Acceptance。

验收重点：
- report truth 是否单一。
- legacy `/api/tasks/*` 是否有兼容或明确迁移。
- frontend/workbench 是否没有定义 report truth。
- contract tests 是否覆盖 path/envelope/permission。
```

## Phase 12 - Risk And Strategy Domain Ownership

Goal:

Extract or finalize risk and strategy domain ownership.

### Implementation Prompt

```text
你是 Phase 12 Implementation：Risk And Strategy Domain Ownership。

目标：稳定 risk warning 与 strategy signal 的 service/domain ownership。

实现要求：
- 明确 risk_warning、strategy_signal、factor/detail ownership。
- 拆分或模块化 projection service。
- 稳定 generated events 和 downstream contracts。
- 迁移/兼容 risk/strategy APIs。
- 前端 risk/strategy center 使用 stable contracts。
- 增加 projection、event、contract tests。
- 写 phase-012-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 12 Acceptance。

验收重点：
- risk/strategy SoT 是否单一。
- report/workbench/AI fallback 是否没有变成 risk/strategy truth。
- generated events 是否可追踪、幂等。
- route compatibility 和 tests 是否充分。
```

## Phase 13 - Market/Data-Ingest Ownership And Route Compatibility

Goal:

Finalize market service/data-ingest ownership and route compatibility.

### Implementation Prompt

```text
你是 Phase 13 Implementation：Market/Data-Ingest Ownership And Route Compatibility。

目标：稳定 market event 与 data-ingest 的最终 ownership。

实现要求：
- market_event facts 和 data-ingest raw/source facts 分离。
- 完成 market/data-ingest route compatibility 或迁移。
- 前端 market center、intelligence center、source operations 使用 stable contracts。
- 增加 source sync、preview、diagnose、mock/demo guard tests。
- 写 phase-013-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 13 Acceptance。

验收重点：
- market facts 与 ingest facts 是否分离。
- mock/demo 是否没有成为 production source。
- route migration/compat 是否符合 Phase 006 guard。
- 前端是否不定义 market truth。
```

## Phase 14 - Workbench Recomposition And Legacy Route Cutover

Goal:

Recompose workbench as pure aggregation and cut over legacy routes where approved.

### Implementation Prompt

```text
你是 Phase 14 Implementation：Workbench Recomposition And Legacy Route Cutover。

目标：将 research workbench 重组为纯 aggregation consumer，并执行 approved legacy route cutover。

实现要求：
- workbench 只消费 stable task/report/risk/strategy/market contracts。
- 不写 domain facts，不触发业务 commands，除 approved navigation/prefill 外不传 truth。
- 执行 route alias/migration/deprecation，仅限已批准范围。
- 更新 frontend API clients。
- 增加 workbench authority guard、route contract tests、frontend build。
- 写 phase-014-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 14 Acceptance。

验收重点：
- workbench 是否纯展示聚合。
- legacy route cutover 是否有兼容/回滚。
- frontend API 是否不使用 deprecated truth source。
- contract/static guard 是否通过。
```

## Phase 15 - Frontend Production Workflows

Goal:

Complete production frontend auth, role-aware operations, data-source ops and AI workflow ops.

### Implementation Prompt

```text
你是 Phase 15 Implementation：Frontend Production Workflows。

目标：完成生产前端工作流。

实现要求：
- 登录/session/JWT-aware UX 或 approved SSO UX。
- role-aware navigation/action states 反映 backend authority。
- data source operations：sync、diagnose、history、deadletter、retry。
- AI workflow operations：trace、resume、rerun、human review queue。
- report/risk/strategy review UX：diff、evidence trace、approval/rejection、export。
- 不让 frontend gating 成为权限 truth。
- 增加 frontend build、关键 e2e 或 static checks。
- 写 phase-015-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 15 Acceptance。

验收重点：
- 前端是否完整覆盖 auth/data/AI/review workflows。
- UI gating 是否只是 affordance。
- 后端拒绝行为是否正确显示。
- 空态、错误态、失败恢复是否可用。
- npm build/e2e/static guard 是否通过。
```

## Phase 16 - Deployment, Observability And Resilience

Goal:

Complete repeatable deployment, observability and resilience baseline.

### Implementation Prompt

```text
你是 Phase 16 Implementation：Deployment, Observability And Resilience。

目标：完成生产化部署和可观测性基础。

实现要求：
- docker compose/profile 包含 gateway、Java services、Python workers、MySQL、Redis、Kafka、config/discovery 如需。
- 加健康检查、structured logs、trace id、metrics、dashboard/alert baseline。
- 加 rate limit、circuit breaker、Kafka lag handling、Redis degradation、workflow timeout policy。
- 提供 runbook 和 startup/shutdown steps。
- 增加 smoke tests 或 compose verification。
- 写 phase-016-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 16 Acceptance。

验收重点：
- compose/profile 是否可重复启动。
- health/metrics/logs/trace 是否可用。
- resilience 策略是否不会破坏业务 contract。
- smoke tests/runbook 是否可信。
```

## Phase 17 - End-To-End, Load And Recovery Testing

Goal:

Add complete e2e, load, failure and recovery test coverage.

### Implementation Prompt

```text
你是 Phase 17 Implementation：End-To-End, Load And Recovery Testing。

目标：建立完整测试闭环。

实现要求：
- e2e 覆盖 task creation -> AI workflow -> report/risk/strategy -> review/audit。
- load tests 覆盖 Kafka backlog、AI workflow concurrency、frontend critical flows。
- failure tests 覆盖 service restart、Kafka down、Redis down、AI timeout、checkpoint recovery。
- 增加测试数据/seed/run scripts。
- CI 或本地验证命令清晰。
- 写 phase-017-implementation.md。
```

### Acceptance Prompt

```text
你是 Phase 17 Acceptance。

验收重点：
- e2e 是否覆盖主业务链。
- load/failure/recovery 是否覆盖关键风险。
- 测试是否可重复运行。
- 失败报告是否清楚。
```

## Phase 18 - Production Readiness And Final Release

Goal:

Finalize full project release package, documentation, runbooks and acceptance.

### Implementation Prompt

```text
你是 Phase 18 Implementation：Production Readiness And Final Release。

目标：完成完整项目最终交付。

实现要求：
- 完成 final release checklist。
- 整理架构图、服务清单、API contract、Kafka topics、DB schema、权限矩阵、运行手册。
- 准备 demo/prod profiles、seed data、演示脚本。
- 汇总 open risks，确认哪些是 accepted residual risk。
- 运行全量 verification。
- 写 phase-018-implementation.md 和 final release notes。
```

### Acceptance Prompt

```text
你是 Phase 18 Acceptance：Production Readiness And Final Release。

验收重点：
- 完整项目目标是否满足。
- 所有主路径是否可运行。
- 权限、数据、AI、报告、风险、策略、审计、部署、测试是否闭环。
- open risks 是否可接受并记录。
- 全量验证是否通过。

输出最终 approve / require fixes / block。
```

## Recommended Execution Order

Run phases in order from Phase 1 to Phase 18.

Only reorder when:

- A phase is blocked by missing prerequisite.
- The user explicitly prioritizes another track.
- A severe main-path breakage appears.

Do not add more harness-only phases. If state files need syncing, do it as a small part of the current phase handoff or a single optional cleanup, not as a repeated process.
