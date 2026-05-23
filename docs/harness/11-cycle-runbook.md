# Cycle Runbook

This file is the human operator runbook.

The user should not need a custom prompt after every phase. Use the same five prompts repeatedly.

## Normal Cycle

1. Open a new Window 0.
2. Paste `docs/harness/prompts/window-0-steering.md`.
3. Wait for Window 0 to write `docs/harness/handoffs/steering-decision-phase-<n>.md`.
4. Approve or override the steering decision.
5. Open a new Window 1.
6. Paste `docs/harness/prompts/window-1-phase-architect.md`.
7. Wait for Window 1 to write `docs/harness/handoffs/phase-<n>-architect.md`.
8. Approve or reject implementation.
9. Open Window 2 implementer window or windows according to the architect handoff.
10. Paste `docs/harness/prompts/window-2-implementer.md`.
11. Window 2 verifies and commits only its own implementation files.
12. After implementation commit, open Window 3.
13. Paste `docs/harness/prompts/window-3-review-eval.md`.
14. If Window 3 requires fixes, return to Window 2.
15. If Window 3 approves, open Window 4.
16. Paste `docs/harness/prompts/window-4-handoff.md`.
17. Window 4 commits only its own final harness updates.
18. After Window 4 writes final handoff and updates current state, return to step 1.

## Fix Loop

If Window 3 returns `require fixes`, do not open Window 4.

Use the same Window 2 prompt. Do not write a custom fix prompt.

1. Open a new Window 2.
2. Paste `docs/harness/prompts/window-2-implementer.md`.
3. Window 2 automatically detects the latest `require fixes` review and writes `docs/harness/handoffs/phase-<n>-fix-<k>-implementation.md`.
4. Window 2 verifies and commits only its own fix files.
5. Open a new Window 3.
6. Paste `docs/harness/prompts/window-3-review-eval.md`.
7. Window 3 automatically detects the latest fix pass and writes `docs/harness/handoffs/phase-<n>-review-fix-<k>.md`.
8. If approved, open Window 4.
9. If still `require fixes`, repeat the loop with `<k+1>`.

Do not overwrite the original implementation or review handoff. They are part of the audit trail.

## What The User Must Approve

The user approves:

- Window 0 steering decision.
- Window 1 implementation scope.
- Any breaking change.
- Any URL path change.
- Any transition host becoming final architecture.
- Any residual risk accepted after review.

The user does not need to approve every fix pass if Window 3 findings are inside the original Window 1 scope. The user must approve if the fix requires new scope, breaking changes, URL changes or new transition paths.

## Commit Policy

Commit after each code-changing Window 2 completes successfully.

Window 2 commits:

- Initial implementation code changes.
- Fix Pass code changes.
- The implementation or fix handoff for that Window 2 pass.

Window 4 commits:

- Final phase handoff.
- Current state updates.
- Debt/backlog/transition harness updates made by Window 4.

Rules:

- Never use `git add .`.
- Stage only files changed by the current window.
- Do not commit unrelated dirty files from other windows or the user.
- If verification fails, do not commit.
- If file ownership is ambiguous, stop and ask the user.
- Window 3 review handoffs may remain uncommitted until Window 4 unless the user wants a stricter commit-after-every-handoff policy.

## What The User Should Not Need To Do

The user should not need to:

- Summarize the previous phase for Window 0.
- Tell Window 0 which final handoff to read.
- Tell Window 2 which fix pass to run.
- Tell Window 3 which fix pass to re-review.
- Decide the next phase manually.
- Let implementation windows continue into the next phase.

Window 0 must recover context from:

- `docs/harness/state/current-state.md`
- latest `docs/harness/handoffs/phase-*-final.md`
- matching steering / architect / implementation / review handoffs
- `docs/harness/10-steering-state-machine.md`

## If Something Feels Off

Stop the cycle and open Window 0 with the normal prompt.

Ask it to report:

- current state file content
- latest final handoff discovered
- missing handoff files
- open blockers
- whether it can safely choose the next phase

Do not continue implementation until Window 0 produces a fresh steering decision and the user approves it.
