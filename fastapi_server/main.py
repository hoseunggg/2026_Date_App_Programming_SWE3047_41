import json
import math
import os
import urllib.error
import urllib.request
from functools import lru_cache
from pathlib import Path

from pydantic import BaseModel
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles


app = FastAPI(title="Final Project API")
BASE_DIR = Path(__file__).resolve().parent
PLACES_PATH = BASE_DIR / "places.json"
PHOTOS_DIR = BASE_DIR / "photos"
ENV_PATH = BASE_DIR / ".env"
GEMINI_MODEL = "gemini-1.5-flash"

if ENV_PATH.exists():
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if not line or line.strip().startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/photos", StaticFiles(directory=PHOTOS_DIR), name="photos")


class CourseRequest(BaseModel):
    prompt: str
    mood: str = "조용한"
    budget: str = "3~5만원"
    transport: str = "도보"


class CourseStop(BaseModel):
    icon: str
    name: str
    detail: str
    place: str
    category: str | None = None
    address: str | None = None
    latitude: float | None = None
    longitude: float | None = None
    imageUrl: str | None = None
    kakaoId: str | None = None


class DateCourse(BaseModel):
    title: str
    area: str
    description: str
    time: str
    budget: str
    stops: list[CourseStop]


class SearchIntent(BaseModel):
    area: str = "서울"
    mood: str = "조용한"
    budget: str = "3~5만원"
    transport: str = "도보"
    keywords: list[str] = []
    preferredCategories: list[str] = []
    avoidCategories: list[str] = []


@app.get("/health")
def health():
    return {
        "ok": True,
        "service": "final-project-fastapi",
        "places": len(load_places()),
        "photoReadyPlaces": len(photo_ready_places()),
    }


@app.post("/courses/recommend", response_model=DateCourse)
def recommend_course(request: CourseRequest, http_request: Request):
    return build_course(request, str(http_request.base_url).rstrip("/"), use_ai=True)


def build_course(request: CourseRequest, base_url: str, use_ai: bool = True) -> DateCourse:
    intent = infer_search_intent(request) if use_ai and gemini_api_key() else fallback_intent(request)
    places = choose_places_by_intent(intent)
    stops = [
        place_to_stop(place, index, base_url)
        for index, place in enumerate(places)
    ]

    fallback = DateCourse(
        title=f"{intent.area} {intent.mood} 데이트 코스",
        area=intent.area,
        description=(
            f"{intent.transport} 이동을 기준으로 {len(stops)}곳을 연결했습니다. "
            f"{', '.join(intent.keywords[:3]) or intent.mood} 조건과 실제 장소 데이터를 함께 반영한 추천입니다."
        ),
        time="3시간 30분",
        budget=f"1인 {intent.budget}",
        stops=stops,
    )

    return fallback


def gemini_api_key() -> str:
    value = os.environ.get("GEMINI_API_KEY", "").strip()
    return "" if value == "YOUR_GEMINI_API_KEY" else value


def infer_search_intent(request: CourseRequest) -> SearchIntent:
    payload = {
        "contents": [
            {
                "parts": [
                    {
                        "text": gemini_intent_prompt(request)
                    }
                ]
            }
        ],
        "generationConfig": {
            "temperature": 0.7,
            "responseMimeType": "application/json",
        },
    }
    url = (
        f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:"
        f"generateContent?key={gemini_api_key()}"
    )
    request_body = json.dumps(payload).encode("utf-8")
    api_request = urllib.request.Request(
        url,
        data=request_body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(api_request, timeout=12) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.URLError as error:
        raise RuntimeError(f"Gemini request failed: {error}") from error

    text = (
        data.get("candidates", [{}])[0]
        .get("content", {})
        .get("parts", [{}])[0]
        .get("text", "")
    )
    data = json.loads(text)
    fallback = fallback_intent(request)
    return SearchIntent(
        area=str(data.get("area") or fallback.area),
        mood=str(data.get("mood") or fallback.mood),
        budget=str(data.get("budget") or fallback.budget),
        transport=str(data.get("transport") or fallback.transport),
        keywords=clean_string_list(data.get("keywords")) or fallback.keywords,
        preferredCategories=clean_string_list(data.get("preferredCategories")),
        avoidCategories=clean_string_list(data.get("avoidCategories")),
    )


def fallback_intent(request: CourseRequest) -> SearchIntent:
    area = pick_area(request.prompt)
    keywords = [area, request.mood] + [
        keyword for keyword in AREA_KEYWORDS + MOOD_CATEGORY_KEYWORDS.get(request.mood, [])
        if keyword in request.prompt
    ]
    return SearchIntent(
        area=area,
        mood=request.mood,
        budget=request.budget,
        transport=request.transport,
        keywords=list(dict.fromkeys([item for item in keywords if item])),
        preferredCategories=MOOD_CATEGORY_KEYWORDS.get(request.mood, []),
        avoidCategories=[],
    )


def clean_string_list(value) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item).strip() for item in value if str(item).strip()][:8]


def gemini_intent_prompt(request: CourseRequest) -> str:
    return f"""
너는 한국 데이트 코스 추천 앱의 검색 의도 분석기다.
사용자의 자연어 요청에서 빠진 조건을 합리적으로 보강해 실제 장소 데이터 검색 조건으로 변환한다.
실제 장소 이름을 새로 만들지 말고, 지역/분위기/키워드/카테고리만 추론한다.
응답은 JSON만 반환한다.

사용자 요청: {request.prompt}
사용자가 선택한 분위기: {request.mood}
사용자가 선택한 예산: {request.budget}
사용자가 선택한 이동수단: {request.transport}
지원 지역 힌트: {", ".join(AREA_KEYWORDS)}
분위기별 카테고리 힌트: {json.dumps(MOOD_CATEGORY_KEYWORDS, ensure_ascii=False)}

반환 형식:
{{
  "area": "지역명",
  "mood": "조용한|맛집 중심|사진 찍기|야경|가성비 중 하나 또는 가장 가까운 표현",
  "budget": "예산 표현",
  "transport": "도보|지하철|드라이브 중 하나",
  "keywords": ["검색 키워드"],
  "preferredCategories": ["선호 카테고리 키워드"],
  "avoidCategories": ["피해야 할 카테고리 키워드"]
}}
""".strip()


def choose_places_by_intent(intent: SearchIntent) -> list[dict]:
    places = photo_ready_places()
    area_matches = [place for place in places if matches_area(place, intent.area)]
    candidates = area_matches or places
    ranked = sorted(
        candidates,
        key=lambda place: score_place_by_intent(place, intent),
        reverse=True,
    )
    return diversify(ranked, count=3)


def score_place_by_intent(place: dict, intent: SearchIntent) -> float:
    category = place.get("category", "")
    title = place.get("title", "")
    score = 0.0

    if matches_area(place, intent.area):
        score += 45

    for keyword in intent.keywords:
        if contains(place, keyword):
            score += 24

    for keyword in intent.preferredCategories:
        if keyword in category or keyword in title:
            score += 34

    for keyword in intent.avoidCategories:
        if keyword in category or keyword in title:
            score -= 80

    for keyword in MOOD_CATEGORY_KEYWORDS.get(intent.mood, []):
        if keyword in category or keyword in title:
            score += 18

    score += safe_float(place.get("rating")) * 10
    score += min(int(place.get("review_count") or 0), 1000) / 50
    score += min(int(place.get("blog_count") or 0), 3000) / 150
    score += float(place.get("rankScore") or 0) * 3
    return score


@app.get("/places")
def places(q: str | None = None, city: str | None = None, limit: int = 30, request: Request = None):
    items = photo_ready_places()
    if q:
        items = [place for place in items if contains(place, q)]
    if city:
        items = [place for place in items if city in place.get("city", "")]

    base_url = str(request.base_url).rstrip("/") if request else ""
    return [public_place(place, base_url) for place in items[:limit]]


@app.get("/places/nearby")
def nearby_places(latitude: float, longitude: float, limit: int = 20, request: Request = None):
    base_url = str(request.base_url).rstrip("/") if request else ""
    ranked = []
    for place in photo_ready_places():
        lat = coordinate(place.get("y"))
        lng = coordinate(place.get("x"))
        if lat is None or lng is None:
            continue
        item = public_place(place, base_url)
        item["distanceMeters"] = round(distance_meters(latitude, longitude, lat, lng))
        ranked.append(item)

    ranked.sort(key=lambda place: place["distanceMeters"])
    return ranked[:limit]


def pick_area(prompt: str) -> str:
    for area in AREA_KEYWORDS:
        if area in prompt:
            return area
    return "서울"


@lru_cache(maxsize=1)
def load_places() -> list[dict]:
    with PLACES_PATH.open(encoding="utf-8") as file:
        return json.load(file)


@lru_cache(maxsize=1)
def photo_index() -> dict[str, list[str]]:
    index: dict[str, list[str]] = {}
    for file in PHOTOS_DIR.iterdir():
        if not file.is_file() or "_" not in file.stem:
            continue
        serial = file.stem.rsplit("_", 1)[0]
        index.setdefault(serial, []).append(file.name)

    for files in index.values():
        files.sort()
    return index


@lru_cache(maxsize=1)
def photo_ready_places() -> list[dict]:
    index = photo_index()
    ready = []
    for place in load_places():
        serial = place_serial(place)
        photos = index.get(serial, [])
        if photos:
            item = dict(place)
            item["serial"] = serial
            item["imageFiles"] = photos
            ready.append(item)
    return ready


