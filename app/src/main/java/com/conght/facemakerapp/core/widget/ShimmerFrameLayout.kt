package com.conght.facemakerapp.core.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.conght.facemakerapp.R
import kotlin.math.max
import kotlin.math.tan

/**
 * Vùng loading: mọi [SkeletonView] con (kể cả nằm sâu trong RecyclerView) được quét bởi
 * CÙNG một dải sáng chéo (~20°, rộng ~40%), chu kỳ 1200ms + nghỉ 300ms, lặp vô hạn.
 *
 * View thật (vd. ô "None") nằm chung vùng KHÔNG bị ảnh hưởng vì chỉ SkeletonView vẽ dải sáng.
 * Animator tự dừng khi detached / bị ẩn và chạy lại khi hiện ra (nếu đã [startShimmer]).
 *
 * attr: `app:shimmerTone` (dark: trên nền tối | light: trên nền sáng), `app:shimmerAutoStart`.
 */
class ShimmerFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class Tone { DARK, LIGHT }

    @ColorInt
    var baseColor: Int = 0
        private set

    @ColorInt
    private var highlightColor: Int = ContextCompat.getColor(context, R.color.shimmer_highlight)

    var tone: Tone = Tone.DARK
        set(value) {
            field = value
            baseColor = ContextCompat.getColor(
                context,
                if (value == Tone.DARK) R.color.shimmer_base_dark else R.color.shimmer_base_light,
            )
            skeletons.forEach { it.invalidate() }
        }

    /** Vị trí dải sáng 0..1; < 0 = không vẽ dải sáng (đang nghỉ / đã dừng). */
    private var progress = -1f
    private var requested = false
    private val skeletons = LinkedHashSet<SkeletonView>()
    private val location = IntArray(2)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SWEEP_MS + REPEAT_DELAY_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val elapsed = it.animatedFraction * duration
            progress = if (elapsed <= SWEEP_MS) elapsed / SWEEP_MS else -1f
            skeletons.forEach(View::invalidate)
        }
    }

    init {
        var autoStart = true
        context.withStyledAttributes(attrs, R.styleable.ShimmerFrameLayout, defStyleAttr) {
            tone = Tone.entries[getInt(R.styleable.ShimmerFrameLayout_shimmerTone, 0)]
            autoStart = getBoolean(R.styleable.ShimmerFrameLayout_shimmerAutoStart, true)
        }
        requested = autoStart
    }

    fun startShimmer() {
        requested = true
        updateRunning()
    }

    fun stopShimmer() {
        requested = false
        updateRunning()
    }

    val isShimmerStarted: Boolean get() = animator.isStarted

    internal fun register(view: SkeletonView) {
        skeletons += view
    }

    internal fun unregister(view: SkeletonView) {
        skeletons -= view
    }

    /** Tạo shader dải sáng cho một skeleton (mỗi skeleton giữ 1 instance, chỉ cập nhật matrix). */
    internal fun createHighlightShader(): Shader = LinearGradient(
        0f, 0f, bandWidth(), 0f,
        intArrayOf(Color.TRANSPARENT, highlightColor, Color.TRANSPARENT),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP,
    )

    /**
     * Đặt matrix cho [shader] của [view] theo toạ độ của layout này để mọi skeleton thấy
     * cùng một dải sáng. Trả về false nếu hiện không có dải sáng.
     */
    internal fun updateHighlight(view: View, shader: Shader, matrix: Matrix): Boolean {
        if (progress < 0f || width == 0) return false
        getLocationInWindow(location)
        val selfX = location[0]
        val selfY = location[1]
        view.getLocationInWindow(location)
        val offsetX = (location[0] - selfX).toFloat()
        val offsetY = (location[1] - selfY).toFloat()

        val band = bandWidth()
        val slant = height * tan(Math.toRadians(TILT_DEGREES.toDouble())).toFloat()
        val start = -band - slant
        val end = width + band
        val x = start + (end - start) * progress
        matrix.setRotate(TILT_DEGREES)
        matrix.postTranslate(x - offsetX, -offsetY)
        shader.setLocalMatrix(matrix)
        return true
    }

    private fun bandWidth(): Float = max(width, 1) * BAND_RATIO

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Độ rộng dải sáng phụ thuộc width -> các skeleton tạo lại shader
        skeletons.forEach(SkeletonView::resetShader)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateRunning()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        progress = -1f
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateRunning()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateRunning()
    }

    private fun updateRunning() {
        val shouldRun = requested && isAttachedToWindow && isShown
        if (shouldRun && !animator.isStarted) {
            animator.start()
        } else if (!shouldRun && animator.isStarted) {
            animator.cancel()
            progress = -1f
            skeletons.forEach(View::invalidate)
        }
    }

    private companion object {
        const val SWEEP_MS = 1200L
        const val REPEAT_DELAY_MS = 300L
        const val TILT_DEGREES = 20f
        const val BAND_RATIO = 0.4f
    }
}
