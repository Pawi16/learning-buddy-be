import logging

from langchain_openai import ChatOpenAI
from langchain_core.exceptions import OutputParserException
from openai import AuthenticationError, RateLimitError, APITimeoutError

from app.core.config import settings
from app.schemas.pdf import SplitStrategy, TopicEnrichment, ProcessedTopic
from app.exceptions.ai_service import (
    AIStrategyException,
    AIEnrichmentException,
    AIServiceUnavailableException,
    AIModelException,
)

logger = logging.getLogger(__name__)

# --- INITIALIZE ZHIPU AI (GLM-4) ---
llm = ChatOpenAI(
    model=settings.MODEL_NAME,
    api_key=settings.ZHIPUAI_API_KEY,
    base_url=settings.ZHIPU_BASE_URL,
    temperature=0.1,  # Keep low for consistent JSON
    max_retries=settings.AI_MAX_RETRIES,
    timeout=settings.AI_TIMEOUT,
)

def determine_split_strategy(font_stats) -> tuple[list[float], float]:
    """
    Asks AI to analyze font statistics and return header sizes.

    Raises:
        AIStrategyException: If AI fails to determine split strategy
        AIServiceUnavailableException: If AI service is unreachable
    """
    if not font_stats:
        logger.warning("No font statistics provided, using default split strategy")
        return [18.0], 12.0
    
    # Logic to find body size (most common font)
    body_size = max(font_stats, key=lambda k: font_stats[k]['count'])
    
    # Build Context (Limit to top 20 sizes to save tokens)
    report = []
    for size in sorted(font_stats.keys(), reverse=True)[:20]: 
        d = font_stats[size]
        # Clean examples to avoid JSON breaking characters
        examples = [ex.replace('"', "'").replace('\n', ' ')[:50] for ex in d['examples'][:3]]
        report.append(f"SIZE {size} | Occurrences: {d['count']} | Examples: {examples}")
    
    context = "\n".join(report)

    prompt = f"""
    You are a Document Layout Engineer.
    Analyze these font stats from a PDF to find the **Topic Headers**.
    
    STATS:
    {context}
    
    RULES:
    1. The Body Text size is likely {body_size}. IGNORE this size.
    2. IGNORE sizes smaller than {body_size}.
    3. Look for sizes with moderate occurrences (5-50) that look like titles.
    4. You MUST return valid JSON matching this structure:
    {{
      "target_font_sizes": [float, float],
      "reasoning": "string"
    }}
    """
    
    try:
        # Method="json_mode" is often more stable for Zhipu than "function_calling"
        response = llm.with_structured_output(SplitStrategy, method="json_mode").invoke(prompt)
        return response.target_font_sizes, body_size
    except AuthenticationError as e:
        logger.error(f"AI authentication failed during split strategy determination: {e}")
        raise AIServiceUnavailableException(
            message="AI service authentication failed",
            details=["Check API key configuration"]
        )
    except RateLimitError as e:
        logger.warning(f"AI rate limit reached during split strategy determination: {e}")
        raise AIServiceUnavailableException(
            message="AI service rate limit exceeded",
            details=["Please try again later"]
        )
    except (APITimeoutError, TimeoutError) as e:
        logger.error(f"AI request timeout during split strategy determination: {e}")
        raise AIServiceUnavailableException(
            message="AI service request timed out",
            details=[f"Timeout: {settings.AI_TIMEOUT}s"]
        )
    except OutputParserException as e:
        logger.error(f"AI output parsing failed during split strategy determination: {e}")
        raise AIModelException(
            message="Failed to parse AI response for split strategy",
            details=["The AI returned invalid JSON format"]
        )
    except Exception as e:
        logger.error(f"Unexpected AI error during split strategy determination: {e}", exc_info=True)
        raise AIStrategyException(
            message="Failed to analyze document structure",
            details=[str(e), "Fallback to default headers failed"]
        )

def enrich_topics(raw_topics) -> list[ProcessedTopic]:
    """
    Enriches raw topics with AI-generated content.

    Raises:
        AIEnrichmentException: If AI fails to enrich any topic
        AIServiceUnavailableException: If AI service is unreachable
    """
    if not raw_topics:
        logger.warning("No topics provided for enrichment")
        return []

    logger.info(f"Enriching {len(raw_topics)} topics using {settings.MODEL_NAME}...")
    final_results = []

    for index, topic in enumerate(raw_topics):
        # Log progress every 5 topics
        if (index + 1) % 5 == 0:
            logger.info(f"Progress: {index + 1}/{len(raw_topics)} topics enriched")

        # Skip very short topics (likely junk)
        if len(topic['content']) < 50:
            logger.debug(f"Skipping topic {index + 1} (too short: {len(topic['content'])} chars)")
            continue

        prompt = f"""
        You are a Course Creator. Refine this raw PDF content into a study topic.
        
        RAW TITLE: {topic['title']}
        RAW CONTENT (Truncated):
        {topic['content'][:3000]}
        
        TASK:
        1. "clean_title": Fix typos, remove numbers like '1.1'.
        2. "description": Write a 2-sentence summary of what this topic covers.
        3. "summary_note": Write detailed study notes in Markdown format (use bullet points).
        
        Return JSON only.
        """
        
        try:
            # We use with_structured_output to force the Pydantic schema
            enrichment = llm.with_structured_output(TopicEnrichment, method="json_mode").invoke(prompt)

            final_results.append(ProcessedTopic(
                order_index=index + 1,
                title=enrichment.clean_title,
                description=enrichment.description,
                raw_text=topic['content'],
                summary_note=enrichment.summary_note
            ))
        except AuthenticationError as e:
            logger.error(f"AI authentication failed while enriching topic {index + 1}: {e}")
            raise AIServiceUnavailableException(
                message="AI service authentication failed",
                details=["Check API key configuration"]
            )
        except RateLimitError as e:
            logger.warning(f"AI rate limit reached while enriching topic {index + 1}: {e}")
            raise AIServiceUnavailableException(
                message="AI service rate limit exceeded",
                details=["Please try again later"]
            )
        except (APITimeoutError, TimeoutError) as e:
            logger.error(f"AI request timeout while enriching topic {index + 1}: {e}")
            raise AIServiceUnavailableException(
                message="AI service request timed out",
                details=[f"Timeout: {settings.AI_TIMEOUT}s"]
            )
        except OutputParserException as e:
            logger.error(f"AI output parsing failed for topic {index + 1}: {e}")
            raise AIEnrichmentException(
                topic_index=index + 1,
                message=f"Failed to parse AI response for topic {index + 1}",
                details=[f"Topic: {topic.get('title', 'Unknown')}", "The AI returned invalid JSON format"]
            )
        except Exception as e:
            logger.error(f"Unexpected AI error while enriching topic {index + 1}: {e}", exc_info=True)
            raise AIEnrichmentException(
                topic_index=index + 1,
                message=f"Failed to enrich topic {index + 1}",
                details=[f"Topic: {topic.get('title', 'Unknown')}", str(e)]
            )
            
    return final_results