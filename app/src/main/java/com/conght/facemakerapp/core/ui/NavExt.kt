package com.conght.facemakerapp.core.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

/**
 * Điều hướng chỉ khi [actionId] thuộc màn đang hiển thị — chặn mở 2 màn khi bấm nhanh
 * vào 2 nút khác nhau (đã rời màn thì action không còn hợp lệ).
 */
fun Fragment.navigateSafe(@IdRes actionId: Int, args: Bundle? = null) {
    val nav = findNavController()
    if (nav.currentDestination?.getAction(actionId) != null) {
        nav.navigate(actionId, args)
    }
}
