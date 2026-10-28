package com.conght.facemakerapp.core.ui

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority

/**
 * Tải trước ảnh vào disk cache của Glide (chỉ file, không decode bitmap → không tốn RAM) để lúc
 * hiện lên ImageView lấy từ đĩa thay vì mạng. Ưu tiên thấp: ảnh đang hiện trên màn hình vẫn được tải trước.
 * Chỉ nhận URL http(s) (bỏ `mock://`, đường dẫn file); mỗi URL chỉ gửi một lần trong phiên.
 */
object ImagePrefetcher {

    private val requested = HashSet<String>()

    fun prefetch(context: Context, urls: Iterable<String?>) {
        val glide = Glide.with(context.applicationContext)
        urls.forEach { url ->
            if (url == null || !url.startsWith("http") || !requested.add(url)) return@forEach
            glide.downloadOnly().load(url).priority(Priority.LOW).submit()
        }
    }
}
