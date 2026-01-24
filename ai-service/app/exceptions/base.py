"""Base exception class for application-specific exceptions."""

from typing import Any, Optional


class AppException(Exception):
    """
    Base exception for all application-specific errors.

    Provides consistent error structure with status code, error type,
    message, and optional details.

    Attributes:
        status_code: HTTP status code for this error
        error_type: Short identifier for the error type
        message: Human-readable error message
        details: Optional list of additional error details
    """

    def __init__(
        self,
        message: str,
        *,
        status_code: int = 500,
        error_type: Optional[str] = None,
        details: Optional[list[str]] = None,
    ) -> None:
        self.status_code = status_code
        self.error_type = error_type or self.__class__.__name__
        self.message = message
        self.details = details or []
        super().__init__(self.message)

    def to_dict(self) -> dict[str, Any]:
        """Convert exception to dictionary for JSON response."""
        return {
            "status": self.status_code,
            "error": self.error_type,
            "message": self.message,
            "details": self.details if self.details else None,
        }
