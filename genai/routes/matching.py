"""FastAPI router exposing matchmaking endpoint for other services."""
from __future__ import annotations

import logging
from typing import List, Dict, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, ConfigDict, Field

from matching_engine import MatchingEngine

router = APIRouter(prefix="/genai", tags=["GenAI Matching"])
engine = MatchingEngine()
logger = logging.getLogger(__name__)


class Candidate(BaseModel):
    id: str = Field(..., description="Unique ID of the candidate user")
    name: Optional[str] = Field(default="", description="Name of the candidate user")
    sportInterests: List[str] = Field(default_factory=list, description="List of sports the user is interested in")
    bio: Optional[str] = Field(default="", description="Bio of the candidate user")
    skillLevel: Optional[str] = Field(default="", description="Skill level of the candidate user")

    model_config = ConfigDict(extra="allow")


class MatchRequest(BaseModel):
    user: Candidate = Field(..., description="Active user as a Candidate object")
    candidates: List[Candidate] = Field(..., description="List of candidate users to rank")


class MatchItem(BaseModel):
    id: str
    score: float
    explanation: str
    common_preferences: List[str]

class MatchResponse(BaseModel):
    matches: List[MatchItem] = Field(..., description="Best candidates with details, ordered by compatibility")


@router.post("/match", response_model=MatchResponse)
async def match(req: MatchRequest) -> MatchResponse:  # noqa: D401  (simple func)
    if not req.user:
        raise HTTPException(status_code=400, detail="user cannot be empty")
    if not req.candidates:
        raise HTTPException(status_code=400, detail="candidates list cannot be empty")

    # Convert Candidate models to dicts
    user_data = req.user.model_dump()
    candidates_data: List[Dict] = [c.model_dump() for c in req.candidates]

    try:
        ranked = engine.match(user_data, candidates_data)
        return MatchResponse(matches=ranked)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Matching failed")
        raise HTTPException(status_code=500, detail="Matching failed") from exc
@router.get("/health")
def health():
    return {"status": "ok"}