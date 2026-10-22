package com.conght.facemakerapp.core.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.conght.facemakerapp.R
import com.google.android.material.snackbar.Snackbar

/**
 * Thông báo ngắn kiểu app (thay Toast hệ thống): thẻ nhỏ nền `brown_primary` bo 16, chữ trắng
 * Londrina, hiện 2s ở đáy màn hình.
 */
fun Fragment.showAppToast(@StringRes message: Int) {
    val anchor = view ?: return
    anchor.showAppToast(anchor.context.getString(message))
}

fun View.showAppToast(message: CharSequence) {
    val snackbar = Snackbar.make(this, message, DURATION_MS)
    val res = resources
    snackbar.view.apply {
        background = ContextCompat.getDrawable(context, R.drawable.bg_app_toast)
        val margin = res.getDimensionPixelSize(R.dimen.app_toast_margin)
        when (val params = layoutParams) {
            is FrameLayout.LayoutParams -> {
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.width = FrameLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(margin, margin, margin, margin)
            }

            is CoordinatorLayout.LayoutParams -> {
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                params.width = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(margin, margin, margin, margin)
            }
        }
        findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.white))
            typeface = ResourcesCompat.getFont(context, R.font.londrina_solid_family)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
    }
    snackbar.show()
}

private const val DURATION_MS = 2_000
