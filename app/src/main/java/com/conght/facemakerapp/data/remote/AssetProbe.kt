package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kiểm tra máy chủ có trả file ảnh thật không. Đã gặp: JSON API vẫn 200 nhưng server (Cloudflare)
 * chặn file ảnh ở một số mạng bằng `302 → /404.html` → UI chỉ còn ô trống.
 */
fun interface AssetProbe {
    /** false = server không trả ảnh; lỗi mạng ném IOException như bình thường. */
    suspend fun isAvailable(url: String): Boolean
}

/** Ảnh trên mọi máy chủ đều bị chặn (không phải lỗi mạng của máy). */
class AssetUnavailableException(url: String) : Exception("Máy chủ không trả ảnh: $url")

@Singleton
class OkHttpAssetProbe @Inject constructor(
    client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : AssetProbe {

    private val client = client.newBuilder()
        .apply { interceptors().clear() }
        .followRedirects(false) // 302 → /404.html coi là không có ảnh
        .build()

    override suspend fun isAvailable(url: String): Boolean = withContext(dispatchers.io) {
        client.newCall(Request.Builder().url(url).head().build()).execute().use { it.isSuccessful }
    }
}
