import subprocess
import sys
import threading
import time
import uvicorn
from app.main import app
from app.config.settings import settings


def run_http():
    uvicorn.run(app, host=settings.app.host, port=settings.app.port)


def supervise_consumer():
    restart_count = 0
    while True:
        print(
            f"[AI-ENGINE][BOOTSTRAP][CONSUMER_PROCESS_START] restartCount={restart_count}",
            flush=True,
        )
        process = subprocess.Popen([sys.executable, "-m", "app.consumer_worker"])
        exit_code = process.wait()
        print(
            f"[AI-ENGINE][BOOTSTRAP][CONSUMER_PROCESS_EXITED] restartCount={restart_count} exitCode={exit_code}",
            flush=True,
        )
        restart_count += 1
        time.sleep(5)


if __name__ == "__main__":
    t = threading.Thread(target=supervise_consumer, daemon=True, name="ai-engine-kafka-consumer-supervisor")
    t.start()
    run_http()
