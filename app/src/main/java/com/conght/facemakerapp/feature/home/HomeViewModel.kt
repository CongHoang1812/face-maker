package com.conght.facemakerapp.feature.home

import androidx.lifecycle.ViewModel
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.RatePrompter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ratePrompter: RatePrompter,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Maker / Cosplay / Lucky roll lấy dữ liệu từ API (không lưu offline) → cần mạng mới vào được. */
    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    /** Back ở Home lần đầu (chưa rate): hiện Rate us trước khi thoát. */
    suspend fun shouldPromptRateOnExit(): Boolean = ratePrompter.shouldPromptOnHomeExit()
}
