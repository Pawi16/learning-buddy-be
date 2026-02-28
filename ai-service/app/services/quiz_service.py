"""Quiz generation service using AI/LLM."""

import logging
from typing import Literal

from langchain_core.exceptions import OutputParserException
from langchain_core.output_parsers import JsonOutputParser
from openai import AuthenticationError, RateLimitError, APITimeoutError

from app.core.config import settings
from app.services import llm_service
from app.schemas.quiz import (
    GenerateQuizRequest,
    QuizResponse,
    QuestionResponse,
    ChoiceResponse,
    LLMQuizQuestion,
    LLMGeneratedBatch,
)
from app.exceptions.quiz_generation import (
    QuizGenerationException,
    QuizParsingException,
    QuizRequestException,
)
from app.exceptions.ai_service import AIServiceUnavailableException

logger = logging.getLogger(__name__)

# --- ENUM MAPPINGS ---

# Maps uppercase API enum values to lowercase LLM values
QUESTION_TYPE_MAP = {
    "NORMAL_MULTIPLE": "normal_multiple",
    "STATEMENT_VERIFICATION": "statement_verification",
    "STATEMENT_COUNTING": "statement_counting",
}

# Reverse map for transforming LLM response back to API format
REVERSE_QUESTION_TYPE_MAP = {v: k for k, v in QUESTION_TYPE_MAP.items()}

# Maps uppercase API difficulty to lowercase LLM difficulty
DIFFICULTY_MAP = {
    "EASY": "easy",
    "MEDIUM": "medium",
    "HARD": "hard",
}

# Reverse map for transforming LLM response back to API format
REVERSE_DIFFICULTY_MAP = {v: k for k, v in DIFFICULTY_MAP.items()}


# --- PRIVATE HELPER FUNCTIONS ---

def _build_single_type_instruction(
    q_type: str, difficulty: str, count: int
) -> str:
    """
    Build prompt instructions for a single question type.

    Args:
        q_type: Question type (lowercase, e.g., 'normal_multiple')
        difficulty: Difficulty level (lowercase, e.g., 'easy')
        count: Number of questions to generate

    Returns:
        Prompt instruction string
    """
    # 1. Define Logic based on Difficulty
    if difficulty.lower() == "easy":
        focus = "direct definitions and basic facts."
        distractors = "Distractors should be obviously incorrect."
        stmt_logic = "Short, simple sentences directly from text."
        stmt_count = 3
        roman_block = "I. [Statement 1]\n          II. [Statement 2]\n          III. [Statement 3]"
        count_opts = "A) 0, B) 1, C) 2, D) 3"

    elif difficulty.lower() == "medium":
        focus = "concepts and comparisons."
        distractors = "Distractors should be plausible but clearly wrong."
        stmt_logic = "Paraphrased concepts or combinations."
        stmt_count = 3
        roman_block = "I. [Concept A]\n          II. [Concept B]\n          III. [Concept C]"
        count_opts = "A) 0, B) 1, C) 2, D) 3"

    else:  # HARD
        focus = "edge cases and limitations."
        distractors = "Distractors must be highly plausible common misconceptions."
        stmt_logic = "Logical deductions (If X then Y)."
        stmt_count = 4
        roman_block = "I. [Deduction 1]\n          II. [Deduction 2]\n          III. [Deduction 3]\n          IV. [Deduction 4]"
        count_opts = "A) 0, B) 1, C) 2, D) 3, E) 4"

    # 2. Build Specific Instructions
    if q_type == "normal_multiple":
        return f"""
        [TARGET TYPE: Normal Multiple Choice]
        - QUANTITY: Generate exactly {count} questions.
        - COMPLEXITY: {difficulty.upper()}
        - GOAL: Focus on {focus}
        - OPTIONS: Provide 4 options. {distractors}
        - TEMPLATE: Question text ending with '?'
        """

    elif q_type == "statement_verification":
        return f"""
        [TARGET TYPE: Statement Verification]
        - QUANTITY: Generate exactly {count} questions.
        - COMPLEXITY: {difficulty.upper()}
        - FORMAT: "Which of the following statements is TRUE?"
        - LOGIC: {stmt_logic}
        """

    elif q_type == "statement_counting":
        return f"""
        [TARGET TYPE: Statement Counting]
        - QUANTITY: Generate exactly {count} questions.
        - COMPLEXITY: {difficulty.upper()}
        - CRITICAL RULE: Write Roman Numeral statements INSIDE the 'question_text'.

        - REQUIRED TEXT TEMPLATE:
           "Consider the following statements regarding [Topic]:
           {roman_block}

           How many of the statements above are TRUE? (Note: There are EXACTLY {stmt_count} statements)"

        - REQUIRED OPTIONS TEMPLATE:
           {count_opts}
        """

    return ""


def _transform_llm_to_api_response(
    llm_questions: list[LLMQuizQuestion], difficulty: str
) -> list[QuestionResponse]:
    """
    Transform LLM response format to API response format.

    Key transformations:
    - Convert lowercase question_type → uppercase
    - Convert lowercase difficulty → uppercase
    - Transform correct_option_id: str → choices: List[ChoiceResponse] with is_correct: bool

    Args:
        llm_questions: List of questions from LLM (with correct_option_id)
        difficulty: Difficulty level (uppercase, e.g., 'MEDIUM')

    Returns:
        List of QuestionResponse objects (with choices and is_correct)
    """
    api_questions = []

    for llm_q in llm_questions:
        # Transform options to choices with is_correct boolean
        choices = [
            ChoiceResponse(
                choice_text=opt.text,
                is_correct=(opt.id == llm_q.correct_option_id),
                explanation=opt.explanation,
            )
            for opt in llm_q.options
        ]

        # Convert question type from lowercase to uppercase
        question_type_upper = REVERSE_QUESTION_TYPE_MAP.get(
            llm_q.type, llm_q.type.upper()
        )

        # Build the QuestionResponse
        api_q = QuestionResponse(
            question_text=llm_q.question_text,
            question_type=question_type_upper,  # type: ignore[arg-type]
            explanation=llm_q.explanation,
            difficulty_level=difficulty,  # type: ignore[arg-type]
            choices=choices,
        )
        api_questions.append(api_q)

    return api_questions


def _generate_quiz_for_config(
    topic_name: str,
    topic_content: str,
    config: dict,
) -> list[QuestionResponse]:
    """
    Generate quiz questions for a single difficulty config.

    Args:
        topic_name: Name of the topic
        topic_content: Content text for the topic
        config: Quiz config dict with difficulty and quiz_type_config

    Returns:
        List of QuestionResponse objects

    Raises:
        QuizParsingException: If LLM response fails to parse
        AIServiceUnavailableException: If AI service is unreachable
    """
    difficulty_upper = config["difficulty"]
    difficulty_lower = DIFFICULTY_MAP.get(
        difficulty_upper, difficulty_upper.lower()
    )

    logger.info(
        f"Generating {difficulty_upper} quiz with "
        f"{len(config['quiz_type_config'])} question types"
    )

    # Create JsonOutputParser for LLM response
    parser = JsonOutputParser(pydantic_object=LLMGeneratedBatch)
    chain = llm_service.llm | parser

    aggregated_questions: list[LLMQuizQuestion] = []
    global_id_counter = 1

    # Process each question type in the config
    for type_req in config["quiz_type_config"]:
        q_type_upper = type_req["type"]
        q_type_lower = QUESTION_TYPE_MAP.get(q_type_upper, q_type_upper.lower())
        q_count = type_req["number"]

        logger.info(
            f"Generating {q_count} questions of type '{q_type_upper}' "
            f"at {difficulty_upper} difficulty"
        )

        # Build prompt instructions for this question type
        type_instructions = _build_single_type_instruction(
            q_type_lower, difficulty_lower, q_count
        )

        prompt = f"""
        You are a Professor creating Exam Questions.
        TOPIC: {topic_name}
        SOURCE MATERIAL:
        {topic_content}

        TASK:
        Generate exactly {q_count} questions of type '{q_type_lower}'.
        DIFFICULTY: {difficulty_upper}

        STRICT FORMATTING RULES:
        {type_instructions}

        OUTPUT INSTRUCTIONS:
        Return the result as a single valid JSON object.

        - EXPLANATIONS: Provide a brief explanation for EACH option explaining why it is correct or incorrect.
        - For correct options: Explain why the answer is correct
        - For incorrect options: Explain why the answer is incorrect (common misconception, etc.)

        REQUIRED JSON STRUCTURE (Follow this exactly):
        {{
            "questions": [
                {{
                    "id": 1,
                    "type": "{q_type_lower}",
                    "question_text": "Question goes here?",
                    "options": [
                        {{"id": "A", "text": "Option A", "explanation": "Why this option is correct/incorrect"}},
                        {{"id": "B", "text": "Option B", "explanation": "Why this option is correct/incorrect"}}
                    ],
                    "correct_option_id": "A",
                    "explanation": "Overall explanation for the question."
                }}
            ]
        }}
        """

        # Retry logic for LLM calls
        max_retries = settings.AI_MAX_RETRIES
        batch_success = False

        for attempt in range(max_retries + 1):
            try:
                # Invoke the chain
                raw_result_dict = chain.invoke(prompt)

                # Validate with Pydantic
                batch_result = LLMGeneratedBatch(**raw_result_dict)

                # Assign sequential IDs and aggregate
                for q in batch_result.questions:
                    q.id = global_id_counter
                    global_id_counter += 1
                    aggregated_questions.append(q)

                logger.info(
                    f"Successfully generated {len(batch_result.questions)} "
                    f"questions of type '{q_type_upper}'"
                )
                batch_success = True
                break

            except AuthenticationError as e:
                logger.error(
                    f"AI authentication failed for question type '{q_type_upper}': {e}"
                )
                raise AIServiceUnavailableException(
                    message="AI service authentication failed",
                    details=["Check API key configuration"],
                )

            except RateLimitError as e:
                logger.warning(
                    f"AI rate limit reached for question type '{q_type_upper}': {e}"
                )
                raise AIServiceUnavailableException(
                    message="AI service rate limit exceeded",
                    details=["Please try again later"],
                )

            except (APITimeoutError, TimeoutError) as e:
                logger.error(
                    f"AI request timeout for question type '{q_type_upper}': {e}"
                )
                raise AIServiceUnavailableException(
                    message="AI service request timed out",
                    details=[f"Timeout: {settings.AI_TIMEOUT}s"],
                )

            except OutputParserException as e:
                logger.warning(
                    f"Attempt {attempt + 1}/{max_retries + 1}: "
                    f"Failed to parse LLM response for '{q_type_upper}': {e}"
                )
                if attempt == max_retries:
                    raise QuizParsingException(
                        message=f"Failed to parse AI response for '{q_type_upper}' questions",
                        details=[str(e)],
                    )

            except Exception as e:
                logger.error(
                    f"Unexpected error generating '{q_type_upper}' questions: {e}",
                    exc_info=True,
                )
                raise QuizGenerationException(
                    message=f"Failed to generate '{q_type_upper}' questions",
                    details=[str(e)],
                )

        if not batch_success:
            logger.warning(
                f"Skipping question type '{q_type_upper}' due to repeated errors"
            )

    # Transform LLM questions to API response format
    return _transform_llm_to_api_response(aggregated_questions, difficulty_upper)


# --- PUBLIC API ---

def generate_quiz(request: GenerateQuizRequest) -> QuizResponse:
    """
    Generate quiz questions from topic content using AI.

    This is the main entry point for quiz generation. It processes all quiz
    configurations (different difficulty levels and question types) and
    aggregates the results.

    Args:
        request: GenerateQuizRequest with topicName, topicContent, and quiz_config

    Returns:
        QuizResponse with topic_title and aggregated questions list

    Raises:
        QuizRequestException: If request validation fails
        QuizParsingException: If LLM response fails to parse
        AIServiceUnavailableException: If AI service is unreachable
        QuizGenerationException: For other quiz generation errors
    """
    logger.info(f"Generating quiz for topic: {request.topicName}")

    # Validate request
    if not request.topicName or not request.topicName.strip():
        raise QuizRequestException(
            message="Topic name is required",
            details=["topicName cannot be empty"],
        )

    if not request.topicContent or not request.topicContent.strip():
        raise QuizRequestException(
            message="Topic content is required",
            details=["topicContent cannot be empty"],
        )

    if not request.quiz_config:
        raise QuizRequestException(
            message="Quiz configuration is required",
            details=["quiz_config cannot be empty"],
        )

    all_questions: list[QuestionResponse] = []

    # Process each difficulty config
    for config in request.quiz_config:
        try:
            config_dict = config.model_dump()
            questions = _generate_quiz_for_config(
                topic_name=request.topicName,
                topic_content=request.topicContent,
                config=config_dict,
            )
            all_questions.extend(questions)

        except QuizParsingException:
            # Re-raise parsing exceptions as-is
            raise

        except AIServiceUnavailableException:
            # Re-raise AI service exceptions as-is
            raise

        except QuizRequestException:
            # Re-raise request exceptions as-is
            raise

        except Exception as e:
            # Wrap other exceptions in QuizGenerationException
            logger.error(
                f"Unexpected error generating quiz for config {config}: {e}",
                exc_info=True,
            )
            raise QuizGenerationException(
                message="Failed to generate quiz",
                details=[str(e)],
            )

    logger.info(
        f"Successfully generated {len(all_questions)} total questions "
        f"for topic: {request.topicName}"
    )

    return QuizResponse(topic_title=request.topicName, questions=all_questions)
