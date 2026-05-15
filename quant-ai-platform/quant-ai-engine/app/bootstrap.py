import threading
import uvicorn
from app.main import app
from app.config.settings import settings
from app.messaging.kafka_consumer import start_consumer


def run_http():
    uvicorn.run(app, host=settings.app.host, port=settings.app.port)


if __name__ == "__main__":
    t = threading.Thread(target=start_consumer, daemon=True)
    t.start()
    run_http()