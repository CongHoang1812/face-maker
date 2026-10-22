package com.conght.facemakerapp.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Quyền app dùng (xem 01_ANALYSIS.md mục 5). App chỉ ĐỌC ảnh (chọn ảnh nền),
 * mọi ảnh lưu vào internal storage nên không cần quyền ghi.
 */
object AppPermissions {

    /** Quyền đọc ảnh theo API level (API 34+ chấp nhận cả quyền chọn một phần ảnh). */
    val media: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** Quyền thông báo, `null` khi API < 33 (không cần xin). */
    val notification: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun isMediaGranted(context: Context): Boolean = media.any { isGranted(context, it) }

    fun isNotificationGranted(context: Context): Boolean = notification?.let { isGranted(context, it) } ?: true

    private fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
