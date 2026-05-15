from fastapi import FastAPI
from app.config.settings import settings

app = FastAPI(title="quant-ai-engine", version="1.0.0")


@app.get("/health")
def health():
    return {
        "status": "ok",
        "env": settings.app.env
    }