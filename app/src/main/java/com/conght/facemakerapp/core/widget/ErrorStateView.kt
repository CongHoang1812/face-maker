package com.conght.facemakerapp.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.ViewErrorStateBinding
import java.io.IOException

/**
 * Trạng thái lỗi (thay shimmer khi `Resource.Error`; màn thiếu trong Figma, tự thiết kế theo
 * design system): icon + thông báo + WoodButton Retry. Lỗi mạng ([IOException]) hiện
 * "No internet connection", còn lại "Something went wrong". Co theo vùng chứa.
 */
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewErrorStateBinding

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this)
    }

    fun setError(throwable: Throwable?) {
        binding.tvErrorMessage.setText(
            if (throwable.isNetworkError()) R.string.error_no_internet else R.string.error_generic,
        )
    }

    fun setOnRetryClickListener(action: () -> Unit) {
        binding.btnRetry.setOnSingleClickListener { action() }
    }

    private fun Throwable?.isNetworkError(): Boolean {
        var cause = this
        while (cause != null) {
            if (cause is IOException) return true
            cause = cause.cause
        }
        return false
    }
}
