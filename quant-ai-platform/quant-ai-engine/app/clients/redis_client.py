import redis
from app.config.settings import settings
from app.observability import set_redis_degraded
from app.utils.logger import log_error, log_info


class RedisClient:
    def __init__(self):
        self.client = redis.Redis(
            host=settings.redis.host,
            port=settings.redis.port,
            db=settings.redis.db,
            decode_responses=True,
            socket_timeout=settings.redis.socket_timeout_seconds,
            socket_connect_timeout=settings.redis.socket_timeout_seconds,
        )
        self.degraded = False

    def get(self, key: str):
        return self._with_degradation("get", key, lambda: self.client.get(key))

    def setex(self, key: str, seconds: int, value: str):
        return self._with_degradation("setex", key, lambda: self.client.setex(key, seconds, value))

    def delete(self, key: str):
        return self._with_degradation("delete", key, lambda: self.client.delete(key))

    def _with_degradation(self, operation: str, key: str, fn):
        try:
            result = fn()
            if self.degraded:
                log_info("", "[AI-ENGINE][REDIS] recovered", operation=operation)
            self.degraded = False
            set_redis_degraded(False)
            return result
        except redis.RedisError as exc:
            self.degraded = True
            set_redis_degraded(True)
            log_error(
                "",
                "[AI-ENGINE][REDIS][DEGRADED]",
                operation=operation,
                key=key,
                error=str(exc),
            )
            if settings.redis.degradation_enabled:
                return None if operation == "get" else False
            raise
