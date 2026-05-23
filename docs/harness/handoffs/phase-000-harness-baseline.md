# Phase 000 Handoff - Bootstrap Harness Baseline

## Status

Completed.

This handoff was produced before Window 0 starts. The current conversation is Bootstrap Harness Window, not Window 0.

## What Was Frozen

The current project is a working prototype/convergence-stage system, not the full microservice architecture originally planned.

Frozen current shape:

- `research-task-service`: task creation and dispatch outbox.
- `ai-orchestration-service`: AI callback consumers, task control/read-model, and multi-domain transition host.
- `quant-ai-engine`: Python LangGraph AI execution engine.
- `quant-ui`: frontend consumer.

## Main Drift Identified

1. `ai-orchestration-service` has absorbed many domains.
2. `TaskQueryController` exposes too many unrelated API surfaces.
3. `TaskQueryServiceImpl` mixes many read models and display aggregation logic.
4. `research-workbench` can be mistaken as authoritative truth.
5. JSON config is used as mutable runtime config.
6. Mock ingest and fallback paths are useful but need lifecycle control.

## Decisions

1. Do not immediately split microservices.
2. Do not continue feature expansion first.
3. Treat splitting API/controller surfaces inside the existing service as the first steering candidate, not as an already approved implementation task.
4. Keep URLs stable unless human approval is given.
5. Use `docs/harness/08-eval-checklist.md` before accepting implementation.
6. Window 0 must operate as a constrained state machine plus human approval point.

## Next Phase

Start Window 0 - Steering.

Window 0 should evaluate the backlog and propose the next phase candidate. The bootstrap recommendation is Phase 001 - Split Controller Surface Inside `ai-orchestration-service`, but Window 0 must still produce a steering decision and wait for human approval.

## Do Not Reopen Without Human Approval

- Whether `research-workbench` is a SoT. It is not.
- Whether frontend may define business truth. It may not.
- Whether mock ingest is production ingestion. It is not.
- Whether `ai-orchestration-service` current scope is final. It is not.
