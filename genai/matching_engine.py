from __future__ import annotations

from typing import List

import config


class MatchingEngine:
    def match(self, user: dict, candidates: List[dict]) -> List[dict]:
        if config.MATCHING_BACKEND == "openwebui":
            import openwebui_client

            return openwebui_client.rank_candidates(user, candidates)

        import deterministic_matcher

        return deterministic_matcher.rank_candidates(user, candidates)
