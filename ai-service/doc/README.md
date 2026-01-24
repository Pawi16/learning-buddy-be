# AI Service Documentation

This directory contains comprehensive API documentation for the Learning Buddy AI Service.

## 📁 Documentation Structure

```
doc/
├── openapi.json              # OpenAPI 3.1 specification (JSON)
├── openapi.yaml              # OpenAPI 3.1 specification (YAML)
├── api-documentation.md      # Human-readable API reference
├── generate-openapi.py       # Script to regenerate OpenAPI specs
├── README.md                 # This file
└── schemas/                  # JSON schemas for validation
    ├── requests/             # Request body schemas
    │   └── process-pdf.json
    └── responses/            # Response body schemas
        ├── health.json
        └── process-pdf.json
```

## 🚀 Quick Start

### View Interactive Documentation

Start the service and visit:
- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc

### Regenerate OpenAPI Specs

If you modify the API, regenerate the documentation:

```bash
# From the ai-service directory
python doc/generate-openapi.py
```

This updates:
- `openapi.json` - Full OpenAPI specification
- `openapi.yaml` - Human-readable YAML version

## 📖 Documentation Files

### OpenAPI Specifications

**`openapi.json`** & **`openapi.yaml`**
- Machine-readable API specifications
- Use with OpenAPI tools and generators
- Always in sync with FastAPI app

**Uses:**
- Generate client SDKs:
  ```bash
  openapi-generator-cli generate -i doc/openapi.json -g python -o ./client
  ```
- Import into Postman, Insomnia, or other API tools
- Validate API compliance

### API Reference

**`api-documentation.md`**
- Human-readable API documentation
- Includes examples in multiple languages
- Describes all endpoints, schemas, and error codes

### JSON Schemas

**`schemas/requests/`** - Request validation schemas
- `process-pdf.json` - Schema for PDF upload requests

**`schemas/responses/`** - Response validation schemas
- `health.json` - Schema for health check response
- `process-pdf.json` - Schema for processed PDF topics

**Example validation:**
```python
import json
from jsonschema import validate

# Load schema
with open('doc/schemas/responses/process-pdf.json') as f:
    schema = json.load(f)

# Validate API response
validate(instance=api_response, schema=schema)
```

## 🛠️ Development Workflow

### After Modifying the API

1. Update code in `app/api/routes.py` or `app/schemas/`
2. Regenerate OpenAPI specs:
   ```bash
   python doc/generate-openapi.py
   ```
3. Test with Swagger UI: http://localhost:8000/docs
4. Update `api-documentation.md` if needed

### Adding New Endpoints

When adding new endpoints:
1. Add the endpoint in `app/api/routes.py` with proper docstrings
2. Run `python doc/generate-openapi.py`
3. Add request/response schemas to `schemas/` if applicable
4. Update `api-documentation.md` with examples

## 📚 External Tools

### OpenAPI Validator

Validate your OpenAPI spec:
```bash
# Online
# Visit: https://validator.swagger.io/
# Upload: doc/openapi.json

# CLI (requires npm)
npm install -g @apidevtools/swagger-cli
swagger-cli validate doc/openapi.json
```

### Client SDK Generation

Generate SDKs in various languages:

```bash
# Python
openapi-generator-cli generate -i doc/openapi.json -g python -o ./python-client

# TypeScript/JavaScript
openapi-generator-cli generate -i doc/openapi.json -g typescript-axios -o ./ts-client

# Java
openapi-generator-cli generate -i doc/openapi.json -g java -o ./java-client

# Go
openapi-generator-cli generate -i doc/openapi.json -g go -o ./go-client
```

### API Testing Tools

Import `openapi.json` into:
- [Postman](https://www.postman.com/) - Import → API → OpenAPI
- [Insomnia](https://insomnia.rest/) - Import → OpenAPI
- [Bruno](https://www.usebruno.com/) - Import Collection

## 🔍 Schema Validation

### Using Python

```python
import json
from jsonschema import validate, ValidationError

def validate_response(response_data, schema_name):
    """Validate API response against JSON schema."""
    with open(f'doc/schemas/responses/{schema_name}.json') as f:
        schema = json.load(f)

    try:
        validate(instance=response_data, schema=schema)
        return True
    except ValidationError as e:
        print(f"Validation failed: {e.message}")
        return False

# Usage
validate_response(topics, 'process-pdf')
```

### Using JavaScript

```javascript
const Ajv = require('ajv');
const fs = require('fs');
const schema = JSON.parse(fs.readFileSync('doc/schemas/responses/process-pdf.json'));

const ajv = new Ajv();
const validate = ajv.compile(schema);

const valid = validate(responseData);
if (!valid) {
    console.log(validate.errors);
}
```

## 📋 Documentation Checklist

- [ ] Keep `openapi.json` in sync with API changes
- [ ] Update `api-documentation.md` with new endpoints
- [ ] Add JSON schemas for new request/response formats
- [ ] Test examples in documentation
- [ ] Validate OpenAPI spec after changes

## 🌐 Publishing Documentation

### Static HTML Docs

Generate static HTML documentation:

```bash
# Using redoc-cli
npm install -g @redocly/cli
redocly build-docs doc/openapi.json -o index.html

# Serve
python -m http.server 8000
# Visit http://localhost:8000/index.html
```

### Swagger UI Standalone

Serve OpenAPI spec with Swagger UI:

```bash
docker run -p 8080:8080 \
  -e SWAGGER_JSON=/openapi/openapi.json \
  -v $(pwd)/doc:/openapi \
  swaggerapi/swagger-ui
```

## 📞 Support

For API questions or issues:
1. Check this README
2. Review `api-documentation.md`
3. Test endpoints in Swagger UI at http://localhost:8000/docs
4. Validate schemas in `schemas/` directory

## 🔗 Related Links

- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [OpenAPI Generator](https://openapi-generator.tech)
- [JSON Schema Reference](https://json-schema.org/)
