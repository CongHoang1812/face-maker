package com.conght.facemakerapp.di

import com.conght.facemakerapp.BuildConfig
import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.data.remote.ApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** [ApiService] trỏ tới máy chủ chính ([AppConfig.BASE_URL]). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryApi

/** [ApiService] trỏ tới máy chủ dự phòng ([AppConfig.BACKUP_BASE_URL]). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackupApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Theo tài liệu API: quá 6 giây thì chuyển sang máy chủ dự phòng. */
    private const val TIMEOUT_SECONDS = 6L

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // BODY sẽ in cả JSON dữ liệu nhân vật (rất lớn) → chỉ log header
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
            },
        )
        .build()

    @Provides
    @Singleton
    @PrimaryApi
    fun providePrimaryApiService(client: OkHttpClient, gson: Gson): ApiService =
        createApiService(AppConfig.BASE_URL, client, gson)

    @Provides
    @Singleton
    @BackupApi
    fun provideBackupApiService(client: OkHttpClient, gson: Gson): ApiService =
        createApiService(AppConfig.BACKUP_BASE_URL, client, gson)

    private fun createApiService(baseUrl: String, client: OkHttpClient, gson: Gson): ApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)
}
