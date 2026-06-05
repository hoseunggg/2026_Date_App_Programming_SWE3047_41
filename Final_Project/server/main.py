import json
import math
import os
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel


app = FastAPI(title="Final Project Places API")

DEFAULT_SEOUL = (37.5665, 126.9780)

PLACES = [
    {
        "title": "성수 카페거리",
        "category": "카페",
        "address": "서울 성동구 성수동2가",
        "city": "서울 성동구",
        "rating": "4.5",
        "reviewCount": 128,
        "latitude": 37.5446,
        "longitude": 127.0557,
        "imageUrl": "https://images.unsplash.com/photo-1554118811-1e0d58224f24?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-seongsu-cafe",
    },
    {
        "title": "서울숲",
        "category": "공원",
        "address": "서울 성동구 뚝섬로 273",
        "city": "서울 성동구",
        "rating": "4.7",
        "reviewCount": 302,
        "latitude": 37.5444,
        "longitude": 127.0374,
        "imageUrl": "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-seoul-forest",
    },
    {
        "title": "익선동 한옥거리",
        "category": "거리",
        "address": "서울 종로구 익선동",
        "city": "서울 종로구",
        "rating": "4.4",
        "reviewCount": 211,
        "latitude": 37.5742,
        "longitude": 126.9899,
        "imageUrl": "https://images.unsplash.com/photo-1534274867514-d5b47ef89ed7?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-ikseondong",
    },
    {
        "title": "광장시장",
        "category": "맛집",
        "address": "서울 종로구 창경궁로 88",
        "city": "서울 종로구",
        "rating": "4.3",
        "reviewCount": 430,
        "latitude": 37.5700,
        "longitude": 126.9996,
        "imageUrl": "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-gwangjang",
    },
    {
        "title": "경복궁",
        "category": "전시",
        "address": "서울 종로구 사직로 161",
        "city": "서울 종로구",
        "rating": "4.6",
        "reviewCount": 520,
        "latitude": 37.5796,
        "longitude": 126.9770,
        "imageUrl": "https://images.unsplash.com/photo-1578645510447-e20b4311e3ce?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-gyeongbokgung",
    },
    {
        "title": "여의도 한강공원",
        "category": "야경",
        "address": "서울 영등포구 여의동로 330",
        "city": "서울 영등포구",
        "rating": "4.6",
        "reviewCount": 360,
        "latitude": 37.5284,
        "longitude": 126.9330,
        "imageUrl": "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-hangang",
    },
    {
        "title": "가로수길",
        "category": "거리",
        "address": "서울 강남구 신사동",
        "city": "서울 강남구",
        "rating": "4.2",
        "reviewCount": 184,
        "latitude": 37.5206,
        "longitude": 127.0226,
        "imageUrl": "https://images.unsplash.com/photo-1518005020951-eccb494ad742?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-garosugil",
    },
    {
        "title": "K현대미술관",
        "category": "전시",
        "address": "서울 강남구 선릉로 807",
        "city": "서울 강남구",
        "rating": "4.1",
        "reviewCount": 93,
        "latitude": 37.5243,
        "longitude": 127.0398,
        "imageUrl": "https://images.unsplash.com/photo-1564399580075-5dfe19c205f3?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-kmuseum",
    },
    {
        "title": "홍대 걷고싶은거리",
        "category": "거리",
        "address": "서울 마포구 서교동",
        "city": "서울 마포구",
        "rating": "4.2",
        "reviewCount": 260,
        "latitude": 37.5552,
        "longitude": 126.9237,
        "imageUrl": "https://images.unsplash.com/photo-1519677100203-a0e668c92439?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-hongdae",
    },
    {
        "title": "롯데월드몰",
        "category": "실내",
        "address": "서울 송파구 올림픽로 300",
        "city": "서울 송파구",
        "rating": "4.4",
        "reviewCount": 410,
        "latitude": 37.5137,
        "longitude": 127.1043,
        "imageUrl": "https://images.unsplash.com/photo-1519567241046-7f570eee3ce6?auto=format&fit=crop&w=900&q=80",
        "kakaoId": "sample-lotteworldmall",
    },
]


class CourseRequest(BaseModel):
    prompt: str
    mood: str
    budget: str
    transport: str


def kakao_rest_api_key() -> str | None:
    env_key = os.getenv("KAKAO_REST_API_KEY")
    if env_key:
        return env_key

    local_properties = Path(__file__).resolve().parent.parent / "local.properties"
    if not local_properties.exists():
        return None

    for line in local_properties.read_text(encoding="utf-8").splitlines():
        if line.startswith("kakao.rest.api.key="):
            return line.split("=", 1)[1].strip()
    return None


def kakao_keyword_to_coord(query: str) -> tuple[float, float]:
    api_key = kakao_rest_api_key()
    if not api_key:
        raise HTTPException(status_code=500, detail="KAKAO_REST_API_KEY is not configured")

    encoded = urllib.parse.urlencode({"query": query, "size": 1})
    request = urllib.request.Request(
        f"https://dapi.kakao.com/v2/local/search/keyword.json?{encoded}",
        headers={"Authorization": f"KakaoAK {api_key}"},
    )

    try:
        with urllib.request.urlopen(request, timeout=7) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"Kakao Local API request failed: {exc}") from exc

    documents = payload.get("documents", [])
    if not documents:
        raise HTTPException(status_code=404, detail=f"No Kakao coordinate found for '{query}'")

    first = documents[0]
    return float(first["y"]), float(first["x"])


def haversine_meters(origin_lat: float, origin_lng: float, dest_lat: float, dest_lng: float) -> int:
    radius = 6371000
    phi1 = math.radians(origin_lat)
    phi2 = math.radians(dest_lat)
    delta_phi = math.radians(dest_lat - origin_lat)
    delta_lambda = math.radians(dest_lng - origin_lng)

    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    return int(radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a)))


def places_by_distance(latitude: float, longitude: float, limit: int) -> list[dict[str, Any]]:
    results = []
    for place in PLACES:
        result = dict(place)
        result["distanceMeters"] = haversine_meters(
            latitude,
            longitude,
            place["latitude"],
            place["longitude"],
        )
        results.append(result)

    return sorted(results, key=lambda item: item["distanceMeters"])[:limit]


@app.get("/places/search")
def search_places(query: str = Query(..., min_length=1), limit: int = Query(20, ge=1, le=50)):
    latitude, longitude = kakao_keyword_to_coord(query)
    return places_by_distance(latitude, longitude, limit)


@app.get("/places/nearby")
def nearby_places(
    latitude: float = Query(...),
    longitude: float = Query(...),
    limit: int = Query(20, ge=1, le=50),
):
    return places_by_distance(latitude, longitude, limit)


@app.post("/courses/recommend")
def recommend_course(request: CourseRequest):
    places = places_by_distance(*DEFAULT_SEOUL, limit=3)
    return {
        "title": f"{request.mood} 데이트 추천 코스",
        "area": "서울",
        "description": f"'{request.prompt}' 조건을 바탕으로 {request.transport} 이동에 맞춰 구성한 코스입니다.",
        "time": "3시간",
        "budget": request.budget,
        "stops": [
            {
                "icon": "Place",
                "name": place["title"],
                "detail": place["category"],
                "place": place["address"],
                "category": place["category"],
                "address": place["address"],
                "latitude": place["latitude"],
                "longitude": place["longitude"],
                "imageUrl": place["imageUrl"],
                "kakaoId": place["kakaoId"],
            }
            for place in places
        ],
    }
