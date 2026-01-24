"""Exceptions for PDF processing errors."""

from app.exceptions.base import AppException


class PDFProcessingException(AppException):
    """Base exception for PDF processing errors."""

    def __init__(
        self,
        message: str,
        error_type: str = "PDFProcessingError",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            status_code=500,
            error_type=error_type,
            details=details,
        )


class PDFParsingException(PDFProcessingException):
    """Raised when PDF parsing fails."""

    def __init__(self, message: str = "Failed to parse PDF file") -> None:
        super().__init__(message=message, error_type="PDFParsingError")


class PDFCorruptedException(PDFProcessingException):
    """Raised when a PDF file is corrupted or invalid."""

    def __init__(
        self,
        message: str = "PDF file is corrupted or invalid",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            error_type="PDFCorrupted",
            details=details,
        )


class PDFStructureException(PDFProcessingException):
    """Raised when PDF structure is unexpected or cannot be analyzed."""

    def __init__(
        self,
        message: str = "PDF structure could not be analyzed",
        details: list[str] | None = None,
    ) -> None:
        super().__init__(
            message=message,
            error_type="PDFStructureError",
            details=details,
        )
