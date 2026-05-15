import json
from app.clients.redis_client import RedisClient
from app.common.exceptions import TaskCancelledException


class TaskControlService:
    def __init__(self):
        self.redis_client = RedisClient()

    def check_cancelled(self, task_id: str):
        raw = self.redis_client.get(f"task:control:{task_id}")
        if not raw:
            return

        try:
            data = json.loads(raw)
        except Exception:
            return

        if data.get("cancelled") is True:
            raise TaskCancelledException(data.get("reason", "task cancelled"))