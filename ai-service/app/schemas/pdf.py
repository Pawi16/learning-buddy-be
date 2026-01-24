from pydantic import BaseModel, Field
from typing import List

class ProcessedTopic(BaseModel):
    order_index: int
    title: str
    description: str
    raw_text: str
    summary_note: str

# Internal use only (for AI structured output)
class TopicEnrichment(BaseModel):
    clean_title: str
    description: str
    summary_note: str

class SplitStrategy(BaseModel):
    target_font_sizes: List[float]
    reasoning: str