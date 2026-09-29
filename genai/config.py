"""Central configuration for deterministic and optional Open WebUI matching."""
from __future__ import annotations

import os
from dotenv import load_dotenv

# Load variables from local .env file when running outside Docker.
load_dotenv()

MATCHING_BACKEND: str = os.getenv("MATCHING_BACKEND", "deterministic").lower()
OPENWEBUI_URL: str | None = os.getenv("OPENWEBUI_URL")
OPENWEBUI_API_KEY: str | None = os.getenv("OPENWEBUI_API_KEY")

if MATCHING_BACKEND not in {"deterministic", "openwebui"}:
    raise ValueError("MATCHING_BACKEND must be 'deterministic' or 'openwebui'")

# Optional tuning parameters
REQUEST_TIMEOUT_SECONDS: int = int(os.getenv("OPENWEBUI_TIMEOUT", 30))

TOP_K_MATCHES: int = int(os.getenv("GENAI_TOP_K", 10))
