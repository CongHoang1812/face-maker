package com.conght.facemakerapp.core.widget

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.withClip
import com.conght.facemakerapp.R

/**
 * Nền nút gỗ (Figma 69:43): viền nâu viên thuốc + đổ bóng trong (inset 0 -1 0 2),
 * lõi "Subtract" 134×32 có các vết mẻ gỗ + highlight 1dp phía trên.
 * Hai đầu bo tròn và vết mẻ sát đầu giữ nguyên kích thước; khi nút rộng/hẹp hơn
 * 142dp chỉ phần thân giữa giãn ra, 2 vết mẻ giữa dời theo tỉ lệ.
 */
class WoodButtonDrawable(
    res: Resources,
    @ColorInt private val shadowColor: Int,
    @ColorInt private val outerColor: Int,
    @ColorInt private val coreColor: Int,
    @ColorInt private val highlightColor: Int,
) : Drawable() {

    private val density = res.displayMetrics.density
    private val coreInsetH = res.getDimension(R.dimen.wood_button_inset_h)
    private val coreInsetTop = res.getDimension(R.dimen.wood_button_inset_top)
    private val coreInsetBottom = res.getDimension(R.dimen.wood_button_inset_bottom)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val corePath = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        buildCorePath(
            bounds.left + coreInsetH,
            bounds.top + coreInsetTop,
            bounds.right - coreInsetH,
            bounds.bottom - coreInsetBottom,
        )
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        // Viền ngoài: lớp bóng tối, rồi vòng nâu (bóng dày 2dp hai bên, 1dp trên, 3dp dưới)
        rect.set(b)
        fillPill(canvas, shadowColor)
        rect.set(
            b.left + SHADOW_SIDE * density,
            b.top + SHADOW_TOP * density,
            b.right - SHADOW_SIDE * density,
            b.bottom - SHADOW_BOTTOM * density,
        )
        fillPill(canvas, outerColor)
        // Lõi: tô highlight, rồi phủ lõi lệch xuống 1dp -> còn lại dải sáng mép trên.
        // Tính ngoài withClip: bên trong, `density` sẽ là Canvas.getDensity() (= 0)
        val highlightOffset = HIGHLIGHT * density
        canvas.withClip(corePath) {
            paint.color = highlightColor
            drawPath(corePath, paint)
            translate(0f, highlightOffset)
            paint.color = coreColor
            drawPath(corePath, paint)
        }
    }

    private fun fillPill(canvas: Canvas, @ColorInt color: Int) {
        paint.color = color
        val r = rect.height() / 2f
        canvas.drawRoundRect(rect, r, r, paint)
    }

    /** Dựng lại path "Subtract" của Figma (toạ độ gốc 134×32) cho khung lõi thực tế. */
    private fun buildCorePath(left: Float, top: Float, right: Float, bottom: Float) {
        corePath.reset()
        val s = (bottom - top) / CORE_H
        val extra = (right - left) - CORE_W * s
        if (right - left <= bottom - top) {
            rect.set(left, top, right, bottom)
            corePath.addRoundRect(rect, rect.height() / 2f, rect.height() / 2f, Path.Direction.CW)
            return
        }
        fun lx(x: Float) = left + x * s
        fun rx(x: Float) = left + x * s + extra
        fun mx(x: Float, f: Float) = left + x * s + extra * f
        fun y(v: Float) = top + v * s
        val middleNotches = extra > -MIDDLE_NOTCH_MIN_EXTRA * s

        corePath.apply {
            moveTo(rx(118f), y(0f))
            cubicTo(rx(119.36f), y(0f), rx(120.681f), y(0.17f), rx(121.941f), y(0.489f))
            lineTo(rx(122.099f), y(5.154f))
            lineTo(rx(124.895f), y(1.558f))
            cubicTo(rx(130.28f), y(4.133f), rx(134f), y(9.632f), rx(134f), y(16f))
            cubicTo(rx(134f), y(24.837f), rx(126.837f), y(32f), rx(118f), y(32f))
            if (middleNotches) {
                lineTo(mx(94.7f, NOTCH_BOTTOM_F), y(32f))
                lineTo(mx(92.565f, NOTCH_BOTTOM_F), y(28.515f))
                lineTo(mx(92.831f, NOTCH_BOTTOM_F), y(32f))
            }
            lineTo(lx(29.435f), y(32f))
            lineTo(lx(29.092f), y(28.515f))
            lineTo(lx(25.104f), y(32f))
            lineTo(lx(16f), y(32f))
            cubicTo(lx(7.163f), y(32f), lx(0f), y(24.837f), lx(0f), y(16f))
            cubicTo(lx(0f), y(10.698f), lx(2.579f), y(5.999f), lx(6.551f), y(3.088f))
            lineTo(lx(11.706f), y(6.474f))
            lineTo(lx(9.179f), y(1.523f))
            cubicTo(lx(11.248f), y(0.547f), lx(13.56f), y(0f), lx(16f), y(0f))
            if (middleNotches) {
                lineTo(mx(56.592f, NOTCH_TOP_F), y(0f))
                lineTo(mx(58.185f, NOTCH_TOP_F), y(3.436f))
                lineTo(mx(59.045f, NOTCH_TOP_F), y(0f))
            }
            close()
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val CORE_W = 134f
        const val CORE_H = 32f
        const val SHADOW_SIDE = 2f
        const val SHADOW_TOP = 1f
        const val SHADOW_BOTTOM = 3f
        const val HIGHLIGHT = 1f

        // Vị trí tương đối của 2 vết mẻ giữa trên thân thẳng (x 16..118 của Figma)
        const val NOTCH_TOP_F = 0.41f
        const val NOTCH_BOTTOM_F = 0.76f

        // Nút quá hẹp thì bỏ 2 vết mẻ giữa để không đè lên vết mẻ sát đầu
        const val MIDDLE_NOTCH_MIN_EXTRA = 60f
    }
}
