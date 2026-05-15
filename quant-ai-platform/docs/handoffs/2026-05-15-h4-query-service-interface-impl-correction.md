# Handoff: H4 Query Service Interface / Impl Correction

Date: 2026-05-15
Status: implemented

## 1. Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/transition-lifetime.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/domain-split-execution-plan.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-h4-pre-gate-completion-review.md`
- `docs/handoffs/2026-05-15-h4-1-report-query-service-split.md`
- `docs/handoffs/2026-05-15-h4-2-risk-query-service-split.md`
- `docs/handoffs/2026-05-15-h4-3-strategy-query-service-split.md`
- `docs/handoffs/2026-05-15-h4-4-market-query-service-split.md`
- `docs/handoffs/2026-05-15-h4-5-audit-config-dashboard-query-split.md`

H1 owner:

- AI Orchestration Service.

Allowed work:

1. Check and correct only the H4 query service interface/implementation shape.
2. Preserve all HTTP API, DTO, mapper, SQL, Redis, Kafka, Python, frontend,
   config, and business semantics.
3. Record this structural correction handoff.
4. Run verification.
5. Commit the allowed H4 correction scope if verification passes.

Hard exclusions:

- no cancel runtime signal implementation;
- no retry dispatch work;
- no projection/idempotency/stale-message work;
- no fallback backfill, deletion, or retirement;
- no T006/T007 continuation;
- no report version snapshots or `version_no` semantic change;
- no frontend, Python, SQL/config, Java common model/messaging, DTO, VO,
  mapper, entity, or test changes;
- no `TaskQueryServiceImpl` split;
- no controller route, HTTP method, request parameter, request body, response
  DTO, or permission-check changes.

## 2. Changed Files

Business-code inspection result:

- No additional Java business-code correction was required in this window. The
  H4 query service files already had the required interface/implementation
  structure when inspected.

Correction handoff added:

- `docs/handoffs/2026-05-15-h4-query-service-interface-impl-correction.md`

Existing H4 query split files expected to be included in the final commit if
still unstaged and inside the allowed scope:

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/controller/TaskQueryController.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/ReportQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/RiskQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/StrategyQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/MarketQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/AuditConfigDashboardQueryService.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/ReportQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/RiskQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/StrategyQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/MarketQueryServiceImpl.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/AuditConfigDashboardQueryServiceImpl.java`

Unrelated pre-existing worktree changes observed and not touched by this
correction window:

- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchTaskMapper.java`
- `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskRetryServiceImpl.java`
- `quant-services/quant-common/quant-common-messaging/pom.xml`
- `quant-services/quant-common/quant-common-model/pom.xml`
- `quant-ai-engine/tests/test_ai_task_message_contract.py`
- `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskRetryServiceTests.java`
- `quant-services/quant-common/quant-common-messaging/src/test/`
- `quant-services/quant-common/quant-common-model/src/test/`

## 3. Interface / Impl Compliance Evidence

Report query:

- `service/ReportQueryService.java` declares `public interface ReportQueryService`.
- `service/impl/ReportQueryServiceImpl.java` declares `@Service` and
  `public class ReportQueryServiceImpl implements ReportQueryService`.
- `TaskQueryController` depends on `private final ReportQueryService
  reportQueryService`.

Risk query:

- `service/RiskQueryService.java` declares `public interface RiskQueryService`.
- `service/impl/RiskQueryServiceImpl.java` declares `@Service` and
  `public class RiskQueryServiceImpl implements RiskQueryService`.
- `TaskQueryController` depends on `private final RiskQueryService
  riskQueryService`.

Strategy query:

- `service/StrategyQueryService.java` declares
  `public interface StrategyQueryService`.
- `service/impl/StrategyQueryServiceImpl.java` declares `@Service` and
  `public class StrategyQueryServiceImpl implements StrategyQueryService`.
- `TaskQueryController` depends on `private final StrategyQueryService
  strategyQueryService`.

Market query:

- `service/MarketQueryService.java` declares `public interface MarketQueryService`.
- `service/impl/MarketQueryServiceImpl.java` declares `@Service` and
  `public class MarketQueryServiceImpl implements MarketQueryService`.
- `TaskQueryController` depends on `private final MarketQueryService
  marketQueryService`.

Audit/config/dashboard query:

- `service/AuditConfigDashboardQueryService.java` declares
  `public interface AuditConfigDashboardQueryService`.
- `service/impl/AuditConfigDashboardQueryServiceImpl.java` declares `@Service`
  and `public class AuditConfigDashboardQueryServiceImpl implements
  AuditConfigDashboardQueryService`.
- `TaskQueryController` depends on `private final
  AuditConfigDashboardQueryService auditConfigDashboardQueryService`.

Controller impl-dependency check:

- No import from `com.quant.aiorchestrator.service.impl.*QueryServiceImpl` was
  found in `TaskQueryController`.
- The controller injects H4 query dependencies by service interface type.

## 4. API Invariance Evidence

- No controller route annotation, HTTP method, request binding, request body,
  response DTO type, or permission check was changed in this correction window.
- No DTO, VO, mapper, entity, SQL, Redis, Kafka, Python, frontend, config, Java
  common model, Java common messaging, or test files were edited by this
  correction window.
- Existing query implementation methods remain structural delegates to the
  pre-existing service methods recorded in H4-1 through H4-5.
- `TaskQueryServiceImpl` was not split or edited.

## 5. Verification

Command:

```text
mvn -pl quant-business/ai-orchestration-service -am "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Result:

```text
BUILD SUCCESS
ai-orchestration-service: Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
Reactor summary: quant-services and required common modules SUCCESS; ai-orchestration-service SUCCESS
Finished at: 2026-05-15T16:52:51+08:00
```

Verification notes:

- Maven emitted existing-style warnings while writing some common-module
  compiler/surefire status files under `target`, including access-denied
  warnings for some common-module surefire reports.
- The reactor still completed with `BUILD SUCCESS`.
- The AI Orchestration Service module compiled and its 41 existing tests
  completed successfully.

## 6. Commit

Commit message:

```text
chore(h4): normalize query services to interface implementations
```

Commit hash:

```text
pending until commit creation; final execution output records the exact hash
```

## 7. Residual Risk

- The H4 query split remains a structural delegate split. Method bodies remain
  in the existing underlying services, preserving behavior and respecting the
  exclusion against splitting `TaskQueryServiceImpl`.
- Unrelated pre-existing retry/common/Python/test worktree changes remain
  unstaged and unmodified by this correction window.
