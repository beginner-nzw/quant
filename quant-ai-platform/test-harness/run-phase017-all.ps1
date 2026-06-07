Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Push-Location $PSScriptRoot\..
try {
  node .\test-harness\scripts\phase017-runtime-preflight.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  node .\test-harness\scripts\phase017-e2e-check.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  node .\test-harness\scripts\phase017-load-check.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  node .\test-harness\scripts\phase017-failure-recovery-check.mjs
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
  Pop-Location
}
