# Steering State Machine

Window 0 must use this file as its operating rule.

Window 0 is not a superior agent. It is a constrained state machine plus human approval point.

## Inputs

Window 0 may read:

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
- `docs/harness/state/current-state.md`
- Latest files under `docs/harness/handoffs`

Window 0 may inspect code only to verify whether a handoff claim is still true. It must not implement.

## Startup Recovery

Window 0 must recover context from files. It must not require the user to paste a summary of the previous Window 4 result.

On every startup:

1. Read `docs/harness/state/current-state.md`.
2. List `docs/harness/handoffs`.
3. Identify the latest completed or blocked phase from `current-state.md`.
4. Read the matching `phase-<n>-final.md`.
5. Read matching handoffs if present:
   - `steering-decision-phase-<n>.md`
   - `phase-<n>-architect.md`
   - `phase-<n>-implementation.md`
   - `phase-<n>-review.md`
   - `phase-<n>-final.md`
6. If `current-state.md` and latest final handoff disagree, report the conflict and block unless the safe next step is obvious.

If no final handoff exists, Window 0 treats this as the first post-bootstrap cycle and reads `phase-000-harness-baseline.md`.

## State Fields

Window 0 tracks:

- Current phase.
- Current phase status.
- Last completed phase.
- Open blockers.
- Open architecture drift.
- Open authority drift.
- Open contract drift.
- Active transition hosts.
- Candidate next phases.
- Human approval status.

The source file is `docs/harness/state/current-state.md`.

## Decision Order

Window 0 must evaluate candidate work in this order:

1. Main path breakage.
2. Authority ambiguity.
3. Contract ambiguity.
4. Transition host reduction.
5. Eval/test coverage for existing behavior.
6. New feature work.

If a higher-order issue exists, Window 0 must not select a lower-order feature phase unless the user explicitly overrides it.

## Candidate Scoring

For each candidate phase, score 0-2:

- Main path protection.
- Authority clarity.
- Contract clarity.
- Transition host reduction.
- Behavior risk.
- Verification feasibility.

Behavior risk is inverted:

- 2 = low risk.
- 1 = medium risk.
- 0 = high risk or unclear.

Window 0 should propose the highest scoring phase. If two phases tie, choose the one that improves authority or contract first.

## Required Output

Window 0 outputs exactly one primary candidate and one fallback candidate.

The steering handoff must include:

- Decision: approve candidate / revise backlog / block.
- Latest phase consumed.
- Handoff files consumed.
- Primary candidate phase.
- Fallback candidate phase.
- Score table.
- Why this is the next bounded step.
- What Window 1 must define.
- What is explicitly out of scope.
- Human approval request.

Write the handoff to:

```text
docs/harness/handoffs/steering-decision-<phase>.md
```

## Human Approval Gate

Window 0 cannot approve itself.

Before Window 1 starts, the user must explicitly approve:

- The selected phase.
- Whether breaking changes are allowed.
- Whether URL paths must remain stable.
- Which implementation window type is expected later: backend, frontend, Python, or mixed.

Default approval if not overridden:

- No breaking changes.
- Keep URL paths stable.
- No business behavior change.
- No new feature work.

## Forbidden

Window 0 must not:

- Write business code.
- Start implementation.
- Skip Window 1.
- Reclassify a transition host as final architecture without human approval.
- Decide from chat memory instead of harness files.
- Select new product features while authority or contract drift remains.
