package com.conght.facemakerapp.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withSave
import com.conght.facemakerapp.R

/**
 * Khối skeleton màu `shimmer_base` (theo tone của [ShimmerFrameLayout] cha gần nhất),
 * được dải sáng của layout cha quét qua.
 *
 * attr: `app:skeletonShape` (rect | oval | topRounded), `app:skeletonRadius`,
 * `app:skeletonMask` (drawable dùng làm khuôn, vd. khung giấy rách `bg_part_item`).
 */
class SkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private enum class Shape { RECT, OVAL, TOP_ROUNDED }

    private var shape = Shape.RECT
    private var radius = 0f
    private var mask: Drawable? = null

    private var host: ShimmerFrameLayout? = null
    private var highlightShader: Shader? = null
    private val shaderMatrix = Matrix()
    private val path = Path()
    private val rect = RectF()
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SkeletonView, defStyleAttr) {
            shape = Shape.entries[getInt(R.styleable.SkeletonView_skeletonShape, 0)]
            radius = getDimension(R.styleable.SkeletonView_skeletonRadius, 0f)
            mask = getDrawable(R.styleable.SkeletonView_skeletonMask)?.mutate()
        }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        host = findHost()?.also { it.register(this) }
    }

    override fun onDetachedFromWindow() {
        host?.unregister(this)
        host = null
        super.onDetachedFromWindow()
    }

    internal fun resetShader() {
        highlightShader = null
        invalidate()
    }

    private fun findHost(): ShimmerFrameLayout? {
        var p = parent
        while (p != null) {
            if (p is ShimmerFrameLayout) return p
            p = p.parent
        }
        return null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        path.reset()
        when (shape) {
            Shape.RECT -> path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            Shape.OVAL -> path.addOval(rect, Path.Direction.CW)
            Shape.TOP_ROUNDED -> path.addRoundRect(
                rect,
                floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                Path.Direction.CW,
            )
        }
        mask?.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val shimmer = host
        val base = shimmer?.baseColor ?: context.getColor(R.color.shimmer_base_light)
        val shader = shimmer?.let { s ->
            (highlightShader ?: s.createHighlightShader().also { highlightShader = it })
                .takeIf { s.updateHighlight(this, it, shaderMatrix) }
        }

        val maskDrawable = mask
        if (maskDrawable != null) {
            // Khuôn bất kỳ: tô khuôn bằng màu base, rồi phủ dải sáng chỉ lên pixel của khuôn
            val layer = if (shader != null) canvas.saveLayer(rect, null) else -1
            maskDrawable.setTint(base)
            maskDrawable.setTintMode(PorterDuff.Mode.SRC_IN)
            maskDrawable.draw(canvas)
            if (shader != null) {
                maskHighlightPaint.shader = shader
                canvas.drawRect(rect, maskHighlightPaint)
                canvas.restoreToCount(layer)
            }
            return
        }

        basePaint.color = base
        canvas.withSave {
            drawPath(path, basePaint)
            if (shader != null) {
                highlightPaint.shader = shader
                drawPath(path, highlightPaint)
            }
        }
    }
}
