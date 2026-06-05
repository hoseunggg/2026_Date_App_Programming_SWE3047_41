package edu.skku.cs.final_project

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Course"
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val course = DateCourseRepository.course
        val scroll = ScrollView(this).apply { setBackgroundColor(Ui.DARK) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(context, 20), Ui.dp(context, 24), Ui.dp(context, 20), Ui.dp(context, 24))
        }
        scroll.addView(root)
        root.addView(Ui.text(this, course.area, 14f, 0x88ffffff.toInt(), true))
        root.addView(Ui.text(this, course.title, 32f, Ui.WHITE, true).apply {
            setPadding(0, Ui.dp(context, 10), 0, 0)
        })
        root.addView(Ui.text(this, course.description, 14f, 0x99ffffff.toInt()))
        course.stops.forEachIndexed { index, stop ->
            root.addView(Ui.text(this, "STEP 0${index + 1} · ${stop.name}\n${stop.detail}\n${stop.place}", 15f, Ui.INK, true).apply {
                setPadding(Ui.dp(context, 18), Ui.dp(context, 14), Ui.dp(context, 18), Ui.dp(context, 14))
                background = Ui.bg(Ui.WHITE, Ui.dp(context, 24))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = Ui.dp(this@ResultActivity, 14)
            })
        }
        return scroll
    }
}
