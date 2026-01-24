"""Exceptions for AI/LLM service errors."""

from app.exceptions.base import AppException


class AIServiceException(AppException):
    """Base exception for AI service errors."""

    def __init__(
        self,
        message: str,
        status_code: int = 500,
        error_type: str = "AIServiceError",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=status_code,
            error_type=error_type,
            details=details,
        )


class AIServiceUnavailableException(AIServiceException):
    """Raised when the AI service is unavailable or unreachable."""

    def __init__(
        self,
        message: str = "AI service is currently unavailable",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=503,
            error_type="AIServiceUnavailable",
            details=details,
        )


class AIModelException(AIServiceException):
    """Raised when the AI model fails to process a request."""

    def __init__(
        self,
        message: str = "AI model processing failed",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="AIModelError",
            details=details,
        )


class AIStrategyException(AIServiceException):
    """Raised when AI fails to determine document structure/split strategy."""

    def __init__(
        self,
        message: str = "Failed to determine document structure",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="AIStrategyError",
            details=details,
        )


class AIEnrichmentException(AIServiceException):
    """Raised when AI fails to enrich topic content."""

    def __init__(
        self,
        topic_index: int | None = None,
        message: str | None = None,
        details: list[str] | None = None,
    ) -> None:
        if message is None:
            topic_info = f" topic #{topic_index}" if topic_index is not None else ""
            message = f"Failed to enrich{topic_info} with AI-generated content"
        super().__init__(
            message=message,
            status_code=500,
            error_type="AIEnrichmentError",
            details=details,
        )
