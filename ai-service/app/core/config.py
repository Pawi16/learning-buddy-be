from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Zhipu AI Config
    ZHIPUAI_API_KEY: str  # We will load this from .env
    MODEL_NAME: str = "GLM-4.5"
    ZHIPU_BASE_URL: str = "https://api.z.ai/api/coding/paas/v4"

    # Server Config
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    RELOAD: bool = True

    # File Upload Config
    MAX_FILE_SIZE: int = 10 * 1024 * 1024  # 10MB
    ALLOWED_EXTENSIONS: set[str] = {".pdf"}

    TEMP_FOLDER: str = "data/temp"

    # AI Service Retry Config
    AI_MAX_RETRIES: int = 2
    AI_RETRY_DELAY: float = 2.0
    AI_TIMEOUT: int = 90
    AI_CONCURRENCY_LIMIT: int = 5

    # Error Handling Config
    INCLUDE_ERROR_DETAILS: bool = False  # Set True for development

    class Config:
        env_file = ".env"

settings = Settings()