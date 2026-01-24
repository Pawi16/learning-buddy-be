# Learning Buddy AI Service

AI-powered PDF processing and topic extraction service that uses Zhipu AI's GLM-4.7 model to analyze PDF documents and generate structured study notes.

## Features

- **PDF Analysis**: Scans PDF structure to identify headers and body text
- **AI-Powered Splitting**: Uses AI to determine optimal document splitting strategy
- **Topic Extraction**: Extracts topics with clean titles and descriptions
- **Study Notes**: Generates comprehensive summaries and study notes for each topic
- **FastAPI**: Modern, fast REST API with automatic documentation

## Prerequisites

- Python 3.13 or higher
- [uv](https://github.com/astral-sh/uv) (recommended) or pip
- Zhipu AI API key (get one at https://open.bigmodel.cn/)

## Installation

1. Clone the repository:
```bash
cd ai-service
```

2. Install dependencies:
```bash
# Using uv (recommended)
uv sync

# Or using pip
pip install -r requirements.txt
```

3. Configure environment:
```bash
cp .env.example .env
# Edit .env and add your ZHIPUAI_API_KEY
```

## Configuration

Environment variables (set in `.env`):

| Variable | Description | Default |
|----------|-------------|---------|
| `ZHIPUAI_API_KEY` | Your Zhipu AI API key | Required |
| `MODEL_NAME` | AI model to use | `glm-4.7` |
| `PORT` | Server port | `8000` |
| `HOST` | Server host | `0.0.0.0` |
| `RELOAD` | Auto-reload on code changes | `True` |
| `MAX_FILE_SIZE` | Maximum upload size (bytes) | `10485760` (10MB) |

## Running the Service

### Development Mode

Using uv (recommended):
```bash
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Or directly with Python:
```bash
python app/main.py
```

### Production Mode

```bash
# Set production environment variables
export RELOAD=false
export PORT=8000

# Run with uvicorn
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

### Change Port

Option 1: Environment variable
```bash
PORT=8080 uv run uvicorn app.main:app
```

Option 2: Modify `.env`
```bash
PORT=8080
```

## API Documentation

Once the service is running, visit:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **OpenAPI JSON**: http://localhost:8000/openapi.json

## API Endpoints

### Health Check
```bash
GET /api/v1/health
```

**Response:**
```json
{
  "status": "healthy",
  "service": "ai-service"
}
```

### Process PDF
```bash
POST /api/v1/process-pdf
```

**Request:**
- Content-Type: `multipart/form-data`
- Body: `file` (PDF file, max 10MB)

**Response:**
```json
[
  {
    "title": "Topic Title",
    "description": "Topic description",
    "raw_text": "Raw content text",
    "summary_notes": "# Markdown formatted\\n\\nStudy notes"
  }
]
```

**Example using curl:**
```bash
curl -X POST \
  http://localhost:8000/api/v1/process-pdf \
  -F "file=@document.pdf"
```

**Example using Python:**
```python
import requests

with open("document.pdf", "rb") as f:
    response = requests.post(
        "http://localhost:8000/api/v1/process-pdf",
        files={"file": f}
    )
topics = response.json()
```

## Project Structure

```
ai-service/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI application entry point
│   ├── api/
│   │   ├── __init__.py
│   │   └── routes.py        # API endpoints
│   ├── core/
│   │   ├── __init__.py
│   │   └── config.py        # Configuration settings
│   ├── schemas/
│   │   ├── __init__.py
│   │   └── pdf.py           # Pydantic models
│   └── services/
│       ├── __init__.py
│       ├── llm_service.py   # AI/LLM integration
│       └── pdf_service.py   # PDF processing logic
├── data/
│   └── temp/                # Temporary file storage
├── .env                     # Environment configuration (not in git)
├── .env.example             # Environment template
├── .gitignore
├── pyproject.toml           # Project dependencies
├── uv.lock                  # Locked dependencies
└── README.md
```

## Development

### Adding New Dependencies

```bash
uv add <package-name>
```

### Running Tests

```bash
# Add pytest first
uv add --dev pytest

# Run tests
uv run pytest
```

## Troubleshooting

### Import Errors
If you get import errors, ensure all `__init__.py` files are present:
```bash
find app -type d -exec touch {}/__init__.py \;
```

### API Key Issues
Make sure your `.env` file contains a valid `ZHIPUAI_API_KEY`.

### Port Already in Use
Change the port in `.env` or use the `PORT` environment variable:
```bash
PORT=8080 uv run uvicorn app.main:app
```

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
