import time
from functools import lru_cache
from typing import Any

from confluent_kafka.admin import AdminClient

from app.config.settings import settings


@lru_cache(maxsize=1)
def _admin_client() -> AdminClient:
    return AdminClient({"bootstrap.servers": settings.kafka.bootstrap_servers})


def _future_result(value: Any, timeout_seconds: float) -> Any:
    return value.result(timeout=timeout_seconds) if hasattr(value, "result") else value


def probe_consumer_group(timeout_seconds: float = 2.0) -> dict[str, Any]:
    checked_at = int(time.time() * 1000)
    try:
        admin = _admin_client()
        group_id = settings.kafka.consumer_group
        member_count = None
        state = None
        method = "none"
        error = None

        if hasattr(admin, "describe_consumer_groups"):
            method = "describe_consumer_groups"
            described = admin.describe_consumer_groups([group_id])
            group_description = _future_result(described[group_id], timeout_seconds)
            members = getattr(group_description, "members", None) or []
            member_count = len(members)
            state_value = getattr(group_description, "state", None)
            state = getattr(state_value, "name", None) or str(state_value or "")
        elif hasattr(admin, "list_consumer_groups"):
            method = "list_consumer_groups"
            listed = _future_result(admin.list_consumer_groups(request_timeout=timeout_seconds), timeout_seconds)
            valid_groups = getattr(listed, "valid", None) or []
            for group in valid_groups:
                if getattr(group, "group_id", None) == group_id:
                    state_value = getattr(group, "state", None)
                    state = getattr(state_value, "name", None) or str(state_value or "")
                    break
            error = "consumer group member count unavailable from list_consumer_groups"
        else:
            error = "AdminClient does not support consumer group inspection"

        active = member_count is not None and member_count > 0
        return {
            "checkedAt": checked_at,
            "groupId": group_id,
            "active": active,
            "memberCount": member_count,
            "state": state,
            "method": method,
            "error": error,
        }
    except Exception as exc:
        return {
            "checkedAt": checked_at,
            "groupId": settings.kafka.consumer_group,
            "active": False,
            "memberCount": None,
            "state": None,
            "method": "error",
            "error": str(exc),
        }
