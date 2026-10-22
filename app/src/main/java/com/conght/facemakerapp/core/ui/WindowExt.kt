package com.conght.facemakerapp.core.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Cộng chiều cao status bar thật vào paddingTop (giữ padding gốc). Nền vẫn vẽ dưới status bar,
 * nội dung đặt theo toạ độ "y Figma - 44dp" (xem dimens.xml).
 */
fun View.applyStatusBarPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(top = initialTop + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/** `true` = icon status bar màu tối (nền sáng), `false` = icon sáng (nền tối). */
fun Fragment.setLightStatusBar(light: Boolean) {
    val window = requireActivity().window
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = light
}
