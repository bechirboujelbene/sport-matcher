from fastapi.testclient import TestClient

from genai.app import app


client = TestClient(app)


def test_match_endpoint_returns_deterministic_matches():
    payload = {
        "user": {
            "id": "u0",
            "name": "Alice",
            "sportInterests": ["Tennis", "Hiking"],
            "bio": "Weekend hiking and outdoor training",
            "skillLevel": "Intermediate",
        },
        "candidates": [
            {
                "id": "u1",
                "name": "Bob",
                "sportInterests": ["Tennis", "Hiking"],
                "bio": "Outdoor hiking on weekends",
                "skillLevel": "Intermediate",
            },
            {
                "id": "u2",
                "name": "Carol",
                "sportInterests": ["Chess", "Reading"],
                "bio": "Indoor games",
                "skillLevel": "Beginner",
            },
        ],
    }

    response = client.post("/genai/match", json=payload)

    assert response.status_code == 200
    matches = response.json()["matches"]
    assert [match["id"] for match in matches] == ["u1", "u2"]
    assert matches[0]["score"] > matches[1]["score"]
    assert matches[0]["common_preferences"] == ["Hiking", "Tennis"]
    assert "shared sports" in matches[0]["explanation"]


def test_match_endpoint_rejects_empty_candidates():
    response = client.post(
        "/genai/match",
        json={
            "user": {"id": "u0", "sportInterests": []},
            "candidates": [],
        },
    )

    assert response.status_code == 400
