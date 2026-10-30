package com.example.welnesstracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import kotlin.math.max

/** Minimal bar chart with a dashed goal line; no third-party chart library needed. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private var goal = 0f

    private val density = resources.displayMetrics.density
    private val fontScale = resources.configuration.fontScale

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f * density * fontScale
        textAlign = Paint.Align.CENTER
    }

    fun setData(values: List<Float>, labels: List<String>, goal: Float) {
        this.values = values
        this.labels = labels
        this.goal = goal
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        barPaint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        goalPaint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)
        textPaint.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)

        val labelHeight = textPaint.textSize * 1.8f
        val top = 8f * density
        val chartHeight = height - labelHeight - top
        val maxValue = max(values.max(), goal).coerceAtLeast(1f)
        val slot = width.toFloat() / values.size
        val barWidth = slot * 0.55f
        val radius = 4f * density

        values.forEachIndexed { i, v ->
            val barHeight = chartHeight * v / maxValue
            val left = i * slot + (slot - barWidth) / 2f
            val rect = RectF(left, top + chartHeight - barHeight, left + barWidth, top + chartHeight)
            canvas.drawRoundRect(rect, radius, radius, barPaint)
            canvas.drawText(labels.getOrElse(i) { "" }, i * slot + slot / 2f, height - labelHeight * 0.3f, textPaint)
        }

        if (goal > 0f) {
            val y = top + chartHeight - chartHeight * goal / maxValue
            canvas.drawLine(0f, y, width.toFloat(), y, goalPaint)
        }
    }
}
