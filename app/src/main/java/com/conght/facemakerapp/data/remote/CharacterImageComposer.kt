package com.conght.facemakerapp.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide
import com.conght.facemakerapp.core.common.DispatcherProvider
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.model.layerUrlFor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Ghép nhân vật thành 1 ảnh, trả đường dẫn tuyệt đối (UI load được bằng `loadImage`). */
fun interface CharacterImageComposer {
    /** [prefix] phân loại file; file cũ cùng prefix bị xoá để cache không phình. */
    suspend fun compose(detail: TemplateDetail, config: CharacterConfig, prefix: String): String
}

/**
 * Ghép các layer của [CharacterConfig] thành 1 ảnh PNG trong cacheDir (API không trả ảnh nhân vật
 * hoàn chỉnh) – dùng cho ảnh tham chiếu Cosplay và kết quả Lucky roll.
 */
@Singleton
class GlideCharacterImageComposer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : CharacterImageComposer {

    override suspend fun compose(detail: TemplateDetail, config: CharacterConfig, prefix: String): String =
        withContext(dispatchers.io) {
            val urls = detail.categories.sortedBy { it.zIndex }.mapNotNull { it.layerUrlFor(config.selections[it.id]) }
            check(urls.isNotEmpty()) { "Nhân vật không có layer nào" }
            var result: Bitmap? = null
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            urls.forEach { url ->
                val target = Glide.with(context).asBitmap().load(url).submit(MAX_SIZE, MAX_SIZE)
                try {
                    val layer = runCatching { target.get() }.getOrElse { throw IOException("Không tải được layer $url", it) }
                    val canvasBitmap = result ?: createBitmap(layer.width, layer.height).also { result = it }
                    Canvas(canvasBitmap).drawBitmap(layer, null, Rect(0, 0, canvasBitmap.width, canvasBitmap.height), paint)
                } finally {
                    Glide.with(context).clear(target)
                }
            }
            save(checkNotNull(result), prefix)
        }

    private fun save(bitmap: Bitmap, prefix: String): String {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles { file -> file.name.startsWith("${prefix}_") }?.forEach(File::delete)
        // Tên mới mỗi lần để Glide không hiện ảnh cũ từ cache
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        bitmap.recycle()
        return file.absolutePath
    }

    private companion object {
        const val DIR = "generated"
        const val MAX_SIZE = 1024
        const val PNG_QUALITY = 100
    }
}
