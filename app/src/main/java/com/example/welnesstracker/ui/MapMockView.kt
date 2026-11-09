package com.example.welnesstracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/** Static map mockup with tappable pins (positions are fractions 0..1 of the view size). */
class MapMockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onPinSelected: ((Int) -> Unit)? = null

    private var pins: List<PointF> = emptyList()
    private var selected = -1
    private val density = resources.displayMetrics.density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val river = Path().apply {
        moveTo(-10f, 190f)
        cubicTo(60f, 150f, 120f, 230f, 200f, 170f)
        cubicTo(280f, 110f, 300f, 120f, 340f, 150f)
        lineTo(340f, 190f)
        cubicTo(300f, 160f, 230f, 215f, 200f, 215f)
        cubicTo(170f, 215f, 60f, 270f, -10f, 235f)
        close()
    }

    fun setPins(points: List<PointF>, selectedIndex: Int = -1) {
        pins = points
        selected = selectedIndex
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background, river, roads and green blocks are drawn in a 320 x 260 design space.
        canvas.save()
        canvas.scale(width / 320f, height / 260f)
        canvas.drawColor(0xFFE8EFE4.toInt())
        paint.style = Paint.Style.FILL
        paint.color = 0xFFA9D3EE.toInt()
        canvas.drawPath(river, paint)
        paint.color = 0xFFCFE3C4.toInt()
        canvas.drawRect(80f, 70f, 160f, 110f, paint)
        canvas.drawRect(180f, 10f, 240f, 50f, paint)
        canvas.drawRect(10f, 130f, 60f, 160f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawLine(0f, 60f, 320f, 60f, paint)
        canvas.drawLine(0f, 120f, 320f, 120f, paint)
        canvas.drawLine(70f, 0f, 70f, 260f, paint)
        canvas.drawLine(170f, 0f, 170f, 260f, paint)
        canvas.drawLine(250f, 0f, 250f, 260f, paint)
        canvas.restore()

        // Pins
        pins.forEachIndexed { i, p ->
            val r = (if (i == selected) 14f else 11f) * density
            val cx = p.x * width
            val cy = p.y * height
            paint.style = Paint.Style.FILL
            paint.color = if (i == selected) 0xFF00796B.toInt() else 0xFFD32F2F.toInt()
            val tail = Path().apply {
                moveTo(cx - r * 0.7f, cy - r * 0.75f)
                lineTo(cx, cy)
                lineTo(cx + r * 0.7f, cy - r * 0.75f)
                close()
            }
            canvas.drawPath(tail, paint)
            canvas.drawCircle(cx, cy - r, r, paint)
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy - r, r * 0.4f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return pins.isNotEmpty()
        if (event.action == MotionEvent.ACTION_UP) {
            val hit = pins.indices.minByOrNull { i ->
                hypot(event.x - pins[i].x * width, event.y - (pins[i].y * height - 11f * density))
            }
            if (hit != null) {
                val p = pins[hit]
                val distance = hypot(event.x - p.x * width, event.y - (p.y * height - 11f * density))
                if (distance <= 24f * density) {
                    selected = hit
                    invalidate()
                    onPinSelected?.invoke(hit)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
