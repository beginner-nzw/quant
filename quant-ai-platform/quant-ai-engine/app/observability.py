import time
from collections.abc import Callable

from fastapi import FastAPI, Request, Response
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Gauge, Histogram, generate_latest


HTTP_REQUESTS = Counter(
    "ai_engine_http_requests_total",
    "AI engine HTTP requests.",
    ["method", "path", "status"],
)

HTTP_REQUEST_LATENCY = Histogram(
    "ai_engine_http_request_duration_seconds",
    "AI engine HTTP request latency.",
    ["method", "path"],
    buckets=(0.01, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10),
)

TASKS = Counter(
    "ai_engine_tasks_total",
    "AI engine workflow task outcomes.",
    ["status"],
)

KAFKA_LAG_MESSAGES = Gauge(
    "ai_engine_kafka_lag_messages",
    "Observed Kafka lag for the AI engine dispatch consumer.",
)

REDIS_DEGRADED = Gauge(
    "ai_engine_redis_degraded",
    "Whether the AI engine is currently tolerating Redis failures.",
)


def install_http_metrics(app: FastAPI) -> None:
    @app.middleware("http")
    async def metrics_middleware(request: Request, call_next: Callable) -> Response:
        start = time.perf_counter()
        path = request.url.path
        method = request.method
        status = "500"
        try:
            response = await call_next(request)
            status = str(response.status_code)
            return response
        finally:
            elapsed = time.perf_counter() - start
            HTTP_REQUESTS.labels(method=method, path=path, status=status).inc()
            HTTP_REQUEST_LATENCY.labels(method=method, path=path).observe(elapsed)


def metrics_response() -> Response:
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


def record_task(status: str) -> None:
    TASKS.labels(status=status).inc()


def record_kafka_lag(lag_messages: int) -> None:
    KAFKA_LAG_MESSAGES.set(max(0, int(lag_messages or 0)))


def set_redis_degraded(degraded: bool) -> None:
    REDIS_DEGRADED.set(1 if degraded else 0)
