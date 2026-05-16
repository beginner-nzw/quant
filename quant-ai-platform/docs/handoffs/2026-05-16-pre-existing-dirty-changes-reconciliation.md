# Pre-Existing Dirty Changes Reconciliation

Date: 2026-05-16
Status: documentation-only reconciliation; no implementation authorized

## 1. Session Contract

Input authority:

- `docs/code-domain-boundary-map.md`
- `docs/harness-authority-contract.md`
- `docs/harness-session-protocol.md`
- `docs/eval-harness.md`
- `docs/governance-orchestrator-protocol.md`
- `docs/handoffs/2026-05-15-contract-parity-guardrail-implementation-note.md`
- `docs/handoffs/2026-05-15-task-retry-dispatch-consistency-implementation-note.md`
- `docs/handoffs/2026-05-16-post-h4-business-backlog-slicing.md`

Allowed work:

1. Audit current dirty and untracked changes only.
2. Read `git status --short`, `git diff --name-only`, `git diff --stat`, and
   targeted diffs or file contents needed for classification.
3. Do not modify business code, tests, config, SQL, Python, Java, or frontend
   files.
4. Do not stage unrelated code or test changes.
5. Add this reconciliation handoff and, if it is the only staged file, commit it.

Hard exclusions:

- No Java, Python, Vue, SQL, config, or test edits.
- No formatting.
- No tests added or run.
- No file deletion.
- No revert, reset, or checkout.
- No retry, contract parity, cancel, projection, Redis, persistence, fallback,
  T006/T007, frontend, or report snapshot implementation.
- No staging of dirty code or test changes.
- This classification is not implementation authorization.

Expected output:

- Dirty changes inventory.
- Per-file classification.
- Recommended next action.
- Residual risk.
- Commit hash if this documentation-only handoff is committed.

## 2. Raw Status Summary

Commands read:

```text
git -c safe.directory=D:/projects/bussiness status --short
git -c safe.directory=D:/projects/bussiness diff --name-only
git -c safe.directory=D:/projects/bussiness diff --stat
git -c safe.directory=D:/projects/bussiness status --short --untracked-files=all
```

Raw `status --short` summary:

```text
 M quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchTaskMapper.java
 M quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskRetryServiceImpl.java
 M quant-ai-platform/quant-services/quant-common/quant-common-messaging/pom.xml
 M quant-ai-platform/quant-services/quant-common/quant-common-model/pom.xml
?? quant-ai-platform/quant-ai-engine/tests/test_ai_task_message_contract.py
?? quant-ai-platform/quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskRetryServiceTests.java
?? quant-ai-platform/quant-services/quant-common/quant-common-messaging/src/test/java/com/quant/common/messaging/KafkaTopicConstantsContractTests.java
?? quant-ai-platform/quant-services/quant-common/quant-common-model/src/test/java/com/quant/quantcommonmodel/AiTaskMessageContractTests.java
```

Raw tracked diff stat:

```text
ResearchTaskMapper.java       |  8 ++++++++
TaskRetryServiceImpl.java     | 10 ++++++++--
quant-common-messaging/pom.xml|  8 ++++++++
quant-common-model/pom.xml    |  8 +++++++-
4 files changed, 31 insertions(+), 3 deletions(-)
```

Tracked diff name-only output contains only the four modified tracked files
listed above. Untracked test files are visible only through the status inventory.

## 3. Classification Legend

1. Historical work appears implemented and recorded, but not committed in this
   checkout.
2. Unrelated to the current governance chain; keep unstaged.
3. Risk unclear; requires human or Orchestrator decision.
4. Future candidate handoff only.

## 4. Per-File Classification

