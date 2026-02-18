"""Exceptions for flashcard generation errors."""

from app.exceptions.ai_service import AIServiceException


class FlashcardGenerationException(AIServiceException):
    """Base exception for flashcard generation errors."""

    def __init__(
        self,
        message: str = "Failed to generate flashcard",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="FlashcardGenerationError",
            details=details,
        )


class FlashcardParsingException(FlashcardGenerationException):
    """Raised when LLM response fails to parse."""

    def __init__(
        self,
        message: str = "Failed to parse AI-generated flashcard",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type="FlashcardParsingError",
            details=details,
        )


class FlashcardRequestException(FlashcardGenerationException):
    """Raised when flashcard request validation fails."""

    def __init__(
        self,
        message: str = "Invalid flashcard request",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=400,
            error_type="FlashcardRequestError",
            details=details,
        )
