package com.conght.facemakerapp.core.ui

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Glide tải ảnh qua OkHttp của app (chung connection pool, HTTP/2) thay vì HttpURLConnection mặc định:
 * timeout mặc định của Glide chỉ 2,5s nên ảnh layer / avatar (~600KB) hay bị bỏ trống khi mạng chậm.
 */
@GlideModule
class FaceMakerGlideModule : AppGlideModule() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GlideEntryPoint {
        fun okHttpClient(): OkHttpClient
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val apiClient = EntryPointAccessors.fromApplication(context, GlideEntryPoint::class.java).okHttpClient()
        val imageClient = apiClient.newBuilder()
            .apply { interceptors().clear() } // không log header từng ảnh
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(imageClient))
    }

    override fun isManifestParsingEnabled(): Boolean = false

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 15L
        const val READ_TIMEOUT_SECONDS = 30L
    }
}
