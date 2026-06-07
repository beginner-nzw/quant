import unittest
import json
from pathlib import Path
from types import SimpleNamespace

from app.messaging import consumer_readiness
from app.messaging.consumer_readiness import is_consumer_ready, load_consumer_runtime_state
from app.messaging import kafka_group_readiness


class ImmediateFuture:
    def __init__(self, value):
        self.value = value

    def result(self, timeout=None):
        return self.value


class FakeAdmin:
    def __init__(self, member_count):
        self.member_count = member_count

    def describe_consumer_groups(self, group_ids):
        group_id = group_ids[0]
        return {
            group_id: ImmediateFuture(
                SimpleNamespace(
                    members=[object()] * self.member_count,
                    state=SimpleNamespace(name="Stable"),
                )
            )
        }


class FakeListOnlyAdmin:
    def list_consumer_groups(self, request_timeout=None):
        return ImmediateFuture(
            SimpleNamespace(
                valid=[
                    SimpleNamespace(
                        group_id="python-ai-engine-group",
                        state=SimpleNamespace(name="Stable"),
                    )
                ]
            )
        )


class Phase017EngineReadinessTests(unittest.TestCase):
    def test_ready_requires_active_assignment_and_recent_poll(self):
        ready, checks = is_consumer_ready(
            {
                "started": True,
                "subscribed": True,
                "running": True,
                "assigned": False,
                "assignmentCount": 0,
                "lastPollAt": 1000,
            },
            now_ms=2000,
        )

        self.assertFalse(ready)
        self.assertFalse(checks["assigned"])
        self.assertEqual(0, checks["assignmentCount"])

    def test_ready_accepts_assigned_recent_consumer(self):
        ready, checks = is_consumer_ready(
            {
                "started": True,
                "subscribed": True,
                "running": True,
                "assigned": True,
                "assignmentCount": 3,
                "lastPollAt": 1000,
            },
            now_ms=2000,
        )

        self.assertTrue(ready)
        self.assertTrue(checks["recentPoll"])

    def test_ready_rejects_stale_poll_even_with_assignment(self):
        ready, checks = is_consumer_ready(
            {
                "started": True,
                "subscribed": True,
                "running": True,
                "assigned": True,
                "assignmentCount": 3,
                "lastPollAt": 1000,
            },
            now_ms=20000,
        )

        self.assertFalse(ready)
        self.assertFalse(checks["recentPoll"])

    def test_kafka_group_probe_rejects_no_active_members(self):
        original_admin_client = kafka_group_readiness._admin_client
        kafka_group_readiness._admin_client = lambda: FakeAdmin(0)
        try:
            group = kafka_group_readiness.probe_consumer_group()
        finally:
            kafka_group_readiness._admin_client = original_admin_client

        self.assertFalse(group["active"])
        self.assertEqual(0, group["memberCount"])

    def test_kafka_group_probe_accepts_active_members(self):
        original_admin_client = kafka_group_readiness._admin_client
        kafka_group_readiness._admin_client = lambda: FakeAdmin(1)
        try:
            group = kafka_group_readiness.probe_consumer_group()
        finally:
            kafka_group_readiness._admin_client = original_admin_client

        self.assertTrue(group["active"])
        self.assertEqual(1, group["memberCount"])

    def test_kafka_group_probe_rejects_unknown_member_count(self):
        original_admin_client = kafka_group_readiness._admin_client
        kafka_group_readiness._admin_client = lambda: FakeListOnlyAdmin()
        try:
            group = kafka_group_readiness.probe_consumer_group()
        finally:
            kafka_group_readiness._admin_client = original_admin_client

        self.assertFalse(group["active"])
        self.assertIsNone(group["memberCount"])
        self.assertIn("member count unavailable", group["error"])

    def test_consumer_runtime_state_prefers_persisted_worker_state(self):
        original_state_file = consumer_readiness.CONSUMER_STATE_FILE
        state_file = Path(__file__).resolve().parent / "phase017-test-consumer-state.json"
        consumer_readiness.CONSUMER_STATE_FILE = state_file
        try:
            state_file.write_text(
                json.dumps({"started": True, "assigned": True, "assignmentCount": 3}),
                encoding="utf-8",
            )
            state = load_consumer_runtime_state()
        finally:
            try:
                state_file.unlink()
            except FileNotFoundError:
                pass
            consumer_readiness.CONSUMER_STATE_FILE = original_state_file

        self.assertTrue(state["started"])
        self.assertTrue(state["assigned"])
        self.assertEqual(3, state["assignmentCount"])


if __name__ == "__main__":
    unittest.main()
