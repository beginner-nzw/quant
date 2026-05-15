def log_info(trace_id: str, message: str):
    print(f"[INFO] traceId={trace_id} {message}")


def log_error(trace_id: str, message: str):
    print(f"[ERROR] traceId={trace_id} {message}")