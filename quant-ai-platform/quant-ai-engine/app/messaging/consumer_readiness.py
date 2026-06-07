import json
from pathlib import Path
import time


CONSUMER_STATE_FILE = Path("/tmp/quant-ai-engine-consumer-state.json")


def load_consumer_runtime_state(fallback_state: dict | None = None) -> dict:
    try:
        if CONSUMER_STATE_FILE.exists():
            return json.loads(CONSUMER_STATE_FILE.read_text(encoding="utf-8"))
    except Exception:
        pass
    return dict(fallback_state or {})


def is_consumer_ready(consumer_state: dict, now_ms: int | None = None) -> tuple[bool, dict]:
    now_ms = now_ms if now_ms is not None else int(time.time() * 1000)
    last_poll_at = consumer_state.get("lastPollAt")
    recent_poll = isinstance(last_poll_at, int) and now_ms - last_poll_at <= 15000
    checks = {
        "started": bool(consumer_state.get("started")),
        "subscribed": bool(consumer_state.get("subscribed")),
        "running": bool(consumer_state.get("running")),
        "assigned": bool(consumer_state.get("assigned")),
        "assignmentCount": int(consumer_state.get("assignmentCount") or 0),
        "recentPoll": recent_poll,
    }
    ready = (
        checks["started"]
        and checks["subscribed"]
        and checks["running"]
        and checks["assigned"]
        and checks["assignmentCount"] > 0
        and checks["recentPoll"]
    )
    return ready, checks
