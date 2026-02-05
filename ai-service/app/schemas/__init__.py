"""Schemas module for AI Service."""

from app.schemas.pdf import ProcessedTopic
from app.schemas.error import ErrorResponse
from app.schemas.quiz import (
    GenerateQuizRequest,
    QuizResponse,
    QuestionResponse,
    ChoiceResponse,
)

__all__ = [
    "ProcessedTopic",
    "ErrorResponse",
    "GenerateQuizRequest",
    "QuizResponse",
    "QuestionResponse",
    "ChoiceResponse",
]
