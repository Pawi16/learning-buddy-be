"""PDF processing service for extracting text and analyzing fonts."""

import logging
import re
from collections import defaultdict

import pdfplumber

from app.exceptions.pdf_processing import (
    PDFParsingException,
    PDFCorruptedException,
    PDFStructureException,
)

logger = logging.getLogger(__name__)


def is_junk(text: str, top: float, page_height: float) -> bool:
    """
    Filters out headers, footers, and page numbers based on position and content.

    Args:
        text: Text line to evaluate
        top: Top position of the text on the page
        page_height: Height of the page

    Returns:
        True if the text should be filtered out, False otherwise
    """
    # 1. Geometry: Ignore top 10% and bottom 10% of the page (usually headers/footers)
    if top < (page_height * 0.10) or top > (page_height * 0.90):
        return True

    # 2. Content: Ignore standalone numbers (Page numbers)
    if re.match(r"^\d+$", text.strip()):
        return True

    # 3. Content: Ignore common junk words
    if text.strip().lower().startswith(("figure", "table", "box", "continued")):
        return True

    return False


def scan_pdf_stats(pdf_path: str) -> tuple[dict, list]:
    """
    Scan the PDF to build a frequency map of font sizes.

    Args:
        pdf_path: Path to the PDF file

    Returns:
        Tuple of (font_stats_dict, list_of_all_text_lines)

    Raises:
        PDFParsingException: If the PDF cannot be opened or parsed
        PDFCorruptedException: If the PDF file is corrupted
    """
    font_stats = defaultdict(lambda: {"count": 0, "total_len": 0, "examples": []})
    all_lines = []

    try:
        with pdfplumber.open(pdf_path) as pdf:
            if not pdf.pages:
                raise PDFStructureException(
                    message="PDF has no pages",
                    details=["The PDF file appears to be empty or corrupted"],
                )

            logger.info(f"Scanning PDF with {len(pdf.pages)} pages")

            # Scan all pages
            for page_num, page in enumerate(pdf.pages, start=1):
                try:
                    words = page.extract_words(extra_attrs=["size", "top"])
                    if not words:
                        logger.debug(f"No words found on page {page_num}")
                        continue

                    # Group words into lines
                    current_line = [words[0]]
                    for word in words[1:]:
                        # If word is on the same vertical level (within 5px tolerance)
                        if abs(word["top"] - current_line[-1]["top"]) < 5:
                            current_line.append(word)
                        else:
                            # New line detected
                            _record_line(current_line, font_stats, all_lines, page.height)
                            current_line = [word]

                    # Record the last line of the page
                    _record_line(current_line, font_stats, all_lines, page.height)

                except Exception as e:
                    logger.error(f"Error processing page {page_num}: {e}", exc_info=True)
                    raise PDFCorruptedException(
                        message=f"Failed to extract content from page {page_num}",
                        details=[str(e)],
                    )

            if not all_lines:
                raise PDFStructureException(
                    message="No extractable content found in PDF",
                    details=["The PDF may be image-based or encrypted"],
                )

            logger.info(
                f"Extracted {len(all_lines)} lines, "
                f"found {len(font_stats)} distinct font sizes"
            )

    except pdfplumber.pdfminer.pdfparser.PDFSyntaxError as e:
        logger.error(f"PDF syntax error: {e}", exc_info=True)
        raise PDFCorruptedException(
            message="PDF file is corrupted or has invalid syntax",
            details=[str(e)],
        )
    except (IOError, OSError) as e:
        logger.error(f"Failed to open PDF file: {e}", exc_info=True)
        raise PDFParsingException(
            message=f"Failed to open PDF file: {str(e)}",
        )
    except (PDFCorruptedException, PDFStructureException):
        # Re-raise our custom exceptions
        raise
    except Exception as e:
        logger.error(f"Unexpected error scanning PDF: {e}", exc_info=True)
        raise PDFParsingException(
            message=f"Failed to scan PDF: {str(e)}",
        )

    return font_stats, all_lines


def _record_line(words: list, stats: dict, all_lines: list, h: float) -> None:
    """
    Helper to record a single line's stats.

    Args:
        words: List of word dictionaries
        stats: Font statistics dictionary to update
        all_lines: List to append the line to
        h: Page height
    """
    if not words:
        return

    # Determine the "dominant" size of the line (usually the max size found)
    size = round(max([w["size"] for w in words]), 1)
    text = " ".join([w["text"] for w in words])

    # Update Stats
    stats[size]["count"] += 1
    stats[size]["total_len"] += len(text)

    # Keep 5 samples of this font size for the AI to analyze later
    if len(stats[size]["examples"]) < 5:
        stats[size]["examples"].append(text[:100])

    # Store raw line for the splitting phase
    all_lines.append({"text": text, "size": size, "top": words[0]["top"], "height": h})


def split_content(
    all_lines: list, target_sizes: list[float], body_size: float
) -> list[dict]:
    """
    Split the raw text lines into topics based on the Header Sizes provided by AI.

    Args:
        all_lines: List of line dictionaries with text, size, top, height
        target_sizes: List of font sizes to use as topic headers
        body_size: The body text font size

    Returns:
        List of topic dictionaries with title and content

    Raises:
        PDFStructureException: If splitting fails
    """
    # Safety: Ensure we don't accidentally split on body text
    valid_targets = {t for t in target_sizes if t > body_size}

    if not valid_targets:
        logger.warning("No valid header sizes found, treating entire document as one topic")
        valid_targets = {max(target_sizes) if target_sizes else body_size + 2.0}

    topics = []
    current_topic = {"title": "Introduction", "content": ""}
    current_header_size = 0

    for line in all_lines:
        text = re.sub(r"\(cid:\d+\)", "", line["text"]).strip()  # Clean PDF artifacts
        size = line["size"]

        # Skip Headers/Footers
        if is_junk(text, line["top"], line["height"]):
            continue

        # Check if this line matches one of our "Header Font Sizes"
        is_header = any(abs(size - t) < 0.2 for t in valid_targets)

        if is_header:
            # Logic: Is this a continuation of the CURRENT header? (Multi-line titles)
            # If size matches current header AND content is short, append to title.
            if abs(size - current_header_size) < 0.2 and len(current_topic["content"]) < 10:
                current_topic["title"] += " " + text
            else:
                # Save previous topic if it has content
                if len(current_topic["content"]) > 50:
                    topics.append(current_topic)

                # Start new topic
                current_topic = {"title": text, "content": ""}
                current_header_size = size
        else:
            # It's just body text, append to current topic
            current_topic["content"] += text + "\n"

    # Don't forget the last topic
    if current_topic["content"]:
        topics.append(current_topic)

    if not topics:
        raise PDFStructureException(
            message="Failed to split content into topics",
            details=["No valid topics could be identified from the PDF structure"],
        )

    logger.info(f"Split content into {len(topics)} topics")
    return topics
