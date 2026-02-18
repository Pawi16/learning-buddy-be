"""Flashcard generation service using AI/LLM."""

import logging
from typing import List

from langchain_core.exceptions import OutputParserException
from langchain_core.output_parsers import JsonOutputParser
from openai import AuthenticationError, RateLimitError, APITimeoutError

from app.core.config import settings
from app.services import llm_service
from app.schemas.flashcard import (
    GenerateFlashcardRequest,
    FlashcardDeckResponse,
    FlashcardItemResponse,
    LLMFlashcardDeck,
    LLMFlashcardItem,
)
from app.exceptions.flashcard_generation import (
    FlashcardGenerationException,
    FlashcardParsingException,
    FlashcardRequestException,
)
from app.exceptions.ai_service import AIServiceUnavailableException

logger = logging.getLogger(__name__)


# --- PRIVATE HELPER FUNCTIONS ---

def _transform_llm_to_api_response(
    llm_deck: LLMFlashcardDeck,
) -> FlashcardDeckResponse:
    """
    Transform LLM response format to API response format.

    Args:
        llm_deck: LLMFlashcardDeck from LLM

    Returns:
        FlashcardDeckResponse with FlashcardItemResponse objects
    """
    # Transform LLM items to API response items
    api_cards = [
        FlashcardItemResponse(
            id=card.id,
            front_text=card.front_text,
            back_text=card.back_text,
            category=card.category,
        )
        for card in llm_deck.cards
    ]

    return FlashcardDeckResponse(
        topic_title=llm_deck.topic_title,
        cards=api_cards,
    )


def _generate_flashcard_deck(
    topic_name: str,
    topic_content: str,
    amount: int,
) -> FlashcardDeckResponse:
    """
    Generate flashcard deck using LLM.

    Args:
        topic_name: Name of the topic
        topic_content: Content text for the topic
        amount: Number of flashcards to generate

    Returns:
        FlashcardDeckResponse with generated flashcards

    Raises:
        FlashcardParsingException: If LLM response fails to parse
        AIServiceUnavailableException: If AI service is unreachable
    """
    logger.info(f"Generating {amount} flashcards for topic: {topic_name}")

    # Create JsonOutputParser for LLM response
    parser = JsonOutputParser(pydantic_object=LLMFlashcardDeck)
    chain = llm_service.llm | parser

    # Build prompt following POC pattern
    prompt = f"""
    You are an Expert Tutor creating a Flashcard Deck for a student.

    TOPIC: {topic_name}
    SOURCE MATERIAL:
    {topic_content}

    TASK:
    Create exactly {amount} high-quality flashcards to help the student memorize the key concepts from the text.

    GUIDELINES:
    1. **Focus:** Cover the most important definitions, advantages, disadvantages, and code concepts.
    2. **Front Side:** Should be a specific question or term (e.g., "What is the main advantage of X?").
    3. **Back Side:** Should be concise and accurate (1-2 sentences).

    SELF-CONTAINED RULE (CRITICAL):
    - Do NOT say "In the text above..." or "As mentioned...".
    - The card must make sense in isolation.
    - BAD Back: "It is used to create objects." (What is 'It'?)
    - GOOD Back: "The Builder Pattern is used to create objects..."

    DIVERSITY RULE:
    - Each card must cover a DIFFERENT fact. Do not repeat the same concept twice.

    CATEGORY CLASSIFICATION:
    - "Definition": For terms, concepts, or "What is..." questions
    - "Concept": For explanations, mechanisms, or "How does..." questions
    - "Pro/Con": For advantages, disadvantages, or trade-offs

    OUTPUT INSTRUCTIONS:
    Return the result as a single valid JSON object.

    REQUIRED JSON STRUCTURE (Follow this exactly):
    {{
        "topic_title": "{topic_name}",
        "cards": [
            {{
                "id": 1,
                "front_text": "What is the Builder pattern?",
                "back_text": "The Builder pattern is a creational design pattern...",
                "category": "Definition"
            }},
            {{
                "id": 2,
                "front_text": "What is the main advantage of the Builder pattern?",
                "back_text": "It provides a clear way to construct objects with many optional parameters...",
                "category": "Pro/Con"
            }}
        ]
    }}
    """

    # Retry logic for LLM calls
    max_retries = settings.AI_MAX_RETRIES

    for attempt in range(max_retries + 1):
        try:
            # Invoke the chain
            raw_result_dict = chain.invoke(prompt)

            # Validate with Pydantic
            llm_deck = LLMFlashcardDeck(**raw_result_dict)

            # Assign sequential IDs if LLM didn't provide them correctly
            for idx, card in enumerate(llm_deck.cards, start=1):
                card.id = idx

            # Validate we got the requested amount
            if len(llm_deck.cards) < amount:
                logger.warning(
                    f"Requested {amount} flashcards, got {len(llm_deck.cards)}"
                )

            logger.info(
                f"Successfully generated {len(llm_deck.cards)} flashcards "
                f"for topic: {topic_name}"
            )

            # Transform to API response format
            return _transform_llm_to_api_response(llm_deck)

        except AuthenticationError as e:
            logger.error(
                f"AI authentication failed for flashcard generation: {e}"
            )
            raise AIServiceUnavailableException(
                message="AI service authentication failed",
                details=["Check API key configuration"],
            )

        except RateLimitError as e:
            logger.warning(
                f"AI rate limit reached for flashcard generation: {e}"
            )
            raise AIServiceUnavailableException(
                message="AI service rate limit exceeded",
                details=["Please try again later"],
            )

        except (APITimeoutError, TimeoutError) as e:
            logger.error(
                f"AI request timeout for flashcard generation: {e}"
            )
            raise AIServiceUnavailableException(
                message="AI service request timed out",
                details=[f"Timeout: {settings.AI_TIMEOUT}s"],
            )

        except OutputParserException as e:
            logger.warning(
                f"Attempt {attempt + 1}/{max_retries + 1}: "
                f"Failed to parse LLM response for flashcards: {e}"
            )
            if attempt == max_retries:
                raise FlashcardParsingException(
                    message="Failed to parse AI response for flashcards",
                    details=[str(e)],
                )

        except Exception as e:
            logger.error(
                f"Unexpected error generating flashcards: {e}",
                exc_info=True,
            )
            raise FlashcardGenerationException(
                message="Failed to generate flashcards",
                details=[str(e)],
            )

    # Should not reach here, but just in case
    raise FlashcardGenerationException(
        message="Failed to generate flashcards after all retries",
        details=["Maximum retry attempts exceeded"],
    )


# --- PUBLIC API ---

def generate_flashcard(request: GenerateFlashcardRequest) -> FlashcardDeckResponse:
    """
    Generate flashcards from topic content using AI.

    This is the main entry point for flashcard generation.

    Args:
        request: GenerateFlashcardRequest with topicName, topicContent, and config

    Returns:
        FlashcardDeckResponse with topic_title and cards list

    Raises:
        FlashcardRequestException: If request validation fails
        FlashcardParsingException: If LLM response fails to parse
        AIServiceUnavailableException: If AI service is unreachable
        FlashcardGenerationException: For other flashcard generation errors
    """
    logger.info(f"Generating flashcard for topic: {request.topicName}")

    # Validate request
    if not request.topicName or not request.topicName.strip():
        raise FlashcardRequestException(
            message="Topic name is required",
            details=["topicName cannot be empty"],
        )

    if not request.topicContent or not request.topicContent.strip():
        raise FlashcardRequestException(
            message="Topic content is required",
            details=["topicContent cannot be empty"],
        )

    if not request.config or request.config.amount <= 0:
        raise FlashcardRequestException(
            message="Valid flashcard configuration is required",
            details=["config.amount must be greater than 0"],
        )

    # Generate flashcards
    try:
        result = _generate_flashcard_deck(
            topic_name=request.topicName,
            topic_content=request.topicContent,
            amount=request.config.amount,
        )

        logger.info(
            f"Successfully generated {len(result.cards)} flashcards "
            f"for topic: {request.topicName}"
        )

        return result

    except FlashcardParsingException:
        # Re-raise parsing exceptions as-is
        raise

    except AIServiceUnavailableException:
        # Re-raise AI service exceptions as-is
        raise

    except FlashcardRequestException:
        # Re-raise request exceptions as-is
        raise

    except Exception as e:
        # Wrap other exceptions in FlashcardGenerationException
        logger.error(
            f"Unexpected error generating flashcard: {e}",
            exc_info=True,
        )
        raise FlashcardGenerationException(
            message="Failed to generate flashcard",
            details=[str(e)],
        )
