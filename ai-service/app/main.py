import logging
import uuid
from typing import Callable

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import router
from app.core.config import settings
from app.exceptions.handlers import EXCEPTION_HANDLERS

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Learning Buddy AI Service",
    description="AI-powered PDF processing and topic extraction service",
    version="0.1.0"
)


# Request ID middleware for tracing
@app.middleware("http")
async def add_request_id(request: Request, call_next: Callable) -> callable:
    """Add unique request ID for tracing."""
    request_id = str(uuid.uuid4())
    request.state.request_id = request_id
    response = await call_next(request)
    response.headers["X-Request-ID"] = request_id
    return response

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register global exception handlers
for exc_class, exc_handler in EXCEPTION_HANDLERS.items():
    app.add_exception_handler(exc_class, exc_handler)

logger.info("Registered global exception handlers")

# Include API router
app.include_router(router, prefix="/api/v1")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=settings.HOST, port=settings.PORT, reload=settings.RELOAD)