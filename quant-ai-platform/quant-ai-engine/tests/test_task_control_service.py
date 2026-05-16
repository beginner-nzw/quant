import unittest

from app.common.exceptions import TaskCancelledException
from app.services import task_control_service


class FakeRedisClient:
    def __init__(self, value):
        self.value = value
        self.keys = []

    def get(self, key):
        self.keys.append(key)
        return self.value


class TaskControlServiceTests(unittest.TestCase):
    def build_service(self, value):
        original = task_control_service.RedisClient
        fake = FakeRedisClient(value)
        task_control_service.RedisClient = lambda: fake
        try:
            service = task_control_service.TaskControlService()
        finally:
            task_control_service.RedisClient = original
        return service, fake

    def test_missing_runtime_signal_does_not_cancel(self):
        service, redis_client = self.build_service(None)

        service.check_cancelled("task-1")

        self.assertEqual(["task:control:task-1"], redis_client.keys)

    def test_valid_runtime_signal_raises_cancelled_exception(self):
        service, redis_client = self.build_service('{"cancelled": true, "reason": "manual cancel"}')

        with self.assertRaises(TaskCancelledException) as context:
            service.check_cancelled("task-1")

        self.assertEqual("manual cancel", str(context.exception))
        self.assertEqual(["task:control:task-1"], redis_client.keys)

    def test_malformed_runtime_signal_does_not_cancel(self):
        service, redis_client = self.build_service('{"cancelled": true')

        service.check_cancelled("task-1")

        self.assertEqual(["task:control:task-1"], redis_client.keys)

    def test_non_object_runtime_signal_does_not_cancel(self):
        for raw in ["true", "[]", '"cancelled"']:
            with self.subTest(raw=raw):
                service, redis_client = self.build_service(raw)

                service.check_cancelled("task-1")

                self.assertEqual(["task:control:task-1"], redis_client.keys)

    def test_optional_reason_field_does_not_break_parsing(self):
        service, redis_client = self.build_service('{"cancelled": true}')

        with self.assertRaises(TaskCancelledException) as context:
            service.check_cancelled("task-1")

        self.assertEqual("task cancelled", str(context.exception))
        self.assertEqual(["task:control:task-1"], redis_client.keys)


if __name__ == "__main__":
    unittest.main()
