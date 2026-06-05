package edu.skku.cs.final_project

data class CourseStop(
    val icon: String,
    val name: String,
    val detail: String,
    val place: String,
    val category: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val kakaoId: String? = null
)

data class DateCourse(
    val title: String,
    val area: String,
    val description: String,
    val time: String,
    val budget: String,
    val stops: List<CourseStop>
)

object DateCourseRepository {
    val moods = listOf("조용한", "맛집 중심", "사진 찍기", "야경", "가성비")

    var course = DateCourse(
        title = "성수에서 천천히 걷는 데이트",
        area = "성수 · 서울숲",
        description = "카페에서 시작해서 서울숲을 걷고, 부담 없는 저녁으로 마무리하는 코스입니다.",
        time = "3시간 30분",
        budget = "1인 3~5만원",
        stops = listOf(
            CourseStop("Cafe", "카페", "대화하기 좋은 조용한 공간", "오후 2:00 · 성수 카페거리"),
            CourseStop("Photo", "산책", "서울숲 근처 포토 스팟", "오후 3:30 · 서울숲"),
            CourseStop("Dinner", "저녁", "예약 가능한 캐주얼 다이닝", "오후 5:30 · 뚝섬역 근처")
        )
    )

    val searches = listOf("홍대 맛집 코스", "한강 야경 코스", "익선동 골목 코스")

    val promptExamples = listOf(
        "성수동에서 조용하고 걷기 좋은 3시간 데이트",
        "홍대에서 가성비 맛집 위주로 저녁 코스",
        "잠실에서 야경 보고 디저트 먹는 코스",
        "비 오는 날 실내에서 할 수 있는 데이트"
    )
}
