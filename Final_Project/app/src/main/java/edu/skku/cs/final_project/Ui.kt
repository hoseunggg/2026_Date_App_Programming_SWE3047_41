package edu.skku.cs.final_project

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView

object Ui {
    const val INK = 0xff18181b.toInt()
    const val MUTED = 0xff71717a.toInt()
    const val SOFT = 0xfff4f4f5.toInt()
    const val WHITE = Color.WHITE
    const val BLACK = 0xff09090b.toInt()
    const val CREAM = 0xfffff7ed.toInt()
    const val ROSE = 0xffffe4e6.toInt()
    const val AMBER = 0xfffffbeb.toInt()
    const val DARK = 0xff09090b.toInt()

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun bg(color: Int, radius: Int = 0, strokeColor: Int? = null, strokeWidth: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) setStroke(strokeWidth, strokeColor)
        }

    fun gradient(start: Int, end: Int, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = radius.toFloat()
        }

    fun text(
        context: Context,
        value: String,
        sp: Float,
        color: Int = INK,
        bold: Boolean = false
    ): TextView {
        return TextView(context).apply {
            text = value
            textSize = sp
            setTextColor(color)
            includeFontPadding = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    }

    fun centeredText(
        context: Context,
        value: String,
        sp: Float,
        color: Int = INK,
        bold: Boolean = false
    ): TextView =
        text(context, value, sp, color, bold).apply {
            gravity = Gravity.CENTER
        }
}
