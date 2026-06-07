param(
    [string]$ComposeFile = "quant-ai-platform/docker/compose/docker-compose.yml",
    [switch]$RuntimeChecks
)

$ErrorActionPreference = "Stop"

$requiredServices = @(
    "gateway",
    "ai-orchestration-service",
    "research-task-service",
    "ai-engine-worker",
    "mysql",
    "redis",
    "zookeeper",
    "kafka",
    "kafka-init",
    "nacos",
    "prometheus",
    "alertmanager",
    "grafana",
    "kafka-exporter",
    "redis-exporter",
    "nginx-exporter"
)

$profileMatrix = @(
    @{
        Name = "infra";
        Profiles = @("infra");
        Required = @("mysql", "redis", "zookeeper", "kafka", "kafka-init", "nacos")
    },
    @{
        Name = "runtime";
        Profiles = @("runtime");
        Required = @("gateway", "ai-orchestration-service", "research-task-service", "ai-engine-worker", "mysql", "redis", "zookeeper", "kafka", "kafka-init", "nacos")
    },
    @{
        Name = "observability";
        Profiles = @("observability");
        Required = $requiredServices
    },
    @{
        Name = "infra-runtime";
        Profiles = @("infra", "runtime");
        Required = @("gateway", "ai-orchestration-service", "research-task-service", "ai-engine-worker", "mysql", "redis", "zookeeper", "kafka", "kafka-init", "nacos")
    },
    @{
        Name = "full";
        Profiles = @("infra", "runtime", "observability");
        Required = $requiredServices
    }
)

function Get-ComposeServices {
    param([string[]]$Profiles)

    $args = @("compose", "-f", $ComposeFile)
    foreach ($profile in $Profiles) {
        $args += @("--profile", $profile)
    }
    $args += @("config", "--services")

    return docker @args
}

foreach ($entry in $profileMatrix) {
    $services = Get-ComposeServices -Profiles $entry.Profiles
    $missing = $entry.Required | Where-Object { $_ -notin $services }
    if ($missing.Count -gt 0) {
        throw "Compose profile '$($entry.Name)' verification failed. Missing services: $($missing -join ', ')"
    }
    Write-Host "compose profile check passed: $($entry.Name)"
}

Write-Host "compose config includes required services across profile matrix"

if (-not $RuntimeChecks) {
    Write-Host "runtime checks skipped; pass -RuntimeChecks after the stack is up"
    exit 0
}

$checks = @(
    @{ Name = "gateway"; Url = "http://127.0.0.1:18080/health"; Pattern = "gateway" },
    @{ Name = "research-task-service"; Url = "http://127.0.0.1:8081/actuator/health/readiness"; Pattern = "UP" },
    @{ Name = "ai-orchestration-service"; Url = "http://127.0.0.1:8082/actuator/health/readiness"; Pattern = "UP" },
    @{ Name = "ai-engine-worker"; Url = "http://127.0.0.1:8090/ready"; Pattern = "ready" },
    @{ Name = "research-task-service-prometheus"; Url = "http://127.0.0.1:8081/actuator/prometheus"; Pattern = "jvm_|http_server_requests|process_uptime" },
    @{ Name = "ai-orchestration-service-prometheus"; Url = "http://127.0.0.1:8082/actuator/prometheus"; Pattern = "jvm_|http_server_requests|process_uptime" },
    @{ Name = "ai-engine-worker-metrics"; Url = "http://127.0.0.1:8090/metrics"; Pattern = "ai_engine_http_requests_total|ai_engine_tasks_total|ai_engine_redis_degraded" },
    @{ Name = "kafka-exporter"; Url = "http://127.0.0.1:9308/metrics"; Pattern = "kafka_brokers|kafka_consumergroup_lag|kafka_topic_partitions" },
    @{ Name = "redis-exporter"; Url = "http://127.0.0.1:9121/metrics"; Pattern = "redis_up" },
    @{ Name = "nginx-exporter"; Url = "http://127.0.0.1:9113/metrics"; Pattern = "nginx_up|nginx_connections" },
    @{ Name = "prometheus"; Url = "http://127.0.0.1:9090/-/healthy"; Pattern = "Prometheus" },
    @{ Name = "grafana"; Url = "http://127.0.0.1:3000/api/health"; Pattern = "ok" }
)

foreach ($check in $checks) {
    $body = (Invoke-WebRequest -Uri $check.Url -UseBasicParsing -TimeoutSec 10).Content
    if ($body -notmatch $check.Pattern) {
        throw "Runtime check failed for $($check.Name): expected pattern '$($check.Pattern)' at $($check.Url)"
    }
    Write-Host "runtime check passed: $($check.Name)"
}

$traceId = "phase-016-smoke-$([Guid]::NewGuid().ToString('N'))"
$gatewayResponse = Invoke-WebRequest -Uri "http://127.0.0.1:18080/health" -Headers @{ "X-Trace-Id" = $traceId } -UseBasicParsing -TimeoutSec 10
if ($gatewayResponse.Headers["X-Trace-Id"] -ne $traceId) {
    throw "Runtime trace check failed: gateway did not preserve X-Trace-Id"
}
Write-Host "runtime check passed: gateway trace header propagation"

$promTargets = (Invoke-WebRequest -Uri "http://127.0.0.1:9090/api/v1/targets" -UseBasicParsing -TimeoutSec 10).Content | ConvertFrom-Json
$expectedJobs = @(
    "ai-orchestration-service",
    "research-task-service",
    "ai-engine-worker",
    "kafka-exporter",
    "redis-exporter",
    "nginx-exporter"
)
foreach ($job in $expectedJobs) {
    $upTarget = $promTargets.data.activeTargets | Where-Object {
        $_.labels.job -eq $job -and $_.health -eq "up"
    }
    if (-not $upTarget) {
        throw "Runtime Prometheus target check failed: job '$job' is not up"
    }
}
Write-Host "runtime check passed: Prometheus scrape targets are up"

$null = Invoke-WebRequest -Uri "http://127.0.0.1:18080/api/__phase_016_smoke" -Headers @{ "X-Trace-Id" = $traceId } -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1
$gatewayLogs = docker logs quant-gateway --tail 50
if ($gatewayLogs -notmatch '"service":"gateway"' -or $gatewayLogs -notmatch "`"traceId`":`"$traceId`"") {
    throw "Runtime log check failed: gateway logs did not include JSON service and traceId fields"
}
Write-Host "runtime check passed: gateway JSON trace logs"

Write-Host "compose runtime smoke checks passed"
