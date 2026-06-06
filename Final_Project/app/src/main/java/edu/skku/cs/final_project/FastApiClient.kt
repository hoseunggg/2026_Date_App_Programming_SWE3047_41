package edu.skku.cs.final_project

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CourseRequest(
    val prompt: String,
    val mood: String,
    val budget: String,
    val transport: String
)

data class NearbyPlace(
    val title: String,
    val category: String,
    val address: String,
    val city: String,
    val rating: String,
    val reviewCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val kakaoId: String?,
    val distanceMeters: Int
)

object FastApiClient {
    fun recommendCourse(request: CourseRequest): DateCourse {
        val body = JSONObject()
            .put("prompt", request.prompt)
            .put("mood", request.mood)
            .put("budget", request.budget)
            .put("transport", request.transport)

        val json = post("/courses/recommend", body)
        return parseCourse(JSONObject(json))
    }

    fun nearbyPlaces(latitude: Double, longitude: Double, limit: Int = 20): List<NearbyPlace> {
        val json = get("/places/nearby?latitude=$latitude&longitude=$longitude&limit=$limit")
        return parsePlaces(JSONArray(json))
    }

    fun searchPlaces(query: String, limit: Int = 10): List<NearbyPlace> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = get("/places/search?query=$encoded&limit=$limit")
        return parsePlaces(JSONArray(json))
    }

    private fun parsePlaces(array: JSONArray): List<NearbyPlace> =
        List(array.length()) { index ->
            val item = array.optJSONObject(index) ?: JSONObject()
            NearbyPlace(
                title = item.optString("title", "장소"),
                category = item.optString("category", "장소"),
                address = item.optString("address", "주소 정보 없음"),
                city = item.optString("city", ""),
                rating = item.optString("rating", "0"),
                reviewCount = item.optInt("reviewCount", 0),
                latitude = item.optNullableDouble("latitude"),
                longitude = item.optNullableDouble("longitude"),
                imageUrl = item.optString("imageUrl").ifBlank { null },
                kakaoId = item.optString("kakaoId").ifBlank { null },
                distanceMeters = item.optInt("distanceMeters", 0)
            )
        }

    private fun get(path: String): String {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val response = stream.use { input ->
            BufferedReader(InputStreamReader(input)).use { it.readText() }
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode}: $response")
        }

        return response
    }

    private fun post(path: String, body: JSONObject): String {
        val connection = (URL("${ApiConfig.BASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { output ->
            output.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val response = stream.use { input ->
            BufferedReader(InputStreamReader(input)).use { it.readText() }
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("HTTP ${connection.responseCode}: $response")
        }

        return response
    }

    private fun parseCourse(json: JSONObject): DateCourse =
        DateCourse(
            title = json.optString("title", "추천 데이트 코스"),
            area = json.optString("area", "서울"),
            description = json.optString("description", "조건에 맞춰 추천한 데이트 코스입니다."),
            time = json.optString("time", "3시간"),
            budget = json.optString("budget", "1인 3~5만원"),
            stops = parseStops(json.optJSONArray("stops") ?: JSONArray())
        )

    private fun parseStops(array: JSONArray): List<CourseStop> =
        List(array.length()) { index ->
            val item = array.optJSONObject(index) ?: JSONObject()
            CourseStop(
                icon = item.optString("icon", "Place"),
                name = item.optString("name", "장소 ${index + 1}"),
                detail = item.optString("detail", "추천 장소"),
                place = item.optString("place", "위치 정보 없음"),
                category = item.optString("category").ifBlank { null },
                address = item.optString("address").ifBlank { null },
                latitude = item.optNullableDouble("latitude"),
                longitude = item.optNullableDouble("longitude"),
                imageUrl = item.optString("imageUrl").ifBlank { null },
                kakaoId = item.optString("kakaoId").ifBlank { null }
            )
        }.ifEmpty {
            listOf(
                CourseStop("Cafe", "카페", "대화하기 좋은 공간", "첫 번째 장소"),
                CourseStop("Walk", "산책", "가볍게 걷기 좋은 코스", "두 번째 장소"),
                CourseStop("Dinner", "저녁", "마무리하기 좋은 식당", "세 번째 장소")
            )
        }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (isNull(name)) null else optDouble(name)
}
