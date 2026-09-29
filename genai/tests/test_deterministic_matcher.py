from genai.deterministic_matcher import rank_candidates


USER = {
    "id": "active",
    "sportInterests": ["Hiking", "Tennis"],
    "bio": "Weekend mountain hiking and outdoor training",
    "skillLevel": "Intermediate",
}


def test_ranking_is_deterministic_and_explainable():
    candidates = [
        {
            "id": "strong",
            "sportInterests": ["Tennis", "Hiking"],
            "bio": "Outdoor mountain hiking every weekend",
            "skillLevel": "Intermediate",
        },
        {
            "id": "weak",
            "sportInterests": ["Chess"],
            "bio": "Board games",
            "skillLevel": "Expert",
        },
    ]

    first = rank_candidates(USER, candidates)
    second = rank_candidates(USER, candidates)

    assert first == second
    assert [match["id"] for match in first] == ["strong", "weak"]
    assert first[0]["common_preferences"] == ["Hiking", "Tennis"]
    assert "shared sports" in first[0]["explanation"]
    assert all(0 <= match["score"] <= 1 for match in first)


def test_ties_are_sorted_by_id_and_top_k_is_respected():
    candidates = [
        {"id": "b", "sportInterests": [], "bio": "", "skillLevel": ""},
        {"id": "a", "sportInterests": [], "bio": "", "skillLevel": ""},
    ]

    ranked = rank_candidates(USER, candidates, top_k=1)

    assert [match["id"] for match in ranked] == ["a"]


def test_sport_matching_is_case_insensitive():
    ranked = rank_candidates(
        USER,
        [{"id": "case", "sportInterests": ["hIKING"], "bio": "", "skillLevel": ""}],
    )

    assert ranked[0]["common_preferences"] == ["hIKING"]
    assert ranked[0]["score"] > 0


def test_empty_candidate_list_returns_empty_list():
    assert rank_candidates(USER, []) == []
