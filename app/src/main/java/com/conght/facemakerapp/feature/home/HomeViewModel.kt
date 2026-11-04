package com.conght.facemakerapp.feature.home

import androidx.lifecycle.ViewModel
import com.conght.facemakerapp.core.common.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Maker / Cosplay / Lucky roll lấy dữ liệu từ API (không lưu offline) → cần mạng mới vào được. */
    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()
}
