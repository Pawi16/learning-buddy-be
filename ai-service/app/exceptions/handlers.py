"""Global exception handlers for consistent error responses."""

import logging
from datetime import datetime
from typing import Any

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.exceptions.base import AppException
from app.exceptions.ai_service import (
    AIServiceUnavailableException,
    AIModelException,
    AIStrategyException,
    AIEnrichmentException,
)
from app.exceptions.quiz_generation import (
    QuizGenerationException,
)
from app.exceptions.flashcard_generation import (
    FlashcardGenerationException,
)
from app.exceptions.pdf_processing import (
    PDFParsingException,
    PDFCorruptedException,
    PDFStructureException,
)
from app.exceptions.validation import (
    MissingFilenameException,
    EmptyFileException,
    UnsupportedFileTypeException,
    FileTooLargeException,
)

logger = logging.getLogger(__name__)


def build_error_response(
    request: Request,
    exc: AppException,
) -> dict[str, Any]:
    """Build standardized error response."""
    return {
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "status": exc.status_code,
        "error": exc.error_type,
        "message": exc.message,
        "path": request.url.path,
        "details": exc.details if exc.details else None,
    }


async def app_exception_handler(request: Request, exc: AppException) -> JSONResponse:
    """Handler for all AppException subclasses."""
    error_dict = build_error_response(request, exc)

    # Log at appropriate level
    if exc.status_code >= 500:
        logger.error(
            f"Server error: {exc.error_type} - {exc.message}",
            exc_info=True,
            extra={"path": request.url.path, "error_type": exc.error_type},
        )
    elif exc.status_code >= 400:
        logger.warning(
            f"Client error: {exc.error_type} - {exc.message}",
            extra={"path": request.url.path, "error_type": exc.error_type},
        )

    return JSONResponse(
        status_code=exc.status_code,
        content=error_dict,
    )


async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """
    Handler for unexpected exceptions.

    This catches any exception that is not an AppException
    and returns a generic 500 error.
    """
    logger.error(
        f"Unhandled exception: {type(exc).__name__} - {str(exc)}",
        exc_info=True,
        extra={"path": request.url.path},
    )

    error_dict = {
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "status": 500,
        "error": "Internal Server Error",
        "message": "An unexpected error occurred",
        "path": request.url.path,
        "details": None,
    }

    return JSONResponse(
        status_code=500,
        content=error_dict,
    )


async def request_validation_error_handler(
    request: Request, exc: RequestValidationError
) -> JSONResponse:
    """Handler for RequestValidationError to convert to ErrorResponse format."""
    # Convert FastAPI validation errors to human-readable details
    errors = []
    for error in exc.errors():
        loc = " -> ".join(str(x) for x in error["loc"])
        errors.append(f"{loc}: {error['msg']}")

    error_dict = {
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "status": 422,
        "error": "ValidationError",
        "message": "Request validation failed",
        "path": request.url.path,
        "details": errors if errors else None,
    }

    logger.warning(
        f"Validation error: {len(errors)} field(s)",
        extra={"path": request.url.path, "errors": errors},
    )

    return JSONResponse(status_code=422, content=error_dict)


# Mapping of exception types to handlers
EXCEPTION_HANDLERS = {
    AppException: app_exception_handler,
    RequestValidationError: request_validation_error_handler,
    Exception: generic_exception_handler,
}