def choose_places(area: str, mood: str) -> list[dict]:
    places = photo_ready_places()
    area_matches = [place for place in places if matches_area(place, area)]
    candidates = area_matches or places

    ranked = sorted(
        candidates,
        key=lambda place: score_place(place, mood, area),
        reverse=True,
    )

    return diversify(ranked, count=3)


def diversify(places: list[dict], count: int) -> list[dict]:
    selected: list[dict] = []
    used_groups: set[str] = set()

    for place in places:
        group = category_group(place.get("category", ""))
        if group in used_groups and len(places) >= count * 2:
            continue
        selected.append(place)
        used_groups.add(group)
        if len(selected) == count:
            return selected

    for place in places:
        if place not in selected:
            selected.append(place)
        if len(selected) == count:
            break

    return selected


def score_place(place: dict, mood: str, area: str) -> float:
    category = place.get("category", "")
    score = 0.0

    for keyword in MOOD_CATEGORY_KEYWORDS.get(mood, []):
        if keyword in category or keyword in place.get("title", ""):
            score += 50

    if matches_area(place, area):
        score += 30

    score += safe_float(place.get("rating")) * 10
    score += min(int(place.get("review_count") or 0), 1000) / 50
    score += min(int(place.get("blog_count") or 0), 3000) / 150
    score += float(place.get("rankScore") or 0) * 3
    return score


def place_to_stop(place: dict, index: int, base_url: str) -> CourseStop:
    category = place.get("category", "")
    rating = place.get("rating", "0")
    review_count = place.get("review_count", 0)
    image = place.get("imageFiles", [None])[0]

    return CourseStop(
        icon=category_group(category),
        name=place.get("title", "장소"),
        detail=f"{category} · 평점 {rating} · 리뷰 {review_count}개",
        place=f"{time_label(index)} · {place.get('roadAddress') or place.get('address')}",
        category=category,
        address=place.get("roadAddress") or place.get("address"),
        latitude=coordinate(place.get("y")),
        longitude=coordinate(place.get("x")),
        imageUrl=f"{base_url}/photos/{image}" if image else None,
        kakaoId=str(place.get("kakao_id")) if place.get("kakao_id") else None,
    )


def public_place(place: dict, base_url: str) -> dict:
    image = place.get("imageFiles", [None])[0]
    return {
        "title": place.get("title"),
        "category": place.get("category"),
        "address": place.get("roadAddress") or place.get("address"),
        "city": place.get("city"),
        "rating": place.get("rating"),
        "reviewCount": place.get("review_count"),
        "blogCount": place.get("blog_count"),
        "latitude": coordinate(place.get("y")),
        "longitude": coordinate(place.get("x")),
        "imageUrl": f"{base_url}/photos/{image}" if image else None,
        "kakaoId": place.get("kakao_id"),
    }


def matches_area(place: dict, area: str) -> bool:
    if area == "서울":
        return place.get("region") == "서울특별시"
    return contains(place, area)


def contains(place: dict, keyword: str) -> bool:
    haystack = " ".join(
        str(place.get(key) or "")
        for key in ("title", "category", "address", "roadAddress", "region", "city")
    )
    return keyword in haystack


def place_serial(place: dict) -> str:
    return f"{place.get('y', '')}{place.get('x', '')}"


def coordinate(value: str | None) -> float | None:
    if not value:
        return None
    return float(value) / 10_000_000


def safe_float(value: str | None) -> float:
    try:
        return float(value or 0)
    except ValueError:
        return 0.0


def distance_meters(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    radius = 6_371_000
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lng2 - lng1)
    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def time_label(index: int) -> str:
    return ["오후 2:00", "오후 3:30", "오후 5:30"][index] if index < 3 else "오후"


def category_group(category: str) -> str:
    if "카페" in category:
        return "Cafe"
    if "음식점" in category or "시장" in category:
        return "Food"
    if "공원" in category or "산책" in category or "숲" in category or "해변" in category:
        return "Walk"
    if "미술관" in category or "박물관" in category or "전시" in category or "문화" in category:
        return "Culture"
    if "백화점" in category or "쇼핑" in category:
        return "Shopping"
    if "테마" in category or "체험" in category:
        return "Activity"
    return "Place"


AREA_KEYWORDS = [
    "성수",
    "강남",
    "홍대",
    "잠실",
    "익선동",
    "한강",
    "서울숲",
    "종로",
    "중구",
    "용산",
    "서초",
    "송파",
    "여의도",
    "부산",
    "제주",
    "경주",
    "강릉",
    "전주",
]


MOOD_CATEGORY_KEYWORDS = {
    "조용한": ["미술관", "박물관", "공원", "숲", "산책", "갤러리", "수목원"],
    "맛집 중심": ["시장", "카페", "음식점", "디저트"],
    "사진 찍기": ["궁궐", "광장", "공원", "전시", "미술관", "거리", "해변"],
    "야경": ["타워", "전망", "한강", "공원", "광장", "도시"],
    "가성비": ["시장", "공원", "산책", "거리", "박물관"],
}
