package com.conght.facemakerapp.core.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.conght.facemakerapp.R

/**
 * Nút tròn nền gỗ (viền nâu + lõi cam) chứa glyph ở giữa.
 * attr: `app:icon` (glyph). Kích thước nút đặt bằng layout (36dp hoặc 40dp);
 * glyph tự co 16dp (nút < 40dp) hoặc 20dp.
 */
class RoundIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_round_icon)
        scaleType = ScaleType.FIT_CENTER
        context.withStyledAttributes(attrs, R.styleable.RoundIconButton, defStyleAttr) {
            getDrawable(R.styleable.RoundIconButton_icon)?.let(::setImageDrawable)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val large = w >= resources.getDimensionPixelSize(R.dimen.icon_button_large)
        val glyph = resources.getDimensionPixelSize(
            if (large) R.dimen.icon_button_glyph_large else R.dimen.icon_button_glyph,
        )
        val horizontal = (w - glyph) / 2
        val vertical = (h - glyph) / 2
        // Lõi nút lệch lên 1dp (viền dưới dày hơn) -> glyph cũng lệch lên
        val shift = resources.getDimensionPixelSize(R.dimen.stroke_thin)
        setPadding(horizontal, vertical - shift, horizontal, vertical + shift)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val target = if (isPressed) PRESSED_SCALE else 1f
        if (scaleX != target) {
            animate().scaleX(target).scaleY(target).setDuration(PRESS_ANIM_MS).start()
        }
    }

    private companion object {
        const val PRESSED_SCALE = 0.95f
        const val PRESS_ANIM_MS = 100L
    }
}
