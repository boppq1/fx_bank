from fastapi import FastAPI
from app.api.routes import router

app = FastAPI(title="RAG Server")
app.include_router(router, prefix="/api")

@app.get("/")
def health():
    return {"status": "ok"}