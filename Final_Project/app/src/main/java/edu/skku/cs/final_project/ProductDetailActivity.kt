package edu.skku.cs.final_project

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Place Detail"
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
        val stop = DateCourseRepository.course.stops.first()
        root.addView(Ui.text(this, stop.name, 30f, Ui.INK, true))
        root.addView(Ui.text(this, stop.place, 14f, Ui.MUTED, true).apply {
            setPadding(0, Ui.dp(context, 8), 0, Ui.dp(context, 18))
        })
        root.addView(Ui.text(this, stop.detail, 16f, Ui.MUTED).apply {
            setLineSpacing(Ui.dp(context, 4).toFloat(), 1.1f)
        })
        root.addView(Ui.centeredText(this, "네이버지도 / 카카오맵 열기", 15f, Ui.WHITE, true).apply {
            background = Ui.bg(Ui.DARK, Ui.dp(context, 20))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 54)).apply {
            topMargin = Ui.dp(this@ProductDetailActivity, 24)
        })
        return scroll
    }
}
