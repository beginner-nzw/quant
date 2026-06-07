Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$composeDir = Join-Path $PSScriptRoot "..\docker\compose"
$runtimeNetwork = "compose_quant_net"
$edgeContainers = @("quant-ai-engine-worker", "quant-ai-engine-consumer", "quant-gateway")

function Get-ContainerNetworks {
  param([string]$Name)

  $exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $Name } | Select-Object -First 1
  if ([string]::IsNullOrWhiteSpace($exists)) {
    return @()
  }

  $json = docker inspect $Name --format "{{json .NetworkSettings.Networks}}"
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($json)) {
    return @()
  }

  $networks = $json | ConvertFrom-Json
  if ($null -eq $networks) {
    return @()
  }

  return @($networks.PSObject.Properties.Name)
}

function Write-EdgeNetworkState {
  param([string]$Prefix)

  foreach ($container in $edgeContainers) {
    $networks = @(Get-ContainerNetworks -Name $container)
    if ($networks.Count -eq 0) {
      Write-Host "  $Prefix ${container}: (missing)"
    }
    else {
      Write-Host "  $Prefix ${container}: $($networks -join ', ')"
    }
  }
}

Write-Host "[phase017-recover] Checking Docker engine..."
docker version --format "{{.Server.Version}}" | Out-Host
if ($LASTEXITCODE -ne 0) {
  throw "Docker engine is not reachable. Start Docker Desktop before recovering Phase 17 runtime."
}

Write-Host "[phase017-recover] Edge container networks before recovery:"
Write-EdgeNetworkState -Prefix "before"

Push-Location $composeDir
try {
  Write-Host "[phase017-recover] Removing stale Phase 17 edge services..."
  $existingEdgeContainers = @(docker ps -a --format "{{.Names}}" | Where-Object { $edgeContainers -contains $_ })
  if ($existingEdgeContainers.Count -gt 0) {
    docker rm -f $existingEdgeContainers | Out-Host
  }
  else {
    Write-Host "  no stale edge containers found"
  }

  Write-Host "[phase017-recover] Rebuilding AI engine worker, AI engine consumer and gateway on $runtimeNetwork..."
  docker compose --profile runtime up -d --build --no-deps ai-engine-worker ai-engine-consumer gateway
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
finally {
  Pop-Location
}

Write-Host "[phase017-recover] Edge container networks after recovery:"
Write-EdgeNetworkState -Prefix "after"

Write-Host "[phase017-recover] Running preflight..."
& "$PSScriptRoot\run-phase017-preflight.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
