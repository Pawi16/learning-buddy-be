"""Flashcard generation schemas."""

from pydantic import BaseModel, Field
from typing import List, Literal


# ============================================================================
# REQUEST MODELS
# ============================================================================

class FlashcardConfig(BaseModel):
    """Configuration for flashcard generation."""
    amount: int = Field(..., description="Number of flashcards to generate")


class GenerateFlashcardRequest(BaseModel):
    """Request to generate flashcards from topic content."""
    topicName: str = Field(..., description="Name of the topic")
    topicContent: str = Field(..., description="Content text for the topic")
    config: FlashcardConfig = Field(..., description="Flashcard configuration")


# ============================================================================
# INTERNAL LLM MODELS
# ============================================================================

class LLMFlashcardItem(BaseModel):
    """Individual flashcard item from LLM output."""
    id: int = Field(..., description="Card sequence number (1, 2, 3...)")
    front_text: str = Field(..., description="The question, term, or concept (Front side)")
    back_text: str = Field(..., description="The answer, definition, or explanation (Back side)")
    category: Literal["Definition", "Concept", "Pro/Con"] = Field(
        ..., description="Type of information"
    )


class LLMFlashcardDeck(BaseModel):
    """Complete flashcard deck from LLM output."""
    topic_title: str = Field(..., description="Title of the topic")
    cards: List[LLMFlashcardItem] = Field(..., description="List of flashcards")


# ============================================================================
# RESPONSE MODELS
# ============================================================================

class FlashcardItemResponse(BaseModel):
    """Individual flashcard item in API response."""
    id: int = Field(..., description="Card sequence number (1, 2, 3...)")
    front_text: str = Field(..., description="The question, term, or concept (Front side)")
    back_text: str = Field(..., description="The answer, definition, or explanation (Back side)")
    category: str = Field(..., description="Type of information")


class FlashcardDeckResponse(BaseModel):
    """Complete flashcard deck in API response."""
    topic_title: str = Field(..., description="Title of the topic")
    cards: List[FlashcardItemResponse] = Field(..., description="List of flashcards")
