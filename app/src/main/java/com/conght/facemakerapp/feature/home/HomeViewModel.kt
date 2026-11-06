package com.conght.facemakerapp.feature.home

import androidx.lifecycle.ViewModel
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Maker / Cosplay / Lucky roll lấy dữ liệu từ API (không lưu offline) → cần mạng mới vào được. */
    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    /**
     * Gọi khi bấm Back ở Home. Trả về true nếu cần hiện dialog Rate, false nếu thoát app luôn.
     * Đếm lưu qua các lần mở app: lần 1 thoát, lần 2 hiện Rate, lần 3 thoát, lần 4 hiện Rate...
     * Đã rate rồi thì luôn thoát.
     */
    suspend fun shouldShowRateOnBack(): Boolean {
        if (settingsRepository.appRated.first()) return false
        return settingsRepository.incrementHomeBackCount() % 2 == 0
    }

    /** Suspend để Home chờ ghi xong rồi mới đóng activity. */
    suspend fun markAppRated() = settingsRepository.setAppRated()
}
