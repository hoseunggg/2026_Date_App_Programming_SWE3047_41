package edu.skku.cs.final_project

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class FittingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Server"
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(context, 20), Ui.dp(context, 24), Ui.dp(context, 20), Ui.dp(context, 24))
            setBackgroundColor(Ui.WHITE)
        }
        scroll.addView(root)
        root.addView(Ui.text(this, "AWS + LLM", 30f, Ui.INK, true))
        root.addView(card("POST /recommend", "지역, 분위기, 예산, 시간 조건을 서버로 보내고 추천 코스 JSON을 받습니다."))
        root.addView(card("Ollama + Qwen2.5", "EC2에서 경량 LLM을 실행해 코스 설명과 순서를 생성합니다."))
        root.addView(card("Map API", "지도 SDK와 외부 지도 앱 Intent로 이동 경로를 제공합니다."))
        return scroll
    }

    private fun card(title: String, body: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(context, 18), Ui.dp(context, 18), Ui.dp(context, 18), Ui.dp(context, 18))
            background = Ui.bg(Ui.SOFT, Ui.dp(context, 24))
            addView(Ui.text(context, title, 18f, Ui.INK, true))
            addView(Ui.text(context, body, 14f, Ui.MUTED).apply {
                setPadding(0, Ui.dp(context, 6), 0, 0)
            })
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(this@FittingActivity, 14)
            }
        }
}
