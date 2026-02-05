"""Exceptions for quiz generation errors."""

from app.exceptions.ai_service import AIServiceException


class QuizGenerationException(AIServiceException):
    """Base exception for quiz generation errors."""

    def __init__(
        self,
        message: str = "Failed to generate quiz",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="QuizGenerationError",
            details=details,
        )


class QuizParsingException(QuizGenerationException):
    """Raised when LLM response fails to parse."""

    def __init__(
        self,
        message: str = "Failed to parse AI-generated quiz",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="QuizParsingError",
            details=details,
        )


class QuizRequestException(QuizGenerationException):
    """Raised when quiz request validation fails."""

    def __init__(
        self,
        message: str = "Invalid quiz request",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=400,
            error_type="QuizRequestError",
            details=details,
        )
