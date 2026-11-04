package com.conght.facemakerapp.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation
import com.conght.facemakerapp.R

/**
 * Chữ có viền ngoài và hai bóng cứng đối xứng (trái-dưới, phải-dưới) như text stroke "Outside" của Figma.
 * attr: `app:textOutlineColor`, `app:textOutlineWidth`, `app:textOutlineShadowColor`, `app:textOutlineShadowOffset`.
 *
 * Viền và bóng vẽ tràn ra ngoài view: view cha cần `android:clipChildren="false"`.
 */
class OutlinedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    @ColorInt
    private var outlineColor = 0
    private var outlineWidth = 0f

    @ColorInt
    private var shadowColor = 0
    private var shadowOffset = 0f

    init {
        context.withStyledAttributes(attrs, R.styleable.OutlinedTextView, defStyleAttr) {
            outlineColor = getColor(R.styleable.OutlinedTextView_textOutlineColor, 0)
            outlineWidth = getDimension(R.styleable.OutlinedTextView_textOutlineWidth, 0f)
            shadowColor = getColor(R.styleable.OutlinedTextView_textOutlineShadowColor, 0)
            shadowOffset = getDimension(R.styleable.OutlinedTextView_textOutlineShadowOffset, 0f)
        }
        // Bóng của textAppearance sẽ đè lên viền ở lượt vẽ fill, nên bỏ và tự vẽ bóng bên dưới viền
        setShadowLayer(0f, 0f, 0f, 0)
    }

    override fun onDraw(canvas: Canvas) {
        val textLayout = layout
        if (textLayout != null && outlineWidth > 0f) {
            val p = paint
            val oldColor = p.color
            p.style = Paint.Style.FILL_AND_STROKE
            p.strokeJoin = Paint.Join.MITER
            // Nét vẽ nằm giữa đường bao chữ: nửa trong bị fill che, nên gấp đôi để phần ngoài đúng outlineWidth
            p.strokeWidth = outlineWidth * 2
            val available = height - extendedPaddingTop - extendedPaddingBottom
            val vOffset = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (available - textLayout.height) / 2f
            } else {
                0f
            }
            canvas.withTranslation(compoundPaddingLeft.toFloat() - scrollX, extendedPaddingTop + vOffset) {
                if (shadowOffset > 0f) {
                    p.color = shadowColor
                    withTranslation(shadowOffset, shadowOffset) { textLayout.draw(this) }
                    withTranslation(-shadowOffset, shadowOffset) { textLayout.draw(this) }
                }
                p.color = outlineColor
                textLayout.draw(this)
            }
            p.style = Paint.Style.FILL
            p.color = oldColor
        }
        super.onDraw(canvas)
    }
}
