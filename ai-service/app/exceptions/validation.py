"""Validation exceptions for file upload and input validation."""

from app.exceptions.base import AppException


class MissingFilenameException(AppException):
    """Raised when a file upload has no filename."""

    def __init__(self, message: str = "No filename provided") -> None:
        super().__init__(
            message=message,
            status_code=400,
            error_type="MissingFilename",
        )


class EmptyFileException(AppException):
    """Raised when an uploaded file is empty (0 bytes)."""

    def __init__(self, message: str = "Empty file") -> None:
        super().__init__(
            message=message,
            status_code=400,
            error_type="EmptyFile",
        )


class UnsupportedFileTypeException(AppException):
    """Raised when a file type is not supported."""

    def __init__(self, allowed_types: set[str], message: str | None = None) -> None:
        if message is None:
            allowed = ", ".join(sorted(allowed_types))
            message = f"Unsupported file type. Allowed types: {allowed}"
        super().__init__(
            message=message,
            status_code=415,
            error_type="UnsupportedFileType",
            details=list(allowed_types) if allowed_types else None,
        )


class FileTooLargeException(AppException):
    """Raised when a file exceeds the maximum allowed size."""

    def __init__(self, max_size_mb: float, message: str | None = None) -> None:
        if message is None:
            message = f"File too large. Maximum size: {max_size_mb:.1f}MB"
        super().__init__(
            message=message,
            status_code=413,
            error_type="FileTooLarge",
            details=[f"Maximum size: {max_size_mb:.1f}MB"],
        )
