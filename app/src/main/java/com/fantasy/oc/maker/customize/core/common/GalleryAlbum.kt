package com.fantasy.oc.maker.customize.core.common

import android.content.Context
import com.fantasy.oc.maker.customize.R

/**
 * Tên album Gallery (`Pictures/{tên}`) mà nút Download ghi vào, cũng là tên hiện trong toast.
 * Tên app có thể chứa ký tự không hợp lệ cho đường dẫn (vd `&`) nên chỉ giữ chữ, số, khoảng trắng.
 */
fun Context.galleryAlbumName(): String =
    getString(R.string.app_name).replace(Regex("[^\\p{L}\\p{N} ]"), "").trim()
