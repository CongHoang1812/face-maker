package com.conght.facemakerapp.core.ui

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.ShimmerFrameLayout
import java.io.File

/**
 * Tải ảnh có shimmer chờ (com.facebook.shimmer): [shimmer] chạy cho tới khi ảnh hiện ra;
 * không có ảnh / tải lỗi thì giữ shimmer (giống thẻ chờ API ở màn chọn template).
 * [url] là URL hoặc đường dẫn file tuyệt đối (internal storage).
 */
fun ImageView.loadWithShimmer(url: String?, shimmer: ShimmerFrameLayout) {
    val image = this
    fun showShimmer() {
        image.isInvisible = true // giữ chỗ cho view neo theo ảnh
        shimmer.isVisible = true
        shimmer.startShimmer()
    }
    Glide.with(image).clear(image)
    val model: Any? = when {
        url.isNullOrBlank() -> null
        url.startsWith("/") -> File(url).takeIf { it.exists() }
        else -> url
    }
    showShimmer()
    if (model == null) return // chưa có ảnh → giữ shimmer
    Glide.with(image)
        .load(model)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean) =
                false // giữ shimmer

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                shimmer.stopShimmer()
                shimmer.isVisible = false
                image.isVisible = true
                return false
            }
        })
        .into(image)
}
