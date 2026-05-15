from concurrent.futures import ThreadPoolExecutor, TimeoutError
from app.common.exceptions import TaskTimeoutException


class TimeoutExecutor:
    def __init__(self):
        self.executor = ThreadPoolExecutor(max_workers=8)

    def run_with_timeout(self, fn, state: dict, timeout_seconds: int):
        future = self.executor.submit(fn, state)
        try:
            return future.result(timeout=timeout_seconds)
        except TimeoutError:
            future.cancel()
            raise TaskTimeoutException(f"node execution timeout after {timeout_seconds}s")