| File | Observed change | Likely source | Classification | Safe to commit now? | Recommendation |
| --- | --- | --- | --- | --- | --- |
| `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/mapper/ResearchTaskMapper.java` | Adds conditional retry update guards on prior status, prior retry count, and non-deleted row. | `2026-05-15-task-retry-dispatch-consistency-implementation-note.md` lists this exact file and describes the same conditional update. | 1 | No. Code staging is excluded in this window. | Leave unstaged. A later Orchestrator-authorized cleanup window may commit it with the rest of the retry historical set or explicitly decide otherwise. |
| `quant-services/quant-business/ai-orchestration-service/src/main/java/com/quant/aiorchestrator/service/impl/TaskRetryServiceImpl.java` | Computes current retry count, passes expected status/count to mapper, and stops dispatch when conditional update affects no row. | Retry dispatch historical note lists this exact file and describes this behavior. | 1 | No. Code staging is excluded in this window. | Leave unstaged with the retry historical set. |
| `quant-services/quant-business/ai-orchestration-service/src/test/java/com/quant/aiorchestrationservice/TaskRetryServiceTests.java` | Adds focused retry tests using local fakes for shared dispatch contract, produced log, cache invalidation, and stale conditional update stop. | Retry dispatch historical note lists this exact file and verification command. | 1 | No. Test staging is excluded in this window. | Leave unstaged with the retry historical set. |
| `quant-services/quant-common/quant-common-model/pom.xml` | Adds `spring-boot-starter-test` as test dependency and newline normalization. | Contract parity historical note lists this exact file. | 1 | No. Config/POM staging is excluded in this window except this docs handoff. | Leave unstaged with the contract parity historical set. |
| `quant-services/quant-common/quant-common-model/src/test/java/com/quant/quantcommonmodel/AiTaskMessageContractTests.java` | Adds reflection tests locking Java AI task message envelope and payload field names. | Contract parity historical note lists this exact file and matching guardrail. | 1 | No. Test staging is excluded in this window. | Leave unstaged with the contract parity historical set. |
| `quant-services/quant-common/quant-common-messaging/pom.xml` | Adds `spring-boot-starter-test` as test dependency. | Contract parity historical note lists this exact file. | 1 | No. Config/POM staging is excluded in this window except this docs handoff. | Leave unstaged with the contract parity historical set. |
| `quant-services/quant-common/quant-common-messaging/src/test/java/com/quant/common/messaging/KafkaTopicConstantsContractTests.java` | Adds topic-name contract test for the four AI task Kafka topics. | Contract parity historical note lists this exact file and matching guardrail. | 1 | No. Test staging is excluded in this window. | Leave unstaged with the contract parity historical set. |
| `quant-ai-engine/tests/test_ai_task_message_contract.py` | Adds Python Pydantic mirror tests for envelope, payload field sets, and required-field policy. | Contract parity historical note lists this exact file and matching Python guardrail. | 1 | No. Python test staging is excluded in this window. | Leave unstaged with the contract parity historical set. |

No dirty file was classified as category 2, 3, or 4 in this pass. The current
inventory maps cleanly to the two historical implementation notes.

## 5. Boundary And Source Mapping

Retry dispatch set:

- H1 owner: AI Orchestration Service.
- Touched surfaces: `POST /api/tasks/{taskId}/retry`, task mapper update,
  `ai.task.dispatch` send path, message-log side effect ordering, Redis cache
  invalidation as existing behavior.
- Source mapping: `2026-05-15-task-retry-dispatch-consistency-implementation-note.md`.
- Current status: appears to be historical work that should have been committed
  or otherwise reconciled, but this window has no authority to stage it.

Contract parity set:

- H1 owners: Common Model for shared message payload contracts, Common Messaging
  for topic constants, AI Engine for Python message mirror tests.
- Touched surfaces: Java message field-name guardrails, Kafka topic-name
  guardrail, Python Pydantic mirror test.
- Source mapping: `2026-05-15-contract-parity-guardrail-implementation-note.md`.
- Current status: appears to be historical work that should have been committed
  or otherwise reconciled, but this window has no authority to stage it.

## 6. Whether Safe To Commit Now

Code/test/config dirty changes:

- Safe to commit now: no.
- Reason: this execution window explicitly excludes staging Java, Python, test,
  config, and POM changes. The files are classifiable, but classification is not
  implementation or cleanup authorization.

This reconciliation document:

- Safe to commit now: yes, if `git status` confirms the only staged file is this
  handoff.
- Reason: the prompt authorizes adding and committing this documentation-only
  handoff.

## 7. Recommended Next Window

Recommended candidate:

- Historical dirty changes commit/reconciliation window.

Candidate scope:

- Decide whether the exact retry historical set and contract parity historical
  set should be committed as one or two historical commits, kept dirty, or
  superseded by a correction plan.
- If commit is authorized, stage only the exact files listed in the relevant
  historical note and re-run or explicitly waive the historical verification
  commands.

Candidate hard exclusions:

- No broad `git add .`.
- No revert, reset, checkout, or deletion unless separately and explicitly
  authorized.
- No new implementation beyond reconciling the already-recorded historical
  work.
- No fallback, T006/T007, frontend, report snapshot, SQL/config ownership,
  Python DB ownership, or H1 boundary reopening.

This recommendation is candidate-only. It is not authorization for the next
window.

## 8. Residual Risk

- The working tree still contains uncommitted functional code and tests that
  match historical notes. Future execution windows may accidentally rely on
  these files unless they are explicitly reconciled.
- The classification did not run verification commands. It relies on the
  recorded historical handoff verification plus source/diff inspection in this
  documentation-only window.
- POM changes are categorized with contract parity because they enable the
  untracked Java contract tests. They remain unstaged because this window cannot
  commit code/test/config changes.
- Line-ending warnings appeared for tracked modified files; no formatting or
  normalization was performed.

## 9. Stop Decision

Stop after this reconciliation handoff.

Do not stage or commit retry, common, message-contract, Java, Python, POM, or
test dirty changes in this window. Do not treat this document as permission to
continue implementation or cleanup.
