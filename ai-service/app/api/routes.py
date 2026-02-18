"""API routes for PDF processing."""

import logging
import os
import shutil
import time
from pathlib import Path
from typing import Annotated

from fastapi import APIRouter, File, UploadFile
# 1. ADD THESE IMPORTS to fix the Java Client error
from fastapi.responses import JSONResponse
from fastapi.encoders import jsonable_encoder

from app.core.config import settings
from app.exceptions.base import AppException
from app.exceptions.validation import (
    MissingFilenameException,
    EmptyFileException,
    UnsupportedFileTypeException,
    FileTooLargeException,
)
from app.schemas.pdf import ProcessedTopic
from app.schemas.quiz import GenerateQuizRequest, QuizResponse
from app.schemas.flashcard import GenerateFlashcardRequest, FlashcardDeckResponse
from app.services import llm_service, pdf_service, quiz_service, flashcard_service
from app.exceptions.quiz_generation import QuizGenerationException
from app.exceptions.flashcard_generation import FlashcardGenerationException

logger = logging.getLogger(__name__)

router = APIRouter()


def validate_file_upload(file: UploadFile) -> None:
    """
    Validate file upload for type, size, and filename.
    """
    if not file.filename:
        raise MissingFilenameException()

    # Check file extension
    file_ext = Path(file.filename).suffix.lower()
    if file_ext not in settings.ALLOWED_EXTENSIONS:
        raise UnsupportedFileTypeException(settings.ALLOWED_EXTENSIONS)

    # Read and validate file size
    file.file.seek(0, os.SEEK_END)
    file_size = file.file.tell()
    file.file.seek(0)  # Reset pointer

    if file_size > settings.MAX_FILE_SIZE:
        max_size_mb = settings.MAX_FILE_SIZE / (1024 * 1024)
        raise FileTooLargeException(max_size_mb)

    if file_size == 0:
        raise EmptyFileException()


def sanitize_filename(filename: str) -> str:
    """
    Sanitize filename to prevent path traversal attacks.
    """
    safe_filename = "".join(c for c in filename if c.isalnum() or c in "._-")
    return safe_filename or "upload.pdf"


@router.get("/health")
async def health_check() -> dict:
    """Health check endpoint."""
    return {"status": "healthy", "service": "ai-service"}


@router.post("/generate-quiz")
async def generate_quiz(request: GenerateQuizRequest) -> JSONResponse:
    """Generate quiz questions from topic content using AI."""
    logger.info(f"Generating quiz for topic: {request.topicName}")
    try:
        result = quiz_service.generate_quiz(request)
        return JSONResponse(content=jsonable_encoder(result))
    except AppException:
        raise  # Let global handler catch
    except Exception as e:
        logger.error(f"Unexpected error in quiz generation: {e}", exc_info=True)
        raise QuizGenerationException(
            message="Failed to generate quiz",
            details=[str(e)]
        )


@router.post("/generate-flashcard")
async def generate_flashcard(request: GenerateFlashcardRequest) -> JSONResponse:
    """Generate flashcards from topic content using AI."""
    logger.info(f"Generating flashcard for topic: {request.topicName}")
    try:
        result = flashcard_service.generate_flashcard(request)
        return JSONResponse(content=jsonable_encoder(result))
    except AppException:
        raise  # Let global handler catch
    except Exception as e:
        logger.error(f"Unexpected error in flashcard generation: {e}", exc_info=True)
        raise FlashcardGenerationException(
            message="Failed to generate flashcard",
            details=[str(e)]
        )


@router.post("/process-pdf")
async def process_pdf(
    file: Annotated[UploadFile, File(...)]
) -> JSONResponse: # <--- Changed return type hint
    """
    Process a PDF file and extract topics with AI-generated summaries.
    """
    # Validate file
    validate_file_upload(file)

    # Sanitize filename
    safe_filename = sanitize_filename(file.filename)
    temp_path = os.path.join(settings.TEMP_FOLDER, safe_filename)

    logger.info(f"Processing PDF: {safe_filename}")

    try:
        # 1. Save file
        os.makedirs(settings.TEMP_FOLDER, exist_ok=True)
        with open(temp_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # 2. Scan PDF for font statistics
        logger.info("Scanning PDF for font statistics...")
        stats, lines = pdf_service.scan_pdf_stats(temp_path)

        # 3. Determine splitting strategy using AI
        logger.info("Determining splitting strategy...")
        # Note: calling this synchronously as requested
        split_sizes, body_size = llm_service.determine_split_strategy(stats)

        # 4. Split content into topics
        logger.info(f"Splitting content into {len(split_sizes)} topics...")
        raw_topics = pdf_service.split_content(lines, split_sizes, body_size)

        # 5. Enrich topics with AI-generated summaries
        logger.info("Enriching topics with AI summaries...")
        start_time = time.time()
        # Note: calling this synchronously as requested
        final_topics = llm_service.enrich_topics(raw_topics)
        elapsed_time = time.time() - start_time
        logger.info(f"Successfully enriched {len(final_topics)} topics in {elapsed_time:.2f} seconds")

        logger.info(f"Successfully processed {len(final_topics)} topics")
        
        # 6. RETURN JSON RESPONSE (The Fix)
        # We explicitly wrap the result in JSONResponse.
        # This ensures the header is 'Content-Type: application/json'
        # instead of 'application/octet-stream', so your Java client won't crash.
        return JSONResponse(content=jsonable_encoder(final_topics))

    finally:
        # Clean up temporary file
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
                logger.debug(f"Cleaned up temporary file: {temp_path}")
            except Exception as e:
                logger.warning(f"Failed to clean up temporary file: {e}")