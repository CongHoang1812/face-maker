package com.conght.facemakerapp.core.ui

import android.view.View
import androidx.core.view.isVisible
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.widget.ErrorStateView
import com.conght.facemakerapp.core.widget.ShimmerFrameLayout

/**
 * Áp [resource] lên UI:
 * - Loading → hiện [shimmer] + chạy dải sáng, ẩn [content] và [error].
 * - Success → dừng & ẩn shimmer, hiện content, gọi [onSuccess].
 * - Error → dừng & ẩn shimmer, ẩn content, hiện [error] (thông báo theo loại lỗi + Retry).
 */
fun <T> bindResource(
    resource: Resource<T>,
    shimmer: ShimmerFrameLayout,
    content: View,
    error: ErrorStateView?,
    onSuccess: (T) -> Unit,
) {
    when (resource) {
        Resource.Loading -> {
            content.isVisible = false
            error?.isVisible = false
            shimmer.isVisible = true
            shimmer.startShimmer()
        }

        is Resource.Success -> {
            shimmer.stopShimmer()
            shimmer.isVisible = false
            error?.isVisible = false
            content.isVisible = true
            onSuccess(resource.data)
        }

        is Resource.Error -> {
            shimmer.stopShimmer()
            shimmer.isVisible = false
            content.isVisible = false
            error?.setError(resource.throwable)
            error?.isVisible = true
        }
    }
}
