from pydantic import BaseModel, Field
from typing import List, Literal
# --- REQUEST MODELS ---
class QuestionTypeRequest(BaseModel):
    # ✅ UPDATED: Strict Upper Case Enums
    type: Literal["NORMAL_MULTIPLE", "STATEMENT_VERIFICATION", "STATEMENT_COUNTING"]
    number: int

class QuizConfig(BaseModel):
    # ✅ UPDATED: Strict Upper Case Enums
    difficulty: Literal["EASY", "MEDIUM", "HARD"]
    quiz_type_config: List[QuestionTypeRequest]

class GenerateQuizRequest(BaseModel):
    topicName: str
    topicContent: str
    quiz_config: List[QuizConfig]

# --- INTERNAL LLM MODELS (Parsing) ---
class LLMQuestionOption(BaseModel):
    id: str
    text: str
    explanation: str

class LLMQuizQuestion(BaseModel):
    id: int
    type: str 
    question_text: str
    options: List[LLMQuestionOption]
    correct_option_id: str
    explanation: str

class LLMGeneratedBatch(BaseModel):
    questions: List[LLMQuizQuestion]

# --- RESPONSE MODELS (Database Schema) ---
class ChoiceResponse(BaseModel):
    choice_text: str
    is_correct: bool
    explanation: str

class QuestionResponse(BaseModel):
    question_text: str
    # ✅ UPDATED: Output strict Enum string
    question_type: Literal["NORMAL_MULTIPLE", "STATEMENT_VERIFICATION", "STATEMENT_COUNTING"] 
    explanation: str
    # ✅ UPDATED: Output strict Enum string
    difficulty_level: Literal["EASY", "MEDIUM", "HARD"]
    choices: List[ChoiceResponse]

class QuizResponse(BaseModel):
    topic_title: str
    questions: List[QuestionResponse]