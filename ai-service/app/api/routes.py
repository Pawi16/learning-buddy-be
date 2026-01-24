import logging
import os
import shutil
from pathlib import Path
from typing import Annotated

from fastapi import APIRouter, File, HTTPException, UploadFile, status

from app.core.config import settings
from app.schemas.pdf import ProcessedTopic
from app.services import llm_service, pdf_service

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

router = APIRouter()


def validate_file_upload(file: UploadFile) -> None:
    """Validate file upload for type, size, and filename."""
    if not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No filename provided"
        )

    # Check file extension
    file_ext = Path(file.filename).suffix.lower()
    if file_ext not in settings.ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail=f"Unsupported file type. Allowed types: {settings.ALLOWED_EXTENSIONS}"
        )

    # Read and validate file size
    file.file.seek(0, os.SEEK_END)
    file_size = file.file.tell()
    file.file.seek(0)  # Reset pointer

    if file_size > settings.MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"File too large. Maximum size: {settings.MAX_FILE_SIZE / (1024 * 1024):.1f}MB"
        )

    if file_size == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Empty file"
        )


def sanitize_filename(filename: str) -> str:
    """Sanitize filename to prevent path traversal attacks."""
    # Keep only safe characters
    safe_filename = "".join(c for c in filename if c.isalnum() or c in "._-")
    return safe_filename or "upload.pdf"


@router.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "ai-service"}


@router.post("/process-pdf", response_model=list[ProcessedTopic])
async def process_pdf(
    file: Annotated[UploadFile, File(...)]
) -> list[ProcessedTopic]:
    """
    Process a PDF file and extract topics with AI-generated summaries.

    Args:
        file: PDF file to process (max 10MB)

    Returns:
        List of processed topics with titles, descriptions, and summaries
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
        split_sizes, body_size = llm_service.determine_split_strategy(stats)

        # 4. Split content into topics
        logger.info(f"Splitting content into {len(split_sizes)} topics...")
        raw_topics = pdf_service.split_content(lines, split_sizes, body_size)

        # 5. Enrich topics with AI-generated summaries
        logger.info("Enriching topics with AI summaries...")
        final_topics = llm_service.enrich_topics(raw_topics)

        logger.info(f"Successfully processed {len(final_topics)} topics")
        return final_topics

    except HTTPException:
        # Re-raise HTTP exceptions as-is
        raise
    except Exception as e:
        logger.error(f"Error processing PDF: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="An error occurred while processing the PDF"
        )
    finally:
        # Clean up temporary file
        if os.path.exists(temp_path):
            try:
                os.remove(temp_path)
                logger.debug(f"Cleaned up temporary file: {temp_path}")
            except Exception as e:
                logger.warning(f"Failed to clean up temporary file: {e}")