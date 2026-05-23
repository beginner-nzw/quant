# Window Protocol

本文件定义本项目使用 Codex 推进工程时的窗口分工。

## Core Rule

一个窗口只承担一个角色。

窗口之间不依赖聊天记忆交接，只通过 `docs/harness` 工件交接。

当前对话窗口不是 Window 0。当前对话窗口是 Bootstrap Harness Window，职责是在 Window 0 开始工作之前，把规则、状态机、工件和提示词准备好。

## Bootstrap Harness Window

Status: completed.

Responsibilities:

- Read project documents and enough code facts.
- Identify current drift.
- Create `docs/harness` baseline artifacts.
- Define Window 0 as a constrained state machine plus human approval point.
- Provide prompts for Window 0-4.
- Do not modify business code.

Outputs:

- `00-project-charter.md`
- `01-current-architecture.md`
- `02-authority-matrix.md`
- `03-host-ownership.md`
- `04-contract-map.md`
- `05-transition-lifetime.md`
- `06-debt-register.md`
- `07-phase-backlog.md`
- `08-eval-checklist.md`
- `09-window-protocol.md`
- `10-steering-state-machine.md`
- `state/current-state.md`
- `handoffs/phase-000-harness-baseline.md`
- `prompts/window-0-steering.md`
- `prompts/window-1-phase-architect.md`
- `prompts/window-2-implementer.md`
- `prompts/window-3-review-eval.md`
- `prompts/window-4-handoff.md`

## Required Sequence

```text
Bootstrap Harness Window
  -> Window 0: Steering
  -> Human approval
  -> Window 1: Phase Architect
  -> Human approval
  -> Window 2: Backend or Frontend Implementer
  -> Window 3: Review / Eval
  -> Window 4: Handoff
  -> Window 0: Steering
```

Window 0 可以在每个阶段收尾后再次启动，但它不能自由决定方向。它只能根据 `10-steering-state-machine.md` 的固定规则提出下一阶段候选目标，并等待人类批准。

## Repeatable Startup Rule

The user should not need to ask for a custom prompt after every phase.

Each window prompt must be reusable. Each new window must restore context from `docs/harness`, not from chat memory.

For every new cycle:

1. Open a new Window 0.
2. Paste `docs/harness/prompts/window-0-steering.md`.
3. Window 0 reads `docs/harness/state/current-state.md`.
4. Window 0 discovers the latest `docs/harness/handoffs/phase-*-final.md`.
5. Window 0 reads the matching steering, architect, implementation, review and final handoffs.
6. Window 0 proposes the next phase and waits for human approval.

The only manual input required from the user is approval or override. The user should not need to summarize the previous phase.

## Window 0 - Steering

Purpose:

- Decide overall direction, phase order and next target candidate.
- Operate as constrained state machine, not as a free-form superior agent.

Responsibilities:

- Read `docs/harness/state/current-state.md`.
- Discover and read latest handoff files.
- Score candidate next phases using `10-steering-state-machine.md`.
- Propose exactly one next phase plus one fallback.
- Ask for human approval before Window 1 starts.

Forbidden:

- Do not write business code.
- Do not skip Window 1.
- Do not approve its own decision.
- Do not invent new product features while authority or contract drift remains.

Output:

- `docs/harness/handoffs/steering-decision-<phase>.md`
- Updated `docs/harness/state/current-state.md` if the user approved the decision.

## Window 1 - Phase Architect

Purpose:

- Convert the approved next target into an implementation-ready phase handoff.

Responsibilities:

- Define belongs, authority, contract and behavior constraints.
- Identify files likely to change.
- Define forbidden changes.
- Define verification commands.
- Ask for human approval before Window 2 starts.

Forbidden:

- Do not write business code.
- Do not expand scope beyond the approved Steering target.

Output:

- `docs/harness/handoffs/phase-<n>-architect.md`

## Window 2 - Backend or Frontend Implementer

Purpose:

- Implement only the approved phase handoff.

Responsibilities:

- Read the Steering decision and Phase Architect handoff.
- Record git baseline before edits.
- Modify only allowed files.
- Preserve contracts unless breaking change is approved.
- Run required verification.
- Write implementation handoff.
- Commit only the files changed by this Window 2 pass.

Forbidden:

- Do not choose the next phase.
- Do not change architecture policy.
- Do not introduce unregistered helper, adapter, fallback or bridge.
- Do not use `git add .`.
- Do not commit unrelated dirty files.

Output:

- Code changes.
- Test results.
- `docs/harness/handoffs/phase-<n>-implementation.md`

Multiple implementers:

- A phase may use Backend, Frontend or Python implementer windows.
- Multiple Window 2 instances are allowed only if Window 1 explicitly partitions file ownership.
- If file ownership is not partitioned, run one Window 2 implementation window only.
- Each implementer writes its own implementation handoff if multiple implementers are used.

## Window 3 - Review / Eval

Purpose:

- Independently evaluate whether implementation drifted from belongs, authority, contract or behavior.

Responsibilities:

- Review git diff and relevant code.
- Run or inspect required checks.
- Produce findings ordered by severity.
- Decide approve / require fixes / block.

Forbidden:

- Do not continue implementation unless explicitly asked.
- Do not move to next phase.

Output:

- `docs/harness/handoffs/phase-<n>-review.md`

If review returns `require fixes`:

- Do not open Window 4.
- Return to Window 2 for a Fix Pass.
- Window 2 uses the same fixed prompt and automatically reads the latest `require fixes` review.
- Window 2 writes `phase-<n>-fix-<k>-implementation.md`.
- Window 3 uses the same fixed prompt and automatically reads the latest fix implementation.
- Window 3 re-reviews and writes `phase-<n>-review-fix-<k>.md`.
- Repeat until the latest review is `approve` or `block`.

## Window 4 - Handoff

Purpose:

- Freeze the phase result and feed it back to Window 0.

Responsibilities:

- Summarize what changed.
- Update debt, backlog, transition lifetime and current state.
- Mark the phase completed or blocked.
- Prepare enough state for the next Window 0 to start without chat context.
- Commit only the final handoff and harness state files changed by Window 4.

Forbidden:

- Do not implement new code.
- Do not select the next phase.
- Do not commit business code by default.

Output:

- `docs/harness/handoffs/phase-<n>-final.md`
- Updated harness state files.

## Do Not Skip

Window 1 is mandatory before any implementation. Window 3 and Window 4 are mandatory before Window 0 can select the next phase.
