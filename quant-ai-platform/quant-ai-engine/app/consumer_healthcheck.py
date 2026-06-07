import sys

from app.messaging.consumer_readiness import is_consumer_ready, load_consumer_runtime_state


if __name__ == "__main__":
    state = load_consumer_runtime_state()
    ready, checks = is_consumer_ready(state)
    if ready:
        print("ready")
        sys.exit(0)
    print({"status": "not_ready", "checks": checks, "state": state})
    sys.exit(1)
