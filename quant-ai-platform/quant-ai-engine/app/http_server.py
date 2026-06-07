import uvicorn

from app.config.settings import settings
from app.main import app


if __name__ == "__main__":
    uvicorn.run(app, host=settings.app.host, port=settings.app.port)
