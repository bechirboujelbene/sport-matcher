from __future__ import annotations

import re
from typing import Iterable

import config

SPORT_WEIGHT = 0.55
SKILL_WEIGHT = 0.25
BIO_WEIGHT = 0.20
SKILL_LEVELS = {
    "beginner": 0,
    "intermediate": 1,
    "advanced": 2,
    "expert": 3,
}
STOPWORDS = {
    "a", "an", "and", "are", "as", "at", "be", "for", "from", "i", "in",
    "is", "it", "my", "of", "on", "or", "the", "to", "we", "with",
}


def _normalized_set(values: Iterable[str] | None) -> set[str]:
    return {str(value).strip().casefold() for value in values or [] if str(value).strip()}


def _jaccard(left: set[str], right: set[str]) -> float:
    union = left | right
    return len(left & right) / len(union) if union else 0.0


def _skill_similarity(left: str | None, right: str | None) -> float:
    left_level = SKILL_LEVELS.get((left or "").strip().casefold())
    right_level = SKILL_LEVELS.get((right or "").strip().casefold())
    if left_level is None or right_level is None:
        return 0.5
    return 1 - abs(left_level - right_level) / (len(SKILL_LEVELS) - 1)


def _bio_tokens(value: str | None) -> set[str]:
    return {
        token
        for token in re.findall(r"[a-z0-9]+", (value or "").casefold())
        if len(token) > 1 and token not in STOPWORDS
    }


def _explanation(common: list[str], skill_score: float, bio_score: float) -> str:
    parts = []
    if common:
        parts.append(f"shared sports: {', '.join(common)}")
    if skill_score >= 0.8:
        parts.append("compatible skill levels")
    elif skill_score >= 0.5:
        parts.append("partially compatible skill levels")
    if bio_score > 0:
        parts.append("similar profile interests")
    return "; ".join(parts) if parts else "No strong shared preferences yet"


def rank_candidates(user: dict, candidates: list[dict], top_k: int | None = None) -> list[dict]:
    limit = config.TOP_K_MATCHES if top_k is None else max(top_k, 0)
    user_sports = _normalized_set(user.get("sportInterests"))
    user_bio = _bio_tokens(user.get("bio"))
    ranked = []

    for candidate in candidates:
        candidate_sports = _normalized_set(candidate.get("sportInterests"))
        common_normalized = user_sports & candidate_sports
        common = sorted(
            {
                str(value).strip()
                for value in candidate.get("sportInterests") or []
                if str(value).strip().casefold() in common_normalized
            },
            key=str.casefold,
        )
        sport_score = _jaccard(user_sports, candidate_sports)
        skill_score = _skill_similarity(user.get("skillLevel"), candidate.get("skillLevel"))
        bio_score = _jaccard(user_bio, _bio_tokens(candidate.get("bio")))
        score = SPORT_WEIGHT * sport_score + SKILL_WEIGHT * skill_score + BIO_WEIGHT * bio_score
        ranked.append({
            "id": str(candidate["id"]),
            "score": round(min(max(score, 0.0), 1.0), 4),
            "explanation": _explanation(common, skill_score, bio_score),
            "common_preferences": common,
        })

    ranked.sort(key=lambda item: (-item["score"], item["id"]))
    return ranked[:limit]
