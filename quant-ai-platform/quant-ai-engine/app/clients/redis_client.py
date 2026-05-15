import redis
from app.config.settings import settings


class RedisClient:
    def __init__(self):
        self.client = redis.Redis(
            host=settings.redis.host,
            port=settings.redis.port,
            db=settings.redis.db,
            decode_responses=True
        )

    def get(self, key: str):
        return self.client.get(key)