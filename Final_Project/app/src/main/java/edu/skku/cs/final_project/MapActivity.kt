package edu.skku.cs.final_project

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import java.net.URLEncoder
import kotlin.math.roundToInt

class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var statusText: TextView
    private lateinit var stops: List<MapStop>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(EXTRA_TITLE) ?: "지도"
        stops = readStops()
        setContentView(buildContent())

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            statusText.text = "카카오 네이티브 앱 키를 local.properties에 추가해주세요."
            return
        }
        startMap()
    }

    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) mapView.resume()
    }

    override fun onPause() {
        if (::mapView.isInitialized) mapView.pause()
        super.onPause()
    }

    private fun buildContent(): View =
        FrameLayout(this).apply {
            setBackgroundColor(Ui.WHITE)
            mapView = MapView(context)
            addView(mapView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            addView(Ui.centeredText(context, "‹", 30f, Ui.INK, true).apply {
                contentDescription = "지도 나가기"
                background = Ui.bg(0xf2ffffff.toInt(), Ui.dp(context, 22), Ui.SOFT, Ui.dp(context, 1))
                elevation = Ui.dp(context, 10).toFloat()
                setOnClickListener { finish() }
            }, FrameLayout.LayoutParams(Ui.dp(context, 48), Ui.dp(context, 48), Gravity.TOP or Gravity.START).apply {
                leftMargin = Ui.dp(context, 16)
                topMargin = Ui.dp(context, 18)
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(Ui.dp(context, 16), Ui.dp(context, 14), Ui.dp(context, 16), Ui.dp(context, 16))
                background = Ui.bg(0xf2ffffff.toInt(), Ui.dp(context, 24), Ui.SOFT, Ui.dp(context, 1))
                elevation = Ui.dp(context, 10).toFloat()
                addView(Ui.text(context, title.toString(), 18f, Ui.INK, true))
                addView(Ui.text(context, intent.getStringExtra(EXTRA_AREA) ?: "", 13f, Ui.MUTED).apply {
                    setPadding(0, Ui.dp(context, 4), 0, Ui.dp(context, 10))
                })
                statusText = Ui.text(context, "지도 준비 중...", 13f, Ui.MUTED)
                addView(statusText)
                addView(Ui.centeredText(context, "카카오맵에서 길찾기", 14f, Ui.WHITE, true).apply {
                    background = Ui.bg(0xfffb7185.toInt(), Ui.dp(context, 18))
                    setOnClickListener { openKakaoMapRoute() }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 46)).apply {
                    topMargin = Ui.dp(context, 12)
                })
                addView(Ui.centeredText(context, "나가기", 14f, Ui.INK, true).apply {
                    background = Ui.bg(Ui.WHITE, Ui.dp(context, 18), Ui.SOFT, Ui.dp(context, 1))
                    setOnClickListener { finish() }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(context, 46)).apply {
                    topMargin = Ui.dp(context, 8)
                })
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                setMargins(Ui.dp(context, 16), 0, Ui.dp(context, 16), Ui.dp(context, 18))
            })
        }

    private fun startMap() {
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() = Unit

            override fun onMapError(error: Exception) {
                statusText.text = "지도 인증 또는 로딩 실패: ${error.message ?: "오류"}"
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                drawStops(kakaoMap)
                drawRoute(kakaoMap)
            }

            override fun getPosition(): LatLng =
                stops.firstOrNull()?.let { LatLng.from(it.latitude, it.longitude) } ?: LatLng.from(37.5665, 126.9780)

            override fun getZoomLevel(): Int = 14
        })
    }

    private fun drawStops(kakaoMap: KakaoMap) {
        val labelManager = kakaoMap.labelManager ?: return
        val labelStyles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from().setTextStyles(28, Color.WHITE, Ui.dp(this, 3), 0xfffb7185.toInt()))
        )
        val layer = labelManager.getLayer() ?: return
        stops.forEachIndexed { index, stop ->
            layer.addLabel(
                LabelOptions.from(LatLng.from(stop.latitude, stop.longitude))
                    .setStyles(labelStyles)
                    .setTexts(LabelTextBuilder().setTexts("${index + 1}"))
            )
        }
        stops.firstOrNull()?.let {
            kakaoMap.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(it.latitude, it.longitude)),
                CameraAnimation.from(500, true, true)
            )
        }
    }

    private fun drawRoute(kakaoMap: KakaoMap) {
        if (stops.size < 2) {
            statusText.text = "표시할 경로가 부족합니다."
            return
        }
        Thread {
            runCatching { KakaoDirectionsClient.route(stops) }
                .onSuccess { route ->
                    runOnUiThread {
                        val points = route.points.map { LatLng.from(it.latitude, it.longitude) }
                        val stylesSet = RouteLineStylesSet.from(
                            "dateRoute",
                            RouteLineStyles.from(RouteLineStyle.from(Ui.dp(this, 9).toFloat(), 0xfffb7185.toInt(), Ui.dp(this, 3).toFloat(), Color.WHITE))
                        )
                        val segment = RouteLineSegment.from(points).setStyles(stylesSet.getStyles(0))
                        kakaoMap.routeLineManager!!.getLayer()!!.addRouteLine(
                            RouteLineOptions.from(segment).setStylesSet(stylesSet)
                        )
                        statusText.text = route.toStatusText()
                    }
                }
                .onFailure { error ->
                    runOnUiThread {
                        Toast.makeText(this, "길찾기 실패: ${error.message}", Toast.LENGTH_LONG).show()
                        statusText.text = "직선 경로로 표시 중입니다."
                        drawFallbackRoute(kakaoMap)
                    }
                }
        }.start()
    }

    private fun drawFallbackRoute(kakaoMap: KakaoMap) {
        val points = stops.map { LatLng.from(it.latitude, it.longitude) }
        val stylesSet = RouteLineStylesSet.from(
            "fallbackRoute",
            RouteLineStyles.from(RouteLineStyle.from(Ui.dp(this, 7).toFloat(), 0xfffb7185.toInt()))
        )
        kakaoMap.routeLineManager!!.getLayer()!!.addRouteLine(
            RouteLineOptions.from(RouteLineSegment.from(points).setStyles(stylesSet.getStyles(0))).setStylesSet(stylesSet)
        )
    }

    private fun RouteSummary.toStatusText(): String =
        if (distanceMeters > 0 || durationSeconds > 0) {
            val km = distanceMeters / 1000.0
            val minutes = (durationSeconds / 60.0).roundToInt()
            "추천 자동차 경로: %.1fkm · 약 %d분".format(km, minutes)
        } else {
            "REST API 키가 없어 장소 순서대로 직선 경로를 표시 중입니다."
        }

    private fun openKakaoMapRoute() {
        val first = stops.firstOrNull() ?: return
        val last = stops.lastOrNull() ?: return
        val url = "https://map.kakao.com/link/from/${first.name.url()},${first.latitude},${first.longitude}/to/${last.name.url()},${last.latitude},${last.longitude}"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun readStops(): List<MapStop> {
        val names = intent.getStringArrayListExtra(EXTRA_STOP_NAMES).orEmpty()
        val details = intent.getStringArrayListExtra(EXTRA_STOP_DETAILS).orEmpty()
        val lats = intent.getDoubleArrayExtra(EXTRA_STOP_LATS) ?: doubleArrayOf()
        val lngs = intent.getDoubleArrayExtra(EXTRA_STOP_LNGS) ?: doubleArrayOf()
        return names.indices.mapNotNull { index ->
            val lat = lats.getOrNull(index) ?: return@mapNotNull null
            val lng = lngs.getOrNull(index) ?: return@mapNotNull null
            MapStop(
                name = names[index],
                detail = details.getOrNull(index).orEmpty(),
                latitude = lat,
                longitude = lng
            )
        }
    }

    private fun String.url(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name())

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_AREA = "area"
        const val EXTRA_STOP_NAMES = "stop_names"
        const val EXTRA_STOP_DETAILS = "stop_details"
        const val EXTRA_STOP_LATS = "stop_lats"
        const val EXTRA_STOP_LNGS = "stop_lngs"
    }
}
