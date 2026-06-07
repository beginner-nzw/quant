from app.config.settings import settings
from app.messaging.consumer_readiness import is_consumer_ready
from app.messaging.kafka_group_readiness import probe_consumer_group
from app.messaging.kafka_consumer import get_consumer_runtime_state
from app.observability import install_http_metrics, metrics_response
from fastapi import FastAPI

app = FastAPI(title="quant-ai-engine", version="1.0.0")
install_http_metrics(app)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "quant-ai-engine",
        "env": settings.app.env
    }


@app.get("/ready")
def ready():
    consumer_state = get_consumer_runtime_state()
    consumer_ready, consumer_ready_checks = is_consumer_ready(consumer_state)
    group_readiness = probe_consumer_group()
    engine_ready = bool(group_readiness.get("active"))
    return {
        "status": "ready" if engine_ready else "not_ready",
        "service": "quant-ai-engine",
        "env": settings.app.env,
        "kafkaBootstrapServers": settings.kafka.bootstrap_servers,
        "workflowTimeoutSeconds": settings.app.workflow_timeout_seconds,
        "redisDegradationEnabled": settings.redis.degradation_enabled,
        "consumerReadyChecks": consumer_ready_checks,
        "consumerGroup": group_readiness,
        "consumer": consumer_state,
    }


@app.get("/metrics")
def metrics():
    return metrics_response()
