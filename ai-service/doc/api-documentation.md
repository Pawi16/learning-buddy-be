# Learning Buddy AI Service - API Documentation

## Overview

The Learning Buddy AI Service provides AI-powered PDF processing and topic extraction. It uses Zhipu AI's GLM-4.7 model to analyze PDF documents, identify topics, and generate comprehensive study notes.

**Base URL:** `http://localhost:8000`
**API Version:** v1
**Content Type:** `application/json` (except for file uploads)

---

## Authentication

Currently, this service does not require authentication. This may change in future versions.

---

## Endpoints

### 1. Health Check

Check if the service is running.

**Endpoint:** `GET /api/v1/health`

**Request:**
```http
GET /api/v1/health HTTP/1.1
Host: localhost:8000
```

**Response:** `200 OK`
```json
{
  "status": "healthy",
  "service": "ai-service"
}
```

**Example:**
```bash
curl http://localhost:8000/api/v1/health
```

---

### 2. Process PDF

Upload and process a PDF file to extract topics with AI-generated summaries.

**Endpoint:** `POST /api/v1/process-pdf`

**Content-Type:** `multipart/form-data`

#### Request Parameters

| Parameter | Type   | Required | Description                              |
|-----------|--------|----------|------------------------------------------|
| file      | file   | Yes      | PDF file to process (max 10MB)          |

#### Request Example

```bash
curl -X POST \
  http://localhost:8000/api/v1/process-pdf \
  -F "file=@document.pdf"
```

```python
import requests

with open("document.pdf", "rb") as f:
    response = requests.post(
        "http://localhost:8000/api/v1/process-pdf",
        files={"file": f}
    )
topics = response.json()
```

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://localhost:8000/api/v1/process-pdf', {
  method: 'POST',
  body: formData
})
.then(response => response.json())
.then(topics => console.log(topics));
```

#### Response

**Success:** `200 OK`

Returns an array of processed topics:

```json
[
  {
    "order_index": 0,
    "title": "Introduction to Machine Learning",
    "description": "Overview of ML concepts and terminology",
    "raw_text": "Machine learning is a subset of artificial intelligence...",
    "summary_note": "# Introduction to Machine Learning\n\n## Key Concepts\n\n- Supervised Learning\n- Unsupervised Learning\n- Reinforcement Learning\n\n..."
  },
  {
    "order_index": 1,
    "title": "Neural Networks",
    "description": "Deep learning with neural network architectures",
    "raw_text": "Neural networks are computing systems inspired by biological neural networks...",
    "summary_note": "# Neural Networks\n\n## Architecture\n\n- Input Layer\n- Hidden Layers\n- Output Layer\n\n..."
  }
]
```

#### Response Schema

| Field        | Type    | Description                                      |
|--------------|---------|--------------------------------------------------|
| order_index  | integer | Sequential order of the topic in the document    |
| title        | string  | AI-generated title for the topic                 |
| description  | string  | Brief description of the topic content           |
| raw_text     | string  | Raw text content extracted from the PDF          |
| summary_note | string  | AI-generated summary notes in Markdown format    |

#### Error Responses

| Status Code | Description                            | Response Body Example                       |
|-------------|----------------------------------------|--------------------------------------------|
| 400         | Bad Request (no filename or empty file) | `{"detail": "No filename provided"}`       |
| 413         | Request Entity Too Large (>10MB)       | `{"detail": "File too large. Maximum size: 10.0MB"}` |
| 415         | Unsupported Media Type (not PDF)      | `{"detail": "Unsupported file type. Allowed types: {'.pdf'}"}` |
| 500         | Internal Server Error                 | `{"detail": "An error occurred while processing the PDF"}` |

#### Validation Rules

- File must have `.pdf` extension (case-insensitive)
- File size must not exceed 10MB
- File must not be empty
- File must be a valid PDF format

---

## Data Models

### ProcessedTopic

A topic extracted from a PDF with AI-generated summaries.

```yaml
type: object
properties:
  order_index:
    type: integer
    minimum: 0
    description: Sequential order of the topic
  title:
    type: string
    minLength: 1
    description: Clean title for the topic
  description:
    type: string
    minLength: 1
    description: Brief description of the topic
  raw_text:
    type: string
    minLength: 1
    description: Raw extracted text from PDF
  summary_note:
    type: string
    minLength: 1
    description: AI-generated summary in Markdown format
required:
  - order_index
  - title
  - description
  - raw_text
  - summary_note
```

---

## Interactive Documentation

When the service is running, you can access interactive API documentation:

- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc

These interfaces allow you to:
- Browse all endpoints
- View request/response schemas
- Test API calls directly from your browser

---

## Rate Limiting

Currently, there are no rate limits imposed. This may be added in future versions.

---

## CORS

Cross-Origin Resource Sharing (CORS) is enabled for all origins by default. Configure appropriately for production.

---

## Error Handling

All errors follow a consistent format:

```json
{
  "detail": "Error message describing what went wrong"
}
```

The service uses standard HTTP status codes:

- **2xx** - Success
- **4xx** - Client error (invalid request)
- **5xx** - Server error (try again later)

---

## OpenAPI Specifications

This API includes machine-readable OpenAPI specifications:

- **JSON:** `doc/openapi.json`
- **YAML:** `doc/openapi.yaml`

Use these to:
- Generate client SDKs with [openapi-generator](https://openapi-generator.tech)
- Validate requests with [openapi-schema-validator](https://github.com/p1c2u/openapi-schema-validator)
- Import into API documentation tools like Swagger UI, Postman, or Insomnia

---

## JSON Schemas

Request and response schemas are available in `doc/schemas/` for validation:

### Request Schemas
- `schemas/requests/process-pdf.json` - PDF processing request schema

### Response Schemas
- `schemas/responses/health.json` - Health check response schema
- `schemas/responses/process-pdf.json` - PDF processing response schema

**Example validation with Python:**

```python
import json
from jsonschema import validate, ValidationError

# Load schema
with open('doc/schemas/responses/process-pdf.json') as f:
    schema = json.load(f)

# Validate response
try:
    validate(instance=response_data, schema=schema)
    print("✓ Valid response")
except ValidationError as e:
    print(f"✗ Invalid response: {e.message}")
```

---

## Version History

| Version | Date       | Changes                  |
|---------|------------|--------------------------|
| 0.1.0   | 2026-01-24 | Initial release          |

---

## Support

For issues or questions:
- Check the service logs
- Review the OpenAPI specifications in `doc/openapi.json`
- Test endpoints using Swagger UI at http://localhost:8000/docs
