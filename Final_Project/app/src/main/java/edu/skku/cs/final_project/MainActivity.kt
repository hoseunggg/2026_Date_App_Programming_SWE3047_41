package edu.skku.cs.final_project

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
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
import java.net.URL

class MainActivity : AppCompatActivity() {
    private var currentPage = "home"
    private var selectedMood = "조용한"
    private var searchStep = 0
    private val savedCourseIds = mutableSetOf("hangang", "jongno")

    private lateinit var pageHost: FrameLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var searchScroll: ScrollView
    private lateinit var chatFlow: LinearLayout
    private lateinit var courseInput: EditText
    private lateinit var followQuestions: LinearLayout
    private lateinit var generateButton: TextView
    private var pendingNearbyRoot: LinearLayout? = null
    private var selectedBudget = "3~5만원"
    private var selectedTransport = "도보"

    private val moodHeroes = mapOf(
        "조용한" to CourseUi(
            id = "seongsu",
            title = "성수에서 천천히 걷는 데이트",
            area = "성수 · 서울숲",
            description = "카페에서 시작해서 서울숲을 걷고, 부담 없는 저녁으로 마무리하는 코스입니다.",
            time = "3시간 30분",
            budget = "1인 3~5만원",
            mood = "조용한",
            imageUrl = photoUrl("3754196441270385663_1.jpg"),
            stops = listOf(
                "성수 카페거리" to "대화하기 좋은 조용한 공간",
                "서울숲" to "걷기 좋은 산책과 포토 스팟",
                "뚝섬역 근처" to "부담 없는 캐주얼 다이닝"
            ),
            routeStops = listOf(
                MapStop("성수 카페거리", "대화하기 좋은 조용한 공간", 37.5446, 127.0557),
                MapStop("서울숲", "걷기 좋은 산책과 포토 스팟", 37.5444, 127.0374),
                MapStop("뚝섬역", "부담 없는 캐주얼 다이닝", 37.5472, 127.0474)
            )
        ),
        "맛집 중심" to CourseUi(
            id = "food",
            title = "시장 골목에서 맛보는 데이트",
            area = "종로 · 광장시장",
            description = "먹거리 많은 시장에서 시작해 가볍게 걷고, 디저트로 마무리하는 코스입니다.",
            time = "2시간 30분",
            budget = "1인 2~4만원",
            mood = "맛집 중심",
            imageUrl = photoUrl("3757016611269990691_1.jpg"),
            stops = listOf("광장시장" to "먹거리 탐방", "종로 골목" to "가벼운 산책", "카페" to "디저트와 쉬는 시간"),
            routeStops = listOf(
                MapStop("광장시장", "먹거리 탐방", 37.5700, 126.9996),
                MapStop("종로3가 골목", "가벼운 산책", 37.5705, 126.9919),
                MapStop("익선동 카페", "디저트와 쉬는 시간", 37.5742, 126.9899)
            )
        ),
        "사진 찍기" to CourseUi(
            id = "gangnam",
            title = "전시 보고 사진 남기는 데이트",
            area = "강남 · 신사",
            description = "미술관과 거리 산책을 이어가며 오늘의 장면을 예쁘게 남기는 코스입니다.",
            time = "3시간",
            budget = "1인 3~5만원",
            mood = "사진 찍기",
            imageUrl = photoUrl("3750759231270604816_1.jpg"),
            stops = listOf("K현대미술관" to "전시 관람", "가로수길" to "거리 구경", "디저트 카페" to "마무리"),
            routeStops = listOf(
                MapStop("K현대미술관", "전시 관람", 37.5243, 127.0398),
                MapStop("가로수길", "거리 구경", 37.5206, 127.0226),
                MapStop("신사동 디저트 카페", "마무리", 37.5190, 127.0234)
            )
        ),
        "야경" to CourseUi(
            id = "hangang",
            title = "한강 노을과 야경 데이트",
            area = "여의도 · 한강",
            description = "해질 무렵 강변을 걷고, 야경이 켜지는 시간에 맞춰 쉬어가는 코스입니다.",
            time = "3시간",
            budget = "1인 2~4만원",
            mood = "야경",
            imageUrl = photoUrl("3752632321269348268_1.jpeg"),
            stops = listOf("여의도 한강공원" to "노을 보기", "강변 산책로" to "야경 산책", "근처 식당" to "저녁"),
            routeStops = listOf(
                MapStop("여의도 한강공원", "노을 보기", 37.5284, 126.9330),
                MapStop("여의도 물빛광장", "야경 산책", 37.5260, 126.9348),
                MapStop("여의나루역 근처 식당", "저녁", 37.5271, 126.9329)
            )
        ),
        "가성비" to CourseUi(
            id = "jongno",
            title = "궁궐 산책 가성비 데이트",
            area = "종로 · 경복궁",
            description = "입장료 부담은 낮추고 볼거리는 챙기는 반나절 산책 코스입니다.",
            time = "반나절",
            budget = "1인 2~4만원",
            mood = "가성비",
            imageUrl = photoUrl("3757884081269770162_1.jpg"),
            stops = listOf("경복궁" to "궁궐 산책", "광화문광장" to "사진 스팟", "광장시장" to "간단한 식사"),
            routeStops = listOf(
                MapStop("경복궁", "궁궐 산책", 37.5796, 126.9770),
                MapStop("광화문광장", "사진 스팟", 37.5726, 126.9769),
                MapStop("광장시장", "간단한 식사", 37.5700, 126.9996)
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "데이트 코스 추천"
        window.statusBarColor = APP_BG
        window.navigationBarColor = APP_BG
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(buildShell())
        observeKeyboard()
        renderPage("home")
    }

    private fun buildShell(): View =
        FrameLayout(this).apply {
            setBackgroundColor(APP_BG)

            val phone = FrameLayout(context).apply {
                background = rounded(Color.WHITE, 30)
                clipToOutline = true
            }
            addView(phone, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(dp(16), dp(24), dp(16), dp(24))
            })

            pageHost = FrameLayout(context)
            phone.addView(pageHost, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            bottomBar = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(6), dp(6), dp(6), dp(6))
                background = rounded(0xf2ffffff.toInt(), 24, SOFT, 1)
                elevation = dp(10).toFloat()
            }
            phone.addView(bottomBar, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(60),
                Gravity.BOTTOM
            ).apply {
                setMargins(dp(24), 0, dp(24), dp(24))
            })
        }

    private fun observeKeyboard() {
        val root = window.decorView.findViewById<View>(android.R.id.content)
        root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val visibleHeight = android.graphics.Rect().also { root.getWindowVisibleDisplayFrame(it) }.height()
                val totalHeight = root.rootView.height
                val keyboardOpen = totalHeight - visibleHeight > totalHeight * 0.18f
                if (currentPage == "ai") {
                    bottomBar.visibility = if (keyboardOpen) View.GONE else View.VISIBLE
                    if (!keyboardOpen && ::courseInput.isInitialized) courseInput.clearFocus()
                } else {
                    bottomBar.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun renderPage(page: String) {
        currentPage = page
        pageHost.removeAllViews()
        pageHost.addView(when (page) {
            "ai" -> aiPage()
            "search" -> placeSearchPage()
            "nearby" -> nearbyPage()
            "saved" -> savedPage()
            else -> homePage()
        })
        renderBottomBar()
    }

    private fun homePage(): View {
        val (scroll, root) = baseScroll()
        root.addView(header("오늘의 데이트", "어떤 분위기가 좋아?", "♡"))
        root.addView(moodRow(), lpMatch(dp(48), top = 26))
        root.addView(heroCard(currentHero()), lpMatch(dp(352), top = 14))
        root.addView(stopList(currentHero()), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 22))
        return scroll
    }

    private fun moodRow(): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        DateCourseRepository.moods.forEach { mood ->
            row.addView(chip(mood, mood == selectedMood).apply {
                setOnClickListener {
                    selectedMood = mood
                    renderPage("home")
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply {
                marginEnd = dp(8)
            })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun heroCard(course: CourseUi): View =
        FrameLayout(this).apply {
            background = rounded(Color.WHITE, 28, SOFT, 1)
            clipToOutline = true

            addView(remoteImage(course.imageUrl), FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(View(context).apply {
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(0x0a000000, 0x16000000, 0xc0000000.toInt())
                )
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            addView(text("⌖ ${course.area}", 14f, Color.WHITE, true).apply {
                setShadowLayer(8f, 0f, 2f, 0x66000000)
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(20)
                topMargin = dp(20)
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text("${course.mood} 무드 추천", 11f, ACCENT, true).apply {
                    setPadding(dp(9), dp(4), dp(9), dp(4))
                    background = rounded(Color.WHITE, 999)
                }, lpWrap())
                addView(text(course.title, 30f, Color.WHITE, true).apply {
                    setLineSpacing(0f, .95f)
                    setShadowLayer(10f, 0f, 2f, 0x66000000)
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 10))
                addView(text(course.description, 14f, 0xe6ffffff.toInt()).apply {
                    setLineSpacing(dp(3).toFloat(), 1f)
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 10))
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            ).apply {
                setMargins(dp(20), 0, dp(20), dp(22))
            })
            addView(View(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                isClickable = true
                setOnClickListener { showCourseDetail(course) }
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            bindCourseClick(this, course)
        }

    private fun stopList(course: CourseUi): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = topBottomStroke()
            course.stops.forEachIndexed { index, stop ->
                addView(timelineStop(index + 1, stop.first, stop.second), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

    private fun timelineStop(step: Int, name: String, detail: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(18), dp(4), dp(18))
            addView(text(stopIcon(step), 18f, INK, true).apply {
                gravity = Gravity.CENTER
                background = rounded(0xfffafafa.toInt(), 14, SOFT, 1)
            }, LinearLayout.LayoutParams(dp(38), dp(38)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(text("0$step", 12f, ACCENT, true), LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT))
                    addView(text(name, 16f, 0xff3f2a2a.toInt(), true))
                })
                addView(text(detail, 14f, 0xff52525b.toInt()), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            })
        }

    private fun aiPage(): View {
        searchStep = 0
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(0, 0, 0, dp(48))
        }
        searchScroll = scroll
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(72))
        }
        scroll.addView(root)
        root.addView(text("AI 추천", 14f, LIGHT_TEXT))
        root.addView(text("지도 위에서 코스를 만들어요", 24f, INK, true))
        root.addView(aiMapPreview(), lpMatch(dp(220), top = 18))
        root.addView(makerCard(), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))

        chatFlow = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(chatFlow, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 14))
        chatFlow.addView(chatBubble("어떤 데이트를 원하시나요?", false), lpWrap(bottom = 10))

        courseInput = EditText(this).apply {
            hint = "예: 강남에서 조용한 첫 데이트 추천해줘"
            textSize = 15f
            minLines = 4
            maxLines = 5
            gravity = Gravity.TOP
            imeOptions = EditorInfo.IME_ACTION_DONE
            setTextColor(INK)
            setHintTextColor(LIGHT_TEXT)
            background = rounded(SOFT, 28)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setOnFocusChangeListener { _, hasFocus ->
                bottomBar.visibility = if (hasFocus) View.GONE else View.VISIBLE
                if (hasFocus) scroll.postDelayed({ scroll.smoothScrollTo(0, courseInput.top - dp(24)) }, 180)
            }
            setOnEditorActionListener { view, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    view.clearFocus()
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(view.windowToken, 0)
                    true
                } else {
                    false
                }
            }
        }
        root.addView(courseInput, lpMatch(dp(92), top = 4))

        generateButton = button("다음 질문 보기")
        root.addView(generateButton, lpMatch(dp(56), top = 18))

        generateButton.setOnClickListener { advanceSearch() }
        return scroll
    }

    private fun aiMapPreview(): View =
        FrameLayout(this).apply {
            background = rounded(0xfff8fafc.toInt(), 28, SOFT, 1)
            clipToOutline = true
            addView(mapGrid(), FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
            addView(text("코스 지도 프리뷰", 12f, ACCENT, true).apply {
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = rounded(Color.WHITE, 999)
                elevation = dp(6).toFloat()
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(14)
                topMargin = dp(14)
            })
            addView(routePreviewLine(), FrameLayout.LayoutParams(dp(190), dp(88)).apply {
                leftMargin = dp(78)
                topMargin = dp(74)
            })
            addView(mapPin("1"), FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                leftMargin = dp(72)
                topMargin = dp(136)
            })
            addView(mapPin("2"), FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                leftMargin = dp(164)
                topMargin = dp(86)
            })
            addView(mapPin("3"), FrameLayout.LayoutParams(dp(34), dp(34)).apply {
                rightMargin = dp(76)
                topMargin = dp(120)
                gravity = Gravity.END
            })
            addView(mapPlace("카페"), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(44)
                topMargin = dp(164)
            })
            addView(mapPlace("산책"), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(138)
                topMargin = dp(48)
            })
            addView(mapPlace("저녁"), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END).apply {
                rightMargin = dp(42)
                topMargin = dp(154)
            })
        }

    private fun mapGrid(): View =
        View(this).apply {
            background = GradientDrawable().apply {
                setColor(0xfff8fafc.toInt())
                setStroke(dp(1), 0x12000000)
            }
        }

    private fun routePreviewLine(): View =
        View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), 0x99fb7185.toInt(), dp(10).toFloat(), dp(8).toFloat())
                cornerRadius = dp(38).toFloat()
            }
        }

    private fun mapPin(label: String): TextView =
        text(label, 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = rounded(ACCENT, 999, Color.WHITE, 3)
            elevation = dp(8).toFloat()
        }

    private fun mapPlace(label: String): TextView =
        text(label, 12f, 0xff3f2a2a.toInt(), true).apply {
            setPadding(dp(9), dp(7), dp(9), dp(7))
            background = rounded(0xeeffffff.toInt(), 999, SOFT, 1)
        }

    private fun makerCard(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = gradient(intArrayOf(0xfffb7185.toInt(), 0xfff9a8d4.toInt(), 0xfffdba74.toInt()), 32)
            addView(text("AI가 데이트 코스를 바로 만들어줘요", 21f, Color.WHITE, true).apply {
                setLineSpacing(0f, 1.02f)
            })
            addView(text("분위기나 상황을 입력하면 이동 동선까지 구성합니다.", 13f, 0xe6ffffff.toInt()).apply {
                setLineSpacing(dp(2).toFloat(), 1f)
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
        }

    private fun advanceSearch() {
        if (searchStep == 0) {
            val value = courseInput.text.toString().ifBlank { "강남에서 조용한 첫 데이트 추천해줘" }
            courseInput.clearFocus()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(courseInput.windowToken, 0)
            bottomBar.visibility = View.VISIBLE
            chatFlow.addView(chatBubble(value, true), lpWrap(bottom = 14))
            chatFlow.addView(chatBubble("좋아요. 몇 가지 더 물어볼게요.", false), lpWrap(bottom = 10))
            courseInput.visibility = View.GONE
            generateButton.visibility = View.GONE
            addChatQuestion(0)
            searchStep = 1
            scrollChatToBottom()
        } else {
            requestCourse()
        }
    }

    private fun addChatQuestion(index: Int) {
        val questions = listOf(
            ChatQuestion("예산은 어느 정도 생각하고 있나요?", listOf("2만원 이하", "3~5만원", "7만원 이상")),
            ChatQuestion("어떤 분위기를 원하시나요?", listOf("조용한", "야경", "활동적인", "감성 카페")),
            ChatQuestion("이동은 어떻게 할 예정인가요?", listOf("도보", "지하철", "드라이브"))
        )
        val question = questions[index]
        chatFlow.addView(chatQuestionCard(question.title, question.options) { answer ->
            chatFlow.addView(chatBubble(answer, true), lpWrap(bottom = 10))
            when (index) {
                0 -> selectedBudget = answer
                1 -> selectedMood = answer
                2 -> selectedTransport = answer
            }
            if (index < questions.lastIndex) {
                addChatQuestion(index + 1)
            } else {
                chatFlow.addView(chatBubble("좋아요. 조건을 반영해서 코스를 만들게요.", false), lpWrap(bottom = 10))
                chatFlow.addView(chatActionButton("코스 생성하기") { button ->
                    button.text = "생성 중..."
                    button.isEnabled = false
                    requestCourse()
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, bottom = 10))
                generateButton.visibility = View.GONE
            }
            scrollChatToBottom()
        }, lpWrap(bottom = 10))
        scrollChatToBottom()
    }

    private fun requestCourse() {
        val prompt = courseInput.text.toString().ifBlank { "강남에서 조용한 첫 데이트 추천해줘" }
        generateButton.isEnabled = false
        generateButton.text = "생성 중..."

        Thread {
            try {
                val course = FastApiClient.recommendCourse(
                    CourseRequest(prompt = prompt, mood = selectedMood, budget = selectedBudget, transport = selectedTransport)
                )
                runOnUiThread {
                    DateCourseRepository.course = course
                    chatFlow.addView(chatCourseCarousel(course), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, bottom = 14))
                    generateButton.text = "다시 생성하기"
                    generateButton.isEnabled = true
                    generateButton.visibility = View.GONE
                    scrollChatToBottom()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "서버 통신 실패: ${error.message}", Toast.LENGTH_LONG).show()
                    chatFlow.addView(chatBubble("서버 연결이 잠깐 실패했어요. 다시 시도해볼까요?", false), lpWrap(bottom = 10))
                    chatFlow.addView(chatActionButton("다시 생성하기") { button ->
                        button.text = "생성 중..."
                        button.isEnabled = false
                        requestCourse()
                    }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, bottom = 10))
                    generateButton.text = "코스 생성하기"
                    generateButton.isEnabled = true
                    generateButton.visibility = View.GONE
                    scrollChatToBottom()
                }
            }
        }.start()
    }

    private fun chatCourseCarousel(course: DateCourse): View {
        val generated = course.toCourseUi("generated")
        val localRecommendations = moodHeroes.values
            .filter { it.id != generated.id }
            .sortedWith(compareByDescending<CourseUi> { it.mood == selectedMood }.thenBy { it.title })
            .take(4)
        val recommendations = listOf(generated) + localRecommendations

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            addView(text("추천 코스를 골라보세요", 14f, 0xff3f2a2a.toInt(), true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, bottom = 10))
            addView(HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                clipToPadding = false
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    recommendations.forEach { item ->
                        addView(chatCourseCard(item), LinearLayout.LayoutParams(dp(244), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            marginEnd = dp(10)
                        })
                    }
                })
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT))
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun chatCourseCard(course: CourseUi): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(14))
            background = rounded(Color.WHITE, 24, SOFT, 1)
            elevation = dp(3).toFloat()
            clipToOutline = true
            addView(remoteImage(course.imageUrl), lpMatch(dp(128), bottom = 12))
            addView(text(course.title, 16f, INK, true).apply {
                maxLines = 2
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(text("${course.area} · ${course.time}", 13f, ACCENT, true).apply {
                maxLines = 1
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 6))
            addView(text(course.description, 14f, MUTED).apply {
                setLineSpacing(dp(2).toFloat(), 1f)
                maxLines = 3
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
            addView(text("상세 보기", 12f, ACCENT, true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 10))
            bindCourseClick(this, course)
        }

    private fun placeSearchPage(): View {
        val (scroll, root) = basePageHeader("장소 검색", "가고 싶은 장소를 찾아보세요")
        root.addView(searchPanel(), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 22))
        return scroll
    }

    private fun searchPanel(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val results = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(18), 0, dp(18), 0)
                background = rounded(SOFT, 24)
                addView(text("⌕", 17f, MUTED, true))
                addView(EditText(context).apply {
                    hint = "지역, 장소, 분위기 검색"
                    textSize = 14f
                    setSingleLine(true)
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    setTextColor(INK)
                    setHintTextColor(MUTED)
                    background = null
                    setPadding(0, 0, 0, 0)
                    setOnEditorActionListener { view, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                            requestPlaceSearchResults(results, view.text.toString())
                            view.clearFocus()
                            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                                .hideSoftInputFromWindow(view.windowToken, 0)
                            true
                        } else {
                            false
                        }
                    }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(10)
                })
            }, lpMatch(dp(56)))

            addView(HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    listOf("전체", "카페", "전시", "공원", "맛집", "야경").forEachIndexed { index, label ->
                        addView(filterChip(label, index == 0).apply {
                            setOnClickListener {
                                requestPlaceSearchResults(results, if (label == "전체") "" else label)
                            }
                        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                            marginEnd = dp(8)
                        })
                    }
                })
            }, lpMatch(dp(42), top = 14))

            addView(results)
            renderSearchResults(results, "")
        }

    private fun requestPlaceSearchResults(container: LinearLayout, query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            renderSearchResults(container, "")
            return
        }

        container.removeAllViews()
        container.addView(text("'$normalized' 기준으로 가까운 장소를 찾는 중...", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))

        Thread {
            runCatching {
                FastApiClient.searchPlaces(normalized, limit = 12)
            }.onSuccess { places ->
                runOnUiThread {
                    renderSearchPlaces(container, normalized, places)
                }
            }.onFailure { error ->
                runOnUiThread {
                    container.removeAllViews()
                    container.addView(text("검색 요청 실패: ${error.message}", 13f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
                    renderSearchResults(container, normalized)
                }
            }
        }.start()
    }

    private fun renderSearchPlaces(container: LinearLayout, query: String, places: List<NearbyPlace>) {
        container.removeAllViews()
        if (places.isEmpty()) {
            container.addView(text("'$query' 주변에서 장소를 찾지 못했어요.", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
            return
        }

        container.addView(text("'$query' 주변 가까운 장소", 14f, LIGHT_TEXT, true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
        val searchCourse = places.toSearchCourse(query)
        container.addView(courseRow(searchCourse), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
        places.forEach { place ->
            container.addView(nearbyPlaceRow(place), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
        }
    }

    private fun renderSearchResults(container: LinearLayout, query: String) {
        container.removeAllViews()
        val normalized = query.trim()
        val courses = moodHeroes.values
            .filter { course ->
                normalized.isBlank() ||
                    course.title.contains(normalized, ignoreCase = true) ||
                    course.area.contains(normalized, ignoreCase = true) ||
                    course.mood.contains(normalized, ignoreCase = true) ||
                    course.stops.any { it.first.contains(normalized, ignoreCase = true) || it.second.contains(normalized, ignoreCase = true) }
            }
            .ifEmpty { listOf(currentHero()) }
        courses.take(6).forEach { course ->
            container.addView(suggestionRow(course), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
        }
    }

    private fun List<NearbyPlace>.toSearchCourse(query: String): CourseUi {
        val stops = take(3)
        val validRouteStops = stops.mapNotNull { place ->
            val lat = place.latitude ?: return@mapNotNull null
            val lng = place.longitude ?: return@mapNotNull null
            MapStop(place.title, place.category, lat, lng)
        }
        return CourseUi(
            id = "search-$query-${stops.joinToString("-") { it.kakaoId ?: it.title }}",
            title = "$query 주변 데이트 코스",
            area = stops.firstOrNull()?.city?.ifBlank { query } ?: query,
            description = "검색한 지역 좌표를 기준으로 가까운 장소 ${stops.size}곳을 거리순으로 연결했습니다.",
            time = stops.firstOrNull()?.let { "가장 가까운 곳 ${formatDistance(it.distanceMeters)}" } ?: "검색 결과",
            budget = "검색 장소",
            mood = "검색",
            imageUrl = stops.firstOrNull()?.imageUrl ?: currentHero().imageUrl,
            stops = stops.map { it.title to it.category }.ifEmpty { currentHero().stops },
            routeStops = validRouteStops
        )
    }

    private fun suggestionRow(course: CourseUi): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 24, SOFT, 1)
            addView(remoteImage(course.imageUrl), LinearLayout.LayoutParams(dp(86), dp(72)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(course.stops.first().first, 15f, INK, true))
                addView(text("${course.area} · ${course.mood} · ${course.stops.first().second}", 14f, MUTED).apply {
                    setLineSpacing(dp(2).toFloat(), 1f)
                    maxLines = 2
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            })
            bindCourseClick(this, course)
        }

    private fun nearbyPage(): View {
        val (scroll, root) = basePageHeader("내 주변", "지금 근처 데이트")
        root.addView(text("현재 위치 기준으로 가까운 장소를 찾고 있어요.", 14f, MUTED).apply {
            setPadding(0, dp(12), 0, 0)
        })
        loadNearbyPlaces(root)
        return scroll
    }

    private fun loadNearbyPlaces(root: LinearLayout) {
        pendingNearbyRoot = root
        if (!hasLocationPermission()) {
            root.addView(button("위치 권한 허용하기").apply {
                setOnClickListener {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        LOCATION_PERMISSION_REQUEST
                    )
                }
            }, lpMatch(dp(54), top = 18))
            return
        }

        val location = lastKnownLocation()
        if (location != null) {
            requestNearbyPlaces(root, normalizedNearbyLocation(location))
        } else {
            requestSingleLocation(root)
        }
    }

    private fun requestSingleLocation(root: LinearLayout) {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { manager.isProviderEnabled(it) }
        if (provider == null) {
            root.addView(text("위치 서비스를 켠 뒤 다시 시도해주세요.", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
            return
        }
        root.addView(text("현재 위치를 가져오는 중...", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
        try {
            var handled = false
            val handler = Handler(Looper.getMainLooper())
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (handled) return
                    handled = true
                    handler.removeCallbacksAndMessages(null)
                    pendingNearbyRoot?.let { requestNearbyPlaces(it, normalizedNearbyLocation(location)) }
                }
            }
            handler.postDelayed({
                if (handled) return@postDelayed
                handled = true
                manager.removeUpdates(listener)
                pendingNearbyRoot?.let { requestNearbyPlaces(it, fallbackSeoulLocation()) }
            }, LOCATION_TIMEOUT_MS)
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        } catch (error: SecurityException) {
            root.addView(text("위치 권한을 확인해주세요.", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
        }
    }

    private fun requestNearbyPlaces(root: LinearLayout, location: Location) {
        val label = if (location.provider == FALLBACK_LOCATION_PROVIDER) {
            "테스트 위치(서울) 기준"
        } else {
            "위도 %.5f · 경도 %.5f 기준".format(location.latitude, location.longitude)
        }
        root.addView(text(label, 12f, LIGHT_TEXT), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 10))
        Thread {
            runCatching {
                FastApiClient.nearbyPlaces(location.latitude, location.longitude, limit = 12)
            }.onSuccess { places ->
                runOnUiThread {
                    clearNearbyDynamicRows(root)
                    if (places.isEmpty()) {
                        root.addView(text("근처 장소를 찾지 못했어요.", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
                    } else {
                        val nearbyCourse = places.toNearbyCourse()
                        root.addView(courseRow(nearbyCourse), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
                        places.drop(3).take(4).forEach { place ->
                            root.addView(nearbyPlaceRow(place), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
                        }
                    }
                }
            }.onFailure { error ->
                runOnUiThread {
                    clearNearbyDynamicRows(root)
                    root.addView(text("근처 장소 요청 실패: ${error.message}", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
                }
            }
        }.start()
    }

    private fun clearNearbyDynamicRows(root: LinearLayout) {
        while (root.childCount > 2) {
            root.removeViewAt(2)
        }
    }

    private fun List<NearbyPlace>.toNearbyCourse(): CourseUi {
        val stops = take(3).ifEmpty { return currentHero() }
        val validRouteStops = stops.mapNotNull { place ->
            val lat = place.latitude ?: return@mapNotNull null
            val lng = place.longitude ?: return@mapNotNull null
            MapStop(place.title, place.category, lat, lng)
        }
        return CourseUi(
            id = "nearby-${stops.joinToString("-") { it.kakaoId ?: it.title }}",
            title = "내 주변 가까운 데이트 코스",
            area = stops.first().city.ifBlank { "현재 위치 주변" },
            description = "현재 위치에서 가까운 장소 ${stops.size}곳을 거리순으로 연결했습니다.",
            time = "가장 가까운 곳 ${formatDistance(stops.first().distanceMeters)}",
            budget = "근처 장소",
            mood = "내 주변",
            imageUrl = stops.first().imageUrl ?: currentHero().imageUrl,
            stops = stops.map { it.title to it.category },
            routeStops = validRouteStops
        )
    }

    private fun nearbyPlaceRow(place: NearbyPlace): View {
        val course = place.toCourseUi()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 24, SOFT, 1)
            addView(remoteImage(course.imageUrl), LinearLayout.LayoutParams(dp(86), dp(72)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(place.title, 15f, INK, true))
                addView(text("${formatDistance(place.distanceMeters)} · ${place.category}", 14f, ACCENT, true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
                addView(text(place.address, 13f, MUTED).apply {
                    maxLines = 2
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            })
            bindCourseClick(this, course)
        }
    }

    private fun NearbyPlace.toCourseUi(): CourseUi =
        CourseUi(
            id = kakaoId ?: title,
            title = title,
            area = city.ifBlank { address },
            description = "$category · 평점 $rating · 리뷰 ${reviewCount}개",
            time = formatDistance(distanceMeters),
            budget = "근처 장소",
            mood = "내 주변",
            imageUrl = imageUrl ?: currentHero().imageUrl,
            stops = listOf(title to category),
            routeStops = listOfNotNull(
                if (latitude != null && longitude != null) MapStop(title, category, latitude, longitude) else null
            )
        )

    private fun formatDistance(meters: Int): String =
        if (meters >= 1000) "%.1fkm".format(meters / 1000.0) else "${meters}m"

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun lastKnownLocation(): Location? {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!hasLocationPermission()) return null
        return runCatching {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    private fun normalizedNearbyLocation(location: Location): Location =
        if (isKoreaLocation(location)) {
            location
        } else {
            fallbackSeoulLocation()
        }

    private fun fallbackSeoulLocation(): Location =
        Location(FALLBACK_LOCATION_PROVIDER).apply {
            latitude = DEFAULT_SEOUL_LATITUDE
            longitude = DEFAULT_SEOUL_LONGITUDE
            time = System.currentTimeMillis()
        }

    private fun isKoreaLocation(location: Location): Boolean =
        location.latitude in 33.0..39.5 && location.longitude in 124.0..132.0

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
            pendingNearbyRoot?.let {
                it.removeAllViews()
                it.addView(text("내 주변", 14f, LIGHT_TEXT))
                it.addView(text("지금 근처 데이트", 24f, INK, true))
                it.addView(text("현재 위치 기준으로 가까운 장소를 찾고 있어요.", 14f, MUTED).apply {
                    setPadding(0, dp(12), 0, 0)
                })
                loadNearbyPlaces(it)
            }
        }
    }

    private fun savedPage(): View {
        val (scroll, root) = basePageHeader("내 데이트", "코스 보관함")
        root.addView(sectionTitle("최근 본 코스", "2개"), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 26))
        listOf("seongsu", "gangnam").mapNotNull { courseById(it) }.forEach { course ->
            root.addView(courseRow(course), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
        }
        root.addView(sectionTitle("저장한 코스", "${savedCourseIds.size}개"), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 28))
        savedCourseIds.mapNotNull { courseById(it) }.forEach { course ->
            root.addView(courseRow(course, saved = true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 12))
        }
        return scroll
    }

    private fun courseRow(course: CourseUi, saved: Boolean = false): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.WHITE, 24, SOFT, 1)
            addView(remoteImage(course.imageUrl), LinearLayout.LayoutParams(dp(86), dp(74)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(course.title, 15f, INK, true))
                addView(text("${course.area} · ${course.stops.first().first}", 14f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(tag(course.mood))
                    addView(tag(course.time), lpWrap(left = 6))
                    if (saved) addView(tag("저장됨"), lpWrap(left = 6))
                }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            })
            bindCourseClick(this, course)
        }

    private fun showCourseDetail(course: CourseUi) {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(detailContent(course, this))
            show()
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun bindCourseClick(view: View, course: CourseUi) {
        view.isClickable = true
        view.setOnClickListener { showCourseDetail(course) }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                bindCourseClick(view.getChildAt(index), course)
            }
        }
    }

    private fun detailContent(course: CourseUi, dialog: Dialog): View =
        ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(FrameLayout(context).apply {
                    addView(remoteImage(course.imageUrl), FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    addView(View(context).apply {
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(0x20000000, 0x10000000, 0xd0000000.toInt())
                        )
                    }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    addView(TextView(context).apply {
                        text = "‹"
                        textSize = 30f
                        gravity = Gravity.CENTER
                        setTextColor(ACCENT)
                        background = rounded(0xeeffffff.toInt(), 999)
                        setOnClickListener { dialog.dismiss() }
                    }, FrameLayout.LayoutParams(dp(42), dp(42)).apply {
                        leftMargin = dp(18)
                        topMargin = dp(18)
                    })
                    addView(TextView(context).apply {
                        text = if (savedCourseIds.contains(course.id)) "♥" else "♡"
                        textSize = 20f
                        gravity = Gravity.CENTER
                        setTextColor(if (savedCourseIds.contains(course.id)) Color.WHITE else ACCENT)
                        background = rounded(if (savedCourseIds.contains(course.id)) ACCENT else 0xeeffffff.toInt(), 999)
                        setOnClickListener {
                            toggleSaved(course.id)
                            val saved = savedCourseIds.contains(course.id)
                            text = if (saved) "♥" else "♡"
                            setTextColor(if (saved) Color.WHITE else ACCENT)
                            background = rounded(if (saved) ACCENT else 0xeeffffff.toInt(), 999)
                        }
                    }, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.END).apply {
                        rightMargin = dp(18)
                        topMargin = dp(18)
                    })
                    addView(LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(text(course.area, 13f, 0xd6ffffff.toInt(), true))
                        addView(text(course.title, 30f, Color.WHITE, true).apply { setLineSpacing(0f, .98f) }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
                    }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).apply {
                        setMargins(dp(20), 0, dp(20), dp(24))
                    })
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(310)))

                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(20), dp(20), dp(20), dp(32))
                    addView(text(course.description, 14f, MUTED).apply {
                        setLineSpacing(dp(3).toFloat(), 1f)
                    })
                    addView(detailMeta(course), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 16))
                    addView(routeStrip(course), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 18))
                    course.stops.forEachIndexed { index, stop ->
                        addView(detailStop(course, index + 1, stop.first, stop.second), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 10))
                    }
                })
            })
        }

    private fun routeStrip(course: CourseUi): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(0xfffff7ed.toInt(), 22, 0xfffed7aa.toInt(), 1)
            addView(text("동선 요약", 11f, 0xfff97316.toInt(), true))
            addView(text(course.stops.joinToString(" → ") { it.first }, 14f, 0xff3f2a2a.toInt(), true).apply {
                setLineSpacing(dp(2).toFloat(), 1f)
            }, lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
            addView(routeMapPreview(course), lpMatch(dp(188), top = 14))
        }

    private fun routeMapPreview(course: CourseUi): View =
        FrameLayout(this).apply {
            background = rounded(0xfff8fafc.toInt(), 22, 0xfffed7aa.toInt(), 1)
            clipToOutline = true
            setOnClickListener { openCourseMap(course) }

            val mapView = MapView(context)
            addView(mapView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))

            addView(text("지도 프리뷰", 11f, ACCENT, true).apply {
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = rounded(0xeeffffff.toInt(), 999)
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(10)
                topMargin = dp(10)
            })

            if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank() || course.routeStops.isEmpty()) {
                addView(text("카카오 지도 인증 또는 위치 정보가 필요합니다.", 13f, MUTED, true).apply {
                    gravity = Gravity.CENTER
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                return@apply
            }

            post {
                runCatching {
                    mapView.start(object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit

                        override fun onMapError(error: Exception) {
                            showPreviewMapError(this@apply)
                        }
                    }, object : KakaoMapReadyCallback() {
                override fun onMapReady(kakaoMap: KakaoMap) {
                    drawPreviewStops(kakaoMap, course.routeStops)
                    if (course.routeStops.size >= 2) {
                        drawPreviewRoute(kakaoMap, course.routeStops)
                    }
                }

                        override fun getPosition(): LatLng =
                            course.routeStops.firstOrNull()?.let { LatLng.from(it.latitude, it.longitude) }
                                ?: LatLng.from(37.5665, 126.9780)

                        override fun getZoomLevel(): Int = 13
                    })
                }.onFailure {
                    showPreviewMapError(this)
                }
            }

            addView(View(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { openCourseMap(course) }
            }, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }

    private fun showPreviewMapError(parent: FrameLayout) {
        parent.addView(text("지도 프리뷰를 불러오지 못했습니다.", 13f, MUTED, true).apply {
            gravity = Gravity.CENTER
            background = rounded(0xeeffffff.toInt(), 18)
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun drawPreviewStops(kakaoMap: KakaoMap, stops: List<MapStop>) {
        val labelManager = kakaoMap.labelManager ?: return
        val labelStyles = labelManager.addLabelStyles(
            LabelStyles.from(LabelStyle.from().setTextStyles(24, Color.WHITE, dp(3), ACCENT))
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
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(it.latitude, it.longitude)))
        }
    }

    private fun drawPreviewRoute(kakaoMap: KakaoMap, stops: List<MapStop>) {
        Thread {
            val route = runCatching { KakaoDirectionsClient.route(stops) }
                .getOrElse { RouteSummary(0, 0, stops) }
            runOnUiThread {
                val points = route.points.map { LatLng.from(it.latitude, it.longitude) }
                val stylesSet = RouteLineStylesSet.from(
                    "previewRoute",
                    RouteLineStyles.from(RouteLineStyle.from(dp(6).toFloat(), ACCENT, dp(2).toFloat(), Color.WHITE))
                )
                val layer = kakaoMap.routeLineManager?.getLayer() ?: return@runOnUiThread
                val segment = RouteLineSegment.from(points).setStyles(stylesSet.getStyles(0))
                layer.addRouteLine(RouteLineOptions.from(segment).setStylesSet(stylesSet))
            }
        }.start()
    }

    private fun FrameLayout.addRoutePin(label: String, place: String, xRatio: Float, yRatio: Float) {
        post {
            val x = (width * xRatio).toInt()
            val y = (height * yRatio).toInt()
            addView(mapPin(label), FrameLayout.LayoutParams(dp(36), dp(36)).apply {
                leftMargin = x - dp(18)
                topMargin = y - dp(18)
            })
            addView(mapPlace(place), FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = (x - dp(34)).coerceAtLeast(dp(8))
                topMargin = (y + dp(18)).coerceAtMost(height - dp(34))
            })
        }
    }

    private fun openCourseMap(course: CourseUi) {
        if (course.routeStops.isEmpty()) {
            Toast.makeText(this, "표시할 위치 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, MapActivity::class.java).apply {
            putExtra(MapActivity.EXTRA_TITLE, course.title)
            putExtra(MapActivity.EXTRA_AREA, course.area)
            putStringArrayListExtra(MapActivity.EXTRA_STOP_NAMES, ArrayList(course.routeStops.map { it.name }))
            putStringArrayListExtra(MapActivity.EXTRA_STOP_DETAILS, ArrayList(course.routeStops.map { it.detail }))
            putExtra(MapActivity.EXTRA_STOP_LATS, course.routeStops.map { it.latitude }.toDoubleArray())
            putExtra(MapActivity.EXTRA_STOP_LNGS, course.routeStops.map { it.longitude }.toDoubleArray())
        })
    }

    private fun detailMeta(course: CourseUi): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            listOf("시간" to course.time, "예산" to course.budget, "분위기" to course.mood).forEach { item ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(12), dp(10), dp(12))
                    background = rounded(0xfffafafa.toInt(), 18, SOFT, 1)
                    addView(text(item.first, 11f, LIGHT_TEXT, true))
                    addView(text(item.second, 13f, INK, true), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                })
            }
        }

    private fun detailStop(course: CourseUi, step: Int, title: String, desc: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(0xfffafafa.toInt(), 22)
            addView(text("0$step", 12f, ACCENT, true).apply {
                gravity = Gravity.CENTER
                background = rounded(0xfffff1f2.toInt(), 12)
            }, LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(title, 15f, INK, true))
                addView(text(desc, 13f, MUTED), lpMatch(ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))
                addView(remoteImage(course.imageUrl), lpMatch(dp(136), top = 16))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            })
        }

    private fun questionCard(title: String, options: List<String>, activeIndex: Int): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(0xfffafafa.toInt(), 28, SOFT, 1)
            addView(text(title, 15f, INK, true))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                options.forEachIndexed { index, option ->
                    addView(chip(option, index == activeIndex, small = true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38)).apply {
                        marginEnd = dp(8)
                        topMargin = dp(14)
                    })
                }
            })
        }

    private fun chatQuestionCard(title: String, options: List<String>, onSelected: (String) -> Unit): View =
        LinearLayout(this).apply questionCard@ {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(0xfffff1f2.toInt(), 26)
            addView(text(title, 16f, 0xff3f2a2a.toInt(), true))

            val rows = if (options.size > 3) options.chunked(2) else listOf(options)
            rows.forEachIndexed { rowIndex, rowOptions ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    rowOptions.forEach { option ->
                        addView(TextView(context).apply {
                            text = option
                            textSize = 14f
                            gravity = Gravity.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ACCENT)
                            setPadding(dp(16), 0, dp(16), 0)
                            background = rounded(Color.WHITE, 999, 0xffffe4e6.toInt(), 1)
                            setOnClickListener {
                                disableOptionButtons(this@questionCard)
                                setTextColor(Color.WHITE)
                                background = rounded(ACCENT, 999)
                                onSelected(option)
                            }
                        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).apply {
                            marginEnd = dp(10)
                        })
                    }
                }, lpMatch(dp(40), top = if (rowIndex == 0) 16 else 8))
            }
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
            }
        }

    private fun disableOptionButtons(view: View) {
        if (view is TextView) view.isEnabled = false
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                disableOptionButtons(view.getChildAt(i))
            }
        }
    }

    private fun chatActionButton(label: String, onClick: (TextView) -> Unit): View =
        LinearLayout(this).apply {
            gravity = Gravity.START
            addView(TextView(context).apply {
                text = label
                textSize = 15f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = rounded(ACCENT, 24)
                setOnClickListener {
                    onClick(this)
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        }

    private fun scrollChatToBottom() {
        if (::searchScroll.isInitialized) {
            searchScroll.postDelayed({ searchScroll.smoothScrollTo(0, searchScroll.getChildAt(0).bottom) }, 120)
        }
    }

    private fun baseScroll(): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setPadding(0, 0, 0, dp(96))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(116))
        }
        scroll.addView(root)
        return scroll to root
    }

    private fun basePageHeader(eyebrow: String, title: String): Pair<ScrollView, LinearLayout> {
        val (scroll, root) = baseScroll()
        root.addView(text(eyebrow, 14f, LIGHT_TEXT))
        root.addView(text(title, 24f, INK, true))
        return scroll to root
    }

    private fun header(eyebrow: String, title: String, action: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(eyebrow, 14f, LIGHT_TEXT))
                addView(text(title, 24f, INK, true))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(TextView(context).apply {
                text = if (savedCourseIds.contains(currentHero().id)) "♥" else action
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(if (savedCourseIds.contains(currentHero().id)) Color.WHITE else INK)
                background = rounded(if (savedCourseIds.contains(currentHero().id)) ACCENT else SOFT, 999)
                setOnClickListener {
                    toggleSaved(currentHero().id)
                    val saved = savedCourseIds.contains(currentHero().id)
                    text = if (saved) "♥" else action
                    setTextColor(if (saved) Color.WHITE else INK)
                    background = rounded(if (saved) ACCENT else SOFT, 999)
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
        }

    private fun sectionTitle(title: String, count: String): View =
        LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(text(title, 17f, INK, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(text(count, 12f, ACCENT, true))
        }

    private fun renderBottomBar() {
        bottomBar.removeAllViews()
        listOf(
            Triple("home", "⌂", "홈"),
            Triple("ai", "✦", "AI 추천"),
            Triple("search", "⌕", "검색"),
            Triple("nearby", "⌖", "근처"),
            Triple("saved", "♡", "저장")
        ).forEach { (id, icon, label) ->
            val active = currentPage == id
            bottomBar.addView(TextView(this).apply {
                text = "$icon $label"
                textSize = 11f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (active) Color.WHITE else MUTED)
                background = rounded(if (active) ACCENT else Color.TRANSPARENT, 18)
                setOnClickListener { renderPage(id) }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginEnd = dp(4)
            })
        }
    }

    private fun chip(label: String, active: Boolean, small: Boolean = false): TextView =
        TextView(this).apply {
            text = label
            textSize = if (small) 13f else 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (active) Color.WHITE else MUTED)
            background = rounded(if (active) ACCENT else SOFT, 999)
            setPadding(dp(if (small) 14 else 16), 0, dp(if (small) 14 else 16), 0)
        }

    private fun filterChip(label: String, active: Boolean): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (active) Color.WHITE else MUTED)
            background = rounded(if (active) ACCENT else Color.WHITE, 999, if (active) ACCENT else SOFT, 1)
            setPadding(dp(13), 0, dp(13), 0)
        }

    private fun mapButton(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ACCENT)
            setPadding(dp(11), dp(9), dp(11), dp(9))
            background = rounded(Color.WHITE, 999, 0xffffe4e6.toInt(), 1)
            setOnClickListener { onClick() }
        }

    private fun tag(label: String): TextView =
        TextView(this).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(MUTED)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = rounded(SOFT, 999)
        }

    private fun chatBubble(message: String, user: Boolean): View =
        TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(if (user) Color.WHITE else 0xff3f2a2a.toInt())
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(if (user) ACCENT else 0xfffff1f2.toInt(), 24)
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                if (user) (resources.displayMetrics.widthPixels * .62f).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (user) Gravity.END else Gravity.START
            }
        }

    private fun button(label: String): TextView =
        TextView(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(ACCENT, 24)
            elevation = dp(4).toFloat()
        }

    private fun remoteImage(imageUrl: String): ImageView =
        ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(SOFT, 18)
            clipToOutline = true
            Thread {
                runCatching {
                    URL(imageUrl).openStream().use { BitmapFactory.decodeStream(it) }
                }.onSuccess { bitmap ->
                    runOnUiThread { setImageBitmap(bitmap) }
                }
            }.start()
        }

    private fun currentHero(): CourseUi =
        moodHeroes[selectedMood] ?: moodHeroes.getValue("조용한")

    private fun courseById(id: String): CourseUi? =
        moodHeroes.values.firstOrNull { it.id == id }

    private fun toggleSaved(id: String) {
        if (savedCourseIds.contains(id)) savedCourseIds.remove(id) else savedCourseIds.add(id)
    }

    private fun stopIcon(step: Int): String =
        when (step) {
            1 -> "☕"
            2 -> "📷"
            else -> "🍽"
        }

    private fun DateCourse.toCourseUi(id: String): CourseUi =
        CourseUi(
            id = id,
            title = title,
            area = area,
            description = description,
            time = time,
            budget = budget,
            mood = selectedMood,
            imageUrl = currentHero().imageUrl,
            stops = stops.map { it.name to it.detail }.ifEmpty { currentHero().stops },
            routeStops = stops.mapNotNull { stop ->
                val lat = stop.latitude ?: return@mapNotNull null
                val lng = stop.longitude ?: return@mapNotNull null
                MapStop(stop.name, stop.detail, lat, lng)
            }.ifEmpty { currentHero().routeStops }
        )

    private fun text(value: String, sp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = sp
            setTextColor(color)
            includeFontPadding = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != null) setStroke(dp(strokeWidth), strokeColor)
        }

    private fun gradient(colors: IntArray, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun topBottomStroke(): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(dp(1), SOFT)
        }

    private fun lpMatch(height: Int, top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
        }

    private fun lpWrap(top: Int = 0, bottom: Int = 0, left: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top)
            bottomMargin = dp(bottom)
            leftMargin = dp(left)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private data class CourseUi(
        val id: String,
        val title: String,
        val area: String,
        val description: String,
        val time: String,
        val budget: String,
        val mood: String,
        val imageUrl: String,
        val stops: List<Pair<String, String>>,
        val routeStops: List<MapStop>
    )

    private data class ChatQuestion(
        val title: String,
        val options: List<String>
    )

    private companion object {
        const val APP_BG = 0xfffff3f3.toInt()
        const val ACCENT = 0xfffb7185.toInt()
        const val INK = 0xff18181b.toInt()
        const val MUTED = 0xff71717a.toInt()
        const val LIGHT_TEXT = 0xffa1a1aa.toInt()
        const val SOFT = 0xfff4f4f5.toInt()
        const val LOCATION_PERMISSION_REQUEST = 2001
        const val FALLBACK_LOCATION_PROVIDER = "fallback-seoul"
        const val DEFAULT_SEOUL_LATITUDE = 37.5665
        const val DEFAULT_SEOUL_LONGITUDE = 126.9780
        const val LOCATION_TIMEOUT_MS = 3000L

        fun photoUrl(fileName: String): String =
            "${ApiConfig.BASE_URL}/photos/$fileName"
    }
}
