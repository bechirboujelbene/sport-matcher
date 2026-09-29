# GenAI Matching Service

This FastAPI service ranks candidate sports partners and returns a score, shared preferences, and an explanation. The default engine is deterministic and runs locally without a model download, hosted LLM, API key, or recurring cost. An optional Open WebUI adapter remains available when explicitly configured.

## Default matching behavior

The deterministic engine combines sport-interest overlap, skill-level proximity, and profile-bio token overlap. It returns results in stable score/ID order and limits the response using `GENAI_TOP_K`. The weights and matching rules are implemented in `deterministic_matcher.py`; these results are rules-based, not an LLM judgment.

Set `MATCHING_BACKEND=deterministic` (the default). To select the optional Open WebUI adapter, set `MATCHING_BACKEND=openwebui`, `OPENWEBUI_URL`, and `OPENWEBUI_API_KEY`. The optional adapter is not required by the project demo, local tests, or default deployment configuration.

## API

`POST /genai/match`

```json
{
  "user": {
    "id": "user-1",
    "sportInterests": ["Hiking", "Tennis"],
    "bio": "Weekend hiker",
    "skillLevel": "Intermediate"
  },
  "candidates": [
    {
      "id": "user-2",
      "sportInterests": ["Hiking"],
      "bio": "I enjoy hiking on weekends",
      "skillLevel": "Intermediate"
    }
  ]
}
```

A successful response has this shape:

```json
{
  "matches": [
    {
      "id": "user-2",
      "score": 0.525,
      "explanation": "shared sports: Hiking; compatible skill levels",
      "common_preferences": ["Hiking"]
    }
  ]
}
```

Other endpoints: `GET /health` and `GET /metrics`.

## Local development and tests

From the repository root:

```bash
python -m pip install -r genai/requirements.txt
PYTHONPATH=. python -m pytest genai/tests
```

Run the API locally from `genai/`:

```bash
MATCHING_BACKEND=deterministic uvicorn app:app --reload
```

The service listens on port 8000 by default. Docker Compose configures the same deterministic backend without requiring external LLM credentials.
