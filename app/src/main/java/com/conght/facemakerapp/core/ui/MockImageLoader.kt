package com.conght.facemakerapp.core.ui

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.signature.ObjectKey
import com.conght.facemakerapp.R
import java.io.File

object MockImageLoader {

    const val MOCK_COLOR_PREFIX = "mock://color/"
    private const val CROSS_FADE_MS = 200

    /**
     * Crossfade thật (placeholder mờ dần về 0). Mặc định Glide giữ placeholder alpha 100 bên dưới
     * → ảnh PNG trong suốt (thumbnail item) bị lộ màu placeholder nâu phía sau.
     */
    private val crossFade = DrawableCrossFadeFactory.Builder(CROSS_FADE_MS).setCrossFadeEnabled(true).build()

    /**
     * `mock://color/#RRGGBB` -> ô màu; còn lại load bằng Glide (URL, hoặc đường dẫn file tuyệt đối)
     * với placeholder màu skeleton ([placeholderColor]) và crossfade 200ms.
     * [forExport] = true: không dùng HARDWARE bitmap để view vẽ được ra Bitmap (export ảnh).
     */
    fun load(
        imageView: ImageView,
        url: String?,
        @ColorRes placeholderColor: Int = R.color.shimmer_base_light,
        forExport: Boolean = false,
    ) {
        if (url != null && url.startsWith(MOCK_COLOR_PREFIX)) {
            Glide.with(imageView).clear(imageView)
            imageView.setImageDrawable(url.removePrefix(MOCK_COLOR_PREFIX).toColorInt().toDrawable())
            return
        }
        val placeholder = ContextCompat.getColor(imageView.context, placeholderColor).toDrawable()
        val file = if (url != null && url.startsWith("/")) File(url) else null
        var request = Glide.with(imageView)
            .load(file ?: url)
            .placeholder(placeholder)
            .transition(DrawableTransitionOptions.withCrossFade(crossFade))
        // File tạm bị ghi đè cùng đường dẫn (vd. character_tmp.png) → thêm signature theo lần sửa
        // để Glide không trả ảnh cũ trong cache.
        if (file != null) request = request.signature(ObjectKey("${file.lastModified()}_${file.length()}"))
        if (forExport) request = request.disallowHardwareConfig()
        request.into(imageView)
    }
}

fun ImageView.loadImage(
    url: String?,
    @ColorRes placeholderColor: Int = R.color.shimmer_base_light,
    forExport: Boolean = false,
) = MockImageLoader.load(this, url, placeholderColor, forExport)
