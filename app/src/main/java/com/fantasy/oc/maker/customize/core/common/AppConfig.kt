package com.fantasy.oc.maker.customize.core.common

import com.fantasy.oc.maker.customize.BuildConfig

object AppConfig {
    /** `true` -> dùng MockDataSource để test UI. Đổi trong app/build.gradle.kts. */
    const val USE_MOCK_DATA: Boolean = BuildConfig.USE_MOCK_DATA

    /** Máy chủ chính; đổi trong app/build.gradle.kts. */
    const val BASE_URL: String = BuildConfig.BASE_URL

    /** Máy chủ dự phòng: chỉ gọi khi máy chủ chính lỗi / quá thời gian. */
    const val BACKUP_BASE_URL: String = BuildConfig.BACKUP_BASE_URL

    /**
     * Mã app trên server (endpoint dữ liệu nhân vật + thư mục ảnh).
     * TODO(RELEASE): đang mượn dữ liệu ST247 để test vì chưa có mã của ST237; đổi sang mã thật trước khi phát hành.
     */
    const val APP_KEY: String = "ST247_ChibiDollDressUp"

    /** Thư mục ảnh tính từ base URL. */
    const val ASSET_PATH: String = "public/app/$APP_KEY/"

    const val PRIVACY_URL: String = "https://sites.google.com/view/d-d-character-maker/home"

    // TODO: thay email nhận góp ý thật
    const val FEEDBACK_EMAIL: String = "feedback@example.com"

    /** Lucky roll: yêu cầu xem quảng cáo (Random dialog – Watch Ad stub) trước khi random. */
    const val LUCKY_ROLL_REQUIRES_AD: Boolean = false

    /** Link Play Store của app (dùng cho Share app / Rate). */
    fun playStoreUrl(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"
}
