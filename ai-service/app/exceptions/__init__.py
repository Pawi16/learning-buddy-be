"""
Exception handling module for AI Service.

This module provides custom exceptions and handlers for consistent error responses
across the application.
"""

from app.exceptions.base import AppException
from app.exceptions.validation import (
    MissingFilenameException,
    EmptyFileException,
    UnsupportedFileTypeException,
    FileTooLargeException,
)
from app.exceptions.pdf_processing import (
    PDFParsingException,
    PDFCorruptedException,
    PDFStructureException,
)
from app.exceptions.ai_service import (
    AIServiceUnavailableException,
    AIModelException,
    AIStrategyException,
    AIEnrichmentException,
)
from app.exceptions.quiz_generation import (
    QuizGenerationException,
    QuizParsingException,
    QuizRequestException,
)

__all__ = [
    # Base
    "AppException",
    # Validation
    "MissingFilenameException",
    "EmptyFileException",
    "UnsupportedFileTypeException",
    "FileTooLargeException",
    # PDF Processing
    "PDFParsingException",
    "PDFCorruptedException",
    "PDFStructureException",
    # AI Service
    "AIServiceUnavailableException",
    "AIModelException",
    "AIStrategyException",
    "AIEnrichmentException",
    # Quiz Generation
    "QuizGenerationException",
    "QuizParsingException",
    "QuizRequestException",
]
