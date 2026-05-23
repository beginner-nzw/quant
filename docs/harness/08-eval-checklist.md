# Eval Checklist

Use this checklist before accepting any code change.

## Review Order

```text
belongs -> authority -> contract -> behavior
```

## Belongs

- [ ] Does the new logic live in the formal host defined by `03-host-ownership.md`?
- [ ] If it lives in a transition host, is that transition listed in `05-transition-lifetime.md`?
- [ ] Did the change add a helper, adapter, bridge or fallback?
- [ ] If yes, does it include a retirement plan or display-only restriction?

## Authority

- [ ] Does the change introduce a second source of truth for any semantic in `02-authority-matrix.md`?
- [ ] Does a read-model become a command source?
- [ ] Does an aggregation view become business truth?
- [ ] Does frontend infer task/report/risk/strategy truth?
- [ ] Does Python fallback result hide its fallback status?

## Contract

- [ ] Does the endpoint belong to the contract class in `04-contract-map.md`?
- [ ] Does the response allow optional authority where authority should be stable?
- [ ] Does it create dual-surface contract for the same semantic?
- [ ] Does it push truth routing to frontend or Python?
- [ ] If endpoint paths change, was breaking change approved?

## Transition Lifetime

- [ ] Is any new temporary path registered in `05-transition-lifetime.md`?
- [ ] Does every transition have exit criteria?
- [ ] Is mock/demo/test behavior clearly marked?

## Behavior

- [ ] Does the code compile?
- [ ] Do relevant Maven tests pass?
- [ ] Does frontend type-check/build pass if frontend changed?
- [ ] Does Python compile/test pass if Python changed?
- [ ] Are error/empty/fallback cases still visible and auditable?

## Default Verification Commands

From `D:\projects\bussiness\quant-ai-platform\quant-services`:

```powershell
mvn -q test
```

From `D:\projects\bussiness\quant-ui`:

```powershell
npm run build
```

From `D:\projects\bussiness\quant-ai-platform\quant-ai-engine`:

```powershell
python -m compileall app
```

If `pytest` is installed:

```powershell
python -m pytest
```

