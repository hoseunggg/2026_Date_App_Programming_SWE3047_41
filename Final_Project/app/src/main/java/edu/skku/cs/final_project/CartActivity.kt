package edu.skku.cs.final_project

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class CartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Saved"
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
        root.addView(Ui.text(this, "저장한 코스", 28f, Ui.INK, true))
        root.addView(Ui.text(this, DateCourseRepository.course.title, 18f, Ui.INK, true).apply {
            setPadding(Ui.dp(context, 18), Ui.dp(context, 18), Ui.dp(context, 18), Ui.dp(context, 6))
            background = Ui.bg(Ui.SOFT, Ui.dp(context, 24))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = Ui.dp(this@CartActivity, 20)
        })
        root.addView(Ui.text(this, "최근 저장한 코스가 여기에 표시됩니다.", 14f, Ui.MUTED))
        return scroll
    }
}
