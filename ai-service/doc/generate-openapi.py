#!/usr/bin/env python3
"""
OpenAPI Documentation Generator

This script generates OpenAPI 3.0 specification files from the FastAPI application.
It exports both JSON and YAML formats for different use cases.

Usage:
    python doc/generate-openapi.py
"""

import json
import sys
from pathlib import Path

# Add parent directory to path to import app modules
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    import yaml
    YAML_AVAILABLE = True
except ImportError:
    YAML_AVAILABLE = False
    print("Warning: PyYAML not installed. YAML generation will be skipped.")
    print("Install with: uv add pyyaml")

from app.main import app


def generate_openapi_json() -> dict:
    """Generate OpenAPI schema from FastAPI app."""
    return app.openapi()


def save_json(schema: dict, output_path: Path) -> None:
    """Save OpenAPI schema as JSON."""
    with open(output_path, "w") as f:
        json.dump(schema, f, indent=2)
    print(f"✓ Generated: {output_path}")


def save_yaml(schema: dict, output_path: Path) -> None:
    """Save OpenAPI schema as YAML."""
    if not YAML_AVAILABLE:
        print(f"✗ Skipped: {output_path} (PyYAML not installed)")
        return

    with open(output_path, "w") as f:
        yaml.dump(schema, f, default_flow_style=False, sort_keys=False)
    print(f"✓ Generated: {output_path}")


def validate_schema(schema: dict) -> bool:
    """Basic validation of the OpenAPI schema."""
    required_fields = ["openapi", "info", "paths"]
    for field in required_fields:
        if field not in schema:
            print(f"✗ Invalid schema: missing '{field}' field")
            return False

    print(f"✓ Schema validation passed (OpenAPI {schema['openapi']})")
    return True


def main():
    """Main generation function."""
    print("Generating OpenAPI documentation from FastAPI app...")
    print("-" * 50)

    # Generate schema
    schema = generate_openapi_json()

    # Validate
    if not validate_schema(schema):
        sys.exit(1)

    # Determine output paths
    doc_dir = Path(__file__).parent
    json_path = doc_dir / "openapi.json"
    yaml_path = doc_dir / "openapi.yaml"

    # Save files
    save_json(schema, json_path)
    save_yaml(schema, yaml_path)

    print("-" * 50)
    print("✓ OpenAPI documentation generated successfully!")
    print(f"\nFiles created:")
    print(f"  - {json_path}")
    if YAML_AVAILABLE:
        print(f"  - {yaml_path}")
    print(f"\nUsage:")
    print(f"  - View in Swagger UI: http://localhost:8000/docs")
    print(f"  - Validate: https://validator.swagger.io/")
    print(f"  - Generate clients: openapi-generator-cli generate -i openapi.json -g <language>")


if __name__ == "__main__":
    main()
