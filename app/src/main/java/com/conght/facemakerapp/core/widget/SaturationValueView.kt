package com.conght.facemakerapp.core.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.withClip
import com.conght.facemakerapp.R

/**
 * Ô chọn saturation (ngang: trắng → hue) / value (dọc: trong suốt → đen), bo 8, con trỏ tròn 16dp.
 * Gọi [onChange] khi người dùng kéo.
 */
class SaturationValueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var onChange: ((saturation: Float, value: Float) -> Unit)? = null

    var hue: Float = 0f
        set(value) {
            field = value
            updateHueShader()
            invalidate()
        }

    var saturation: Float = 1f
        private set
    var value: Float = 1f
        private set

    private val radius = resources.getDimension(R.dimen.radius_medium)
    private val cursorRadius = resources.getDimension(R.dimen.color_picker_cursor) / 2
    private val rect = RectF()
    private val clip = Path()
    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.stroke_medium)
        color = ContextCompat.getColor(context, R.color.white)
    }

    fun setSaturationValue(saturation: Float, value: Float) {
        this.saturation = saturation.coerceIn(0f, 1f)
        this.value = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        updateHueShader()
        // Figma: trong suốt tới ~16% rồi đậm dần xuống đen
        valuePaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
            floatArrayOf(0f, VALUE_FADE_START, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    private fun updateHueShader() {
        if (width == 0) return
        val pure = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        huePaint.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, pure, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.withClip(clip) {
            drawRect(rect, huePaint)
            drawRect(rect, valuePaint)
        }
        val x = saturation * width
        val y = (1f - value) * height
        canvas.drawCircle(x, y, cursorRadius - cursorPaint.strokeWidth / 2, cursorPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                setSaturationValue(event.x / width, 1f - event.y / height)
                onChange?.invoke(saturation, value)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private companion object {
        const val VALUE_FADE_START = 0.16f
    }
}
