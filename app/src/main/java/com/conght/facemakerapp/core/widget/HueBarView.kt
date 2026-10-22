package com.conght.facemakerapp.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.conght.facemakerapp.R

/**
 * Thanh chọn hue cầu vồng bo tròn (cao 18dp), con trỏ tròn 24dp viền trắng mang màu hue hiện tại.
 * Chiều cao view nên bằng con trỏ (24dp) để không bị cắt.
 */
class HueBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onChange: ((hue: Float) -> Unit)? = null

    var hue: Float = 0f
        set(value) {
            field = value.coerceIn(0f, MAX_HUE)
            invalidate()
        }

    private val cursorRadius = resources.getDimension(R.dimen.color_picker_hue_cursor) / 2
    private val strokeWidth = resources.getDimension(R.dimen.color_picker_cursor_stroke)
    private val barHeight = resources.getDimension(R.dimen.color_picker_hue_bar_h)
    private val bar = RectF()
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@HueBarView.strokeWidth
        color = ContextCompat.getColor(context, R.color.white)
        setShadowLayer(strokeWidth, 0f, strokeWidth / 2, ContextCompat.getColor(context, R.color.overlay_20))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Chừa chỗ cho con trỏ ở 2 đầu
        bar.set(cursorRadius, (h - barHeight) / 2, w - cursorRadius, (h + barHeight) / 2)
        val colors = IntArray(HUE_STOPS + 1) { Color.HSVToColor(floatArrayOf(it * MAX_HUE / HUE_STOPS, 1f, 1f)) }
        barPaint.shader = LinearGradient(bar.left, 0f, bar.right, 0f, colors, null, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val r = barHeight / 2
        canvas.drawRoundRect(bar, r, r, barPaint)
        val x = bar.left + hue / MAX_HUE * bar.width()
        fillPaint.color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val cy = height / 2f
        canvas.drawCircle(x, cy, cursorRadius - strokeWidth, fillPaint)
        canvas.drawCircle(x, cy, cursorRadius - strokeWidth, strokePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                hue = (event.x - bar.left) / bar.width() * MAX_HUE
                onChange?.invoke(hue)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private companion object {
        const val MAX_HUE = 360f
        const val HUE_STOPS = 6
    }
}
