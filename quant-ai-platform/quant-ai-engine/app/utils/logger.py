from datetime import datetime, timezone
from typing import Any

import orjson


def log_event(level: str, trace_id: str, message: str, **fields: Any) -> None:
    payload = {
        "ts": datetime.now(timezone.utc).isoformat(),
        "level": level.upper(),
        "service": "quant-ai-engine",
        "traceId": trace_id or "",
        "message": message,
    }
    payload.update({key: value for key, value in fields.items() if value is not None})
    print(orjson.dumps(payload, default=str).decode("utf-8"), flush=True)


def log_info(trace_id: str, message: str, **fields: Any) -> None:
    log_event("INFO", trace_id, message, **fields)


def log_error(trace_id: str, message: str, **fields: Any) -> None:
    log_event("ERROR", trace_id, message, **fields)
