package com.conght.facemakerapp.core.common

import com.conght.facemakerapp.BuildConfig

object AppConfig {
    /** `true` -> dùng MockDataSource để test UI. Đổi trong app/build.gradle.kts. */
    const val USE_MOCK_DATA: Boolean = BuildConfig.USE_MOCK_DATA

    /** Máy chủ chính; đổi trong app/build.gradle.kts. */
    const val BASE_URL: String = BuildConfig.BASE_URL

    /** Máy chủ dự phòng: chỉ gọi khi máy chủ chính lỗi / quá thời gian. */
    const val BACKUP_BASE_URL: String = BuildConfig.BACKUP_BASE_URL

    /** Mã app trên server (endpoint dữ liệu nhân vật + thư mục ảnh). */
    const val APP_KEY: String = "ST279_AnimeOCMaker"

    /** Thư mục ảnh tính từ base URL. */
    const val ASSET_PATH: String = "public/app/$APP_KEY/"

    // TODO: thay URL chính sách bảo mật thật
    const val PRIVACY_URL: String = "https://example.com/privacy"

    // TODO: thay email nhận góp ý thật
    const val FEEDBACK_EMAIL: String = "feedback@example.com"

    /** Lucky roll: yêu cầu xem quảng cáo (Random dialog – Watch Ad stub) trước khi random. */
    const val LUCKY_ROLL_REQUIRES_AD: Boolean = false

    /**
     * Bản debug: chèn 1 lần vài item mẫu (chưa có ảnh → ô ảnh shimmer) vào My creation để xem giao diện.
     * Bản release luôn tắt.
     */
    val SEED_SAMPLE_CREATIONS: Boolean = BuildConfig.DEBUG

    /** Link Play Store của app (dùng cho Share app / Rate). */
    fun playStoreUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"
}
