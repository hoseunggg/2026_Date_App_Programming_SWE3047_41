package edu.skku.cs.final_project

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MapStop(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteSummary(
    val distanceMeters: Int,
    val durationSeconds: Int,
    val points: List<MapStop>
)

object KakaoDirectionsClient {
    fun route(stops: List<MapStop>): RouteSummary {
        require(stops.size >= 2) { "길찾기는 두 개 이상의 장소가 필요합니다." }
        if (BuildConfig.KAKAO_REST_API_KEY.isBlank()) {
            return RouteSummary(0, 0, stops)
        }

        val origin = stops.first().toCoordParam()
        val destination = stops.last().toCoordParam()
        val waypoints = stops.drop(1).dropLast(1).take(5).joinToString("|") { it.toCoordParam() }
        val query = buildList {
            add("origin=${origin.encode()}")
            add("destination=${destination.encode()}")
            if (waypoints.isNotBlank()) add("waypoints=${waypoints.encode()}")
            add("priority=RECOMMEND")
            add("summary=false")
            add("alternatives=false")
            add("road_details=false")
        }.joinToString("&")

        val connection = (URL("https://apis-navi.kakaomobility.com/v1/directions?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 15000
            setRequestProperty("Authorization", "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}")
            setRequestProperty("Content-Type", "application/json")
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
        return parseRoute(JSONObject(response), stops)
    }

    private fun parseRoute(json: JSONObject, fallback: List<MapStop>): RouteSummary {
        val route = json.optJSONArray("routes")?.optJSONObject(0) ?: return RouteSummary(0, 0, fallback)
        val summary = route.optJSONObject("summary") ?: JSONObject()
        val points = mutableListOf<MapStop>()
        val sections = route.optJSONArray("sections") ?: JSONArray()
        for (sectionIndex in 0 until sections.length()) {
            val roads = sections.optJSONObject(sectionIndex)?.optJSONArray("roads") ?: continue
            for (roadIndex in 0 until roads.length()) {
                val vertexes = roads.optJSONObject(roadIndex)?.optJSONArray("vertexes") ?: continue
                var i = 0
                while (i + 1 < vertexes.length()) {
                    val lng = vertexes.optDouble(i)
                    val lat = vertexes.optDouble(i + 1)
                    points.add(MapStop("", "", lat, lng))
                    i += 2
                }
            }
        }
        return RouteSummary(
            distanceMeters = summary.optInt("distance", 0),
            durationSeconds = summary.optInt("duration", 0),
            points = points.ifEmpty { fallback }
        )
    }

    private fun MapStop.toCoordParam(): String =
        "$longitude,$latitude,name=$name"

    private fun String.encode(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())
}
