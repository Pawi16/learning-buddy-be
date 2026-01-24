from langchain_openai import ChatOpenAI
from app.core.config import settings
from app.schemas.pdf import SplitStrategy, TopicEnrichment, ProcessedTopic
import json

# --- INITIALIZE ZHIPU AI (GLM-4) ---
llm = ChatOpenAI(
    model=settings.MODEL_NAME,
    api_key=settings.ZHIPUAI_API_KEY,
    base_url=settings.ZHIPU_BASE_URL,
    temperature=0.1,  # Keep low for consistent JSON
    max_retries=2,
)

def determine_split_strategy(font_stats) -> tuple[list[float], float]:
    """
    Asks AI to analyze font statistics and return header sizes.
    """
    if not font_stats: return [18.0], 12.0
    
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
    except Exception as e:
        print(f"⚠️ AI Split Error: {e}")
        # Fallback: Return a sensible default if AI fails
        return [body_size + 2.0], body_size

def enrich_topics(raw_topics) -> list[ProcessedTopic]:
    final_results = []
    print(f"🧠 Enriching {len(raw_topics)} topics using {settings.MODEL_NAME}...")

    for index, topic in enumerate(raw_topics):
        # Skip very short topics (likely junk)
        if len(topic['content']) < 50: 
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
        except Exception as e:
            print(f"❌ Error enriching topic {index + 1}: {e}")
            # Fallback logic so the whole process doesn't crash
            final_results.append(ProcessedTopic(
                order_index=index + 1,
                title=topic['title'],
                description="Content extracted from PDF.",
                raw_text=topic['content'],
                summary_note="> AI Generation failed. Raw text available."
            ))
            
    return final_results