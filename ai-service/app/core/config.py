from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    # Zhipu AI Config
    ZHIPUAI_API_KEY: str  # We will load this from .env
    MODEL_NAME: str = "GLM-4.7"
    ZHIPU_BASE_URL: str = "https://api.z.ai/api/coding/paas/v4"

    # Server Config
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    RELOAD: bool = True

    # File Upload Config
    MAX_FILE_SIZE: int = 10 * 1024 * 1024  # 10MB
    ALLOWED_EXTENSIONS: set[str] = {".pdf"}

    TEMP_FOLDER: str = "data/temp"
    
    class Config:
        env_file = ".env"

settings = Settings()