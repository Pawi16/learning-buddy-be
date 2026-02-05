# Learning Buddy AI Service - API Documentation

## Overview

The Learning Buddy AI Service provides AI-powered PDF processing, topic extraction, and quiz generation. It uses Zhipu AI's GLM-4.5 model to analyze PDF documents, identify topics, generate comprehensive study notes, and create quizzes.

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

### 2. Generate Quiz

Generate quiz questions based on a topic and its content.

**Endpoint:** `POST /api/v1/generate-quiz`

**Content-Type:** `application/json`

#### Request Parameters

| Parameter     | Type   | Required | Description                              |
|---------------|--------|----------|------------------------------------------|
| topicName     | string | Yes      | Name of the topic to generate quiz for   |
| topicContent  | string | Yes      | Content/text about the topic             |
| quiz_config   | array  | Yes      | Quiz configuration array                 |

#### Quiz Config Structure

| Field              | Type   | Required | Description                              |
|--------------------|--------|----------|------------------------------------------|
| difficulty         | string | Yes      | Difficulty level: EASY, MEDIUM, or HARD  |
| quiz_type_config   | array  | Yes      | Array of question type specifications    |

#### Question Type Specification

| Field   | Type   | Required | Description                                                                    |
|---------|--------|----------|--------------------------------------------------------------------------------|
| type    | string | Yes      | Question type: NORMAL_MULTIPLE, STATEMENT_VERIFICATION, or STATEMENT_COUNTING  |
| number  | int    | Yes      | Number of questions of this type to generate                                   |

#### Request Examples

```bash
curl -X POST \
  http://localhost:8000/api/v1/generate-quiz \
  -H "Content-Type: application/json" \
  -d '{
    "topicName": "Builder Pattern",
    "topicContent": "The Builder pattern is a creational design pattern that separates the construction of a complex object from its representation.",
    "quiz_config": [
      {
        "difficulty": "EASY",
        "quiz_type_config": [
          {"type": "NORMAL_MULTIPLE", "number": 2}
        ]
      }
    ]
  }'
```

```python
import requests

response = requests.post(
    "http://localhost:8000/api/v1/generate-quiz",
    json={
        "topicName": "Builder Pattern",
        "topicContent": "The Builder pattern is a creational design pattern that separates the construction of a complex object from its representation.",
        "quiz_config": [
            {
                "difficulty": "EASY",
                "quiz_type_config": [{"type": "NORMAL_MULTIPLE", "number": 2}]
            }
        ]
    }
)
quiz = response.json()
```

```javascript
fetch('http://localhost:8000/api/v1/generate-quiz', {
  method: 'POST',
  headers: {'Content-Type': 'application/json'},
  body: JSON.stringify({
    topicName: "Builder Pattern",
    topicContent: "The Builder pattern is a creational design pattern that separates the construction of a complex object from its representation.",
    quiz_config: [
      {
        difficulty: "EASY",
        quiz_type_config: [{type: "NORMAL_MULTIPLE", number: 2}]
      }
    ]
  })
})
.then(response => response.json())
.then(quiz => console.log(quiz));
```

#### Response

**Success:** `200 OK`

Returns a quiz with generated questions:

```json
{
  "topic_title": "Builder Pattern",
  "questions": [
    {
      "question_text": "What is the primary purpose of the Builder pattern?",
      "question_type": "NORMAL_MULTIPLE",
      "explanation": "The Builder pattern separates the construction of an object from its representation, allowing the same construction process to create different representations.",
      "difficulty_level": "EASY",
      "choices": [
        {"choice_text": "To improve performance", "is_correct": false},
        {"choice_text": "To separate construction from representation", "is_correct": true},
        {"choice_text": "To reduce memory usage", "is_correct": false},
        {"choice_text": "To simplify inheritance", "is_correct": false}
      ]
    },
    {
      "question_text": "The Builder pattern is a creational design pattern.",
      "question_type": "STATEMENT_VERIFICATION",
      "explanation": "True. The Builder pattern is classified as a creational design pattern that deals with object creation mechanisms.",
      "difficulty_level": "EASY",
      "choices": [
        {"choice_text": "True", "is_correct": true},
        {"choice_text": "False", "is_correct": false}
      ]
    },
    {
      "question_text": "How many key components does the Builder pattern typically have?",
      "question_type": "STATEMENT_COUNTING",
      "explanation": "The Builder pattern typically has 4 key components: Builder, Concrete Builder, Director, and Product.",
      "difficulty_level": "EASY",
      "choices": [
        {"choice_text": "2", "is_correct": false},
        {"choice_text": "3", "is_correct": false},
        {"choice_text": "4", "is_correct": true},
        {"choice_text": "5", "is_correct": false}
      ]
    }
  ]
}
```

#### Response Schema

| Field            | Type    | Description                                      |
|------------------|---------|--------------------------------------------------|
| topic_title      | string  | Title of the quiz topic                          |
| questions        | array   | List of generated questions                      |

#### Question Schema

| Field            | Type    | Description                                      |
|------------------|---------|--------------------------------------------------|
| question_text    | string  | The question text                                |
| question_type    | string  | Type: NORMAL_MULTIPLE, STATEMENT_VERIFICATION, or STATEMENT_COUNTING |
| explanation      | string  | Explanation of the answer                        |
| difficulty_level | string  | Difficulty: EASY, MEDIUM, or HARD                |
| choices          | array   | Array of choice objects                          |

#### Choice Schema

| Field        | Type    | Description                      |
|--------------|---------|----------------------------------|
| choice_text  | string  | The choice text                 |
| is_correct   | boolean | Whether this choice is correct  |

#### Error Responses

| Status Code | Description                            | Response Body Example                                     |
|-------------|----------------------------------------|----------------------------------------------------------|
| 400         | Bad Request - Invalid quiz config      | See Error Handling section below                         |
| 500         | Internal Server Error - Quiz generation failed | See Error Handling section below                    |
| 503         | Service Unavailable - AI service down  | See Error Handling section below                         |

#### Validation Rules

- `topicName` must be non-empty
- `topicContent` must be non-empty
- `quiz_config` must be a non-empty array
- `difficulty` must be one of: EASY, MEDIUM, HARD
- `type` must be one of: NORMAL_MULTIPLE, STATEMENT_VERIFICATION, STATEMENT_COUNTING
- `number` must be a positive integer

---

### 3. Process PDF

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
| 400         | Bad Request (no filename or empty file) | See Error Handling section below           |
| 413         | Request Entity Too Large (>10MB)       | See Error Handling section below           |
| 415         | Unsupported Media Type (not PDF)      | See Error Handling section below           |
| 500         | Internal Server Error                 | See Error Handling section below           |
| 503         | Service Unavailable                    | See Error Handling section below           |

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

### GenerateQuizRequest

Request to generate quiz questions for a topic.

```yaml
type: object
properties:
  topicName:
    type: string
    minLength: 1
    description: Name of the topic to generate quiz for
  topicContent:
    type: string
    minLength: 1
    description: Content/text about the topic
  quiz_config:
    type: array
    items:
      $ref: '#/QuizConfig'
    description: Quiz configuration array
required:
  - topicName
  - topicContent
  - quiz_config
```

### QuizConfig

Configuration for quiz generation at a specific difficulty level.

```yaml
type: object
properties:
  difficulty:
    type: string
    enum: [EASY, MEDIUM, HARD]
    description: Difficulty level for questions
  quiz_type_config:
    type: array
    items:
      $ref: '#/QuestionTypeRequest'
    description: Question type specifications
required:
  - difficulty
  - quiz_type_config
```

### QuestionTypeRequest

Specification for generating questions of a specific type.

```yaml
type: object
properties:
  type:
    type: string
    enum: [NORMAL_MULTIPLE, STATEMENT_VERIFICATION, STATEMENT_COUNTING]
    description: Type of question to generate
  number:
    type: integer
    minimum: 1
    description: Number of questions to generate
required:
  - type
  - number
```

### QuizResponse

Response containing generated quiz questions.

```yaml
type: object
properties:
  topic_title:
    type: string
    description: Title of the quiz topic
  questions:
    type: array
    items:
      $ref: '#/QuestionResponse'
    description: Generated questions
required:
  - topic_title
  - questions
```

### QuestionResponse

A single quiz question.

```yaml
type: object
properties:
  question_text:
    type: string
    minLength: 1
    description: The question text
  question_type:
    type: string
    enum: [NORMAL_MULTIPLE, STATEMENT_VERIFICATION, STATEMENT_COUNTING]
    description: Type of question
  explanation:
    type: string
    minLength: 1
    description: Explanation of the answer
  difficulty_level:
    type: string
    enum: [EASY, MEDIUM, HARD]
    description: Difficulty level of the question
  choices:
    type: array
    items:
      $ref: '#/ChoiceResponse'
    description: Available choices for the question
required:
  - question_text
  - question_type
  - explanation
  - difficulty_level
  - choices
```

### ChoiceResponse

A single choice for a question.

```yaml
type: object
properties:
  choice_text:
    type: string
    minLength: 1
    description: The choice text
  is_correct:
    type: boolean
    description: Whether this choice is correct
required:
  - choice_text
  - is_correct
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

All errors follow a consistent structured format:

```json
{
  "timestamp": "2026-01-25T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No filename provided",
  "path": "/api/v1/process-pdf",
  "details": null
}
```

**Error Response Fields:**

| Field     | Type    | Description                                    |
|-----------|---------|------------------------------------------------|
| timestamp | string  | ISO 8601 timestamp when the error occurred     |
| status    | integer | HTTP status code                               |
| error     | string  | HTTP status phrase or error type               |
| message   | string  | Human-readable error message                   |
| path      | string  | Request path that caused the error             |
| details   | array   | Optional additional error details (or null)    |

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
- `schemas/requests/generate-quiz.json` - Quiz generation request schema
- `schemas/requests/quiz-config.json` - Quiz configuration schema
- `schemas/requests/question-type-request.json` - Question type specification schema

### Response Schemas
- `schemas/responses/health.json` - Health check response schema
- `schemas/responses/process-pdf.json` - PDF processing response schema
- `schemas/responses/generate-quiz.json` - Quiz generation response schema
- `schemas/responses/question.json` - Question response schema
- `schemas/responses/choice.json` - Choice response schema
- `schemas/responses/error.json` - Error response schema

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
| 0.2.0   | 2026-02-05 | Added quiz generation endpoint, updated model to GLM-4.5, structured error responses |
| 0.1.0   | 2026-01-24 | Initial release          |

---

## Support

For issues or questions:
- Check the service logs
- Review the OpenAPI specifications in `doc/openapi.json`
- Test endpoints using Swagger UI at http://localhost:8000/docs
