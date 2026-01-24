"""Error response schema matching backend format."""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    """Standardized error response format."""

    timestamp: str = Field(
        ...,
        description="ISO 8601 timestamp when the error occurred",
        examples=["2026-01-25T10:30:00Z"],
    )
    status: int = Field(
        ...,
        description="HTTP status code",
        examples=[400, 413, 415, 500, 503],
    )
    error: str = Field(
        ...,
        description="HTTP status phrase or error type",
        examples=["Bad Request", "Payload Too Large", "Internal Server Error"],
    )
    message: str = Field(
        ...,
        description="Human-readable error message",
        examples=["No filename provided", "File too large", "AI service unavailable"],
    )
    path: str = Field(
        ...,
        description="Request path that caused the error",
        examples=["/api/v1/process-pdf"],
    )
    details: Optional[list[str]] = Field(
        default=None,
        description="Optional additional error details",
        examples=[["Maximum size: 10.0MB", ".pdf"]],
    )
