package com.conght.facemakerapp.feature.decor.editor

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.loadImage

/**
 * Nội dung một phần tử trên canvas: sticker (ảnh), speech (ảnh bong bóng + chữ) hoặc text.
 * Vị trí / scale / xoay / lật do [DecorCanvasView] áp qua translation / scale / rotation.
 */
@SuppressLint("ViewConstructor") // chỉ tạo bằng code từ DecorCanvasView
class DecorElementView(context: Context, val type: ElementType) : FrameLayout(context) {

    private val image: ImageView? = if (type == ElementType.TEXT) {
        null
    } else {
        ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            .also { addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)) }
    }

    private val text: TextView? = if (type == ElementType.STICKER) {
        null
    } else {
        TextView(context).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            val size = if (type == ElementType.SPEECH) R.dimen.decor_speech_text else R.dimen.decor_text_size
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(size))
            // Viền mờ nâu để chữ trắng vẫn đọc được trên nền trắng
            setShadowLayer(
                resources.getDimension(R.dimen.decor_text_shadow), 0f, 0f,
                ContextCompat.getColor(context, R.color.brown_dark),
            )
        }.also {
            val params = if (type == ElementType.SPEECH) {
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    val pad = resources.getDimensionPixelSize(R.dimen.decor_speech_text_padding)
                    setMargins(pad, pad, pad, pad)
                }
            } else {
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
            addView(it, params)
        }
    }

    private var bound: DecorElement? = null

    fun bind(element: DecorElement) {
        val old = bound
        bound = element
        if (image != null && old?.imageUrl != element.imageUrl) {
            image.loadImage(element.imageUrl, R.color.transparent, forExport = true)
        }
        if (text != null) {
            if (old?.text != element.text) text.text = element.text
            if (old?.fontId != element.fontId || old == null) {
                text.typeface = ResourcesCompat.getFont(context, DecorFonts.of(element.fontId).fontRes)
            }
            element.color?.let(text::setTextColor)
        }
    }

    companion object {
        /** Kích thước gốc (chưa scale) theo loại; TEXT = wrap_content. */
        fun layoutParamsFor(context: Context, type: ElementType): LayoutParams {
            val res = context.resources
            return when (type) {
                ElementType.STICKER -> LayoutParams(
                    res.getDimensionPixelSize(R.dimen.decor_sticker_w),
                    res.getDimensionPixelSize(R.dimen.decor_sticker_h),
                    Gravity.CENTER,
                )

                ElementType.SPEECH -> LayoutParams(
                    res.getDimensionPixelSize(R.dimen.decor_speech_w),
                    res.getDimensionPixelSize(R.dimen.decor_speech_h),
                    Gravity.CENTER,
                )

                ElementType.TEXT -> LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
        }
    }
}
