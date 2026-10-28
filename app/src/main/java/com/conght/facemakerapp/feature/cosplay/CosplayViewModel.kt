package com.conght.facemakerapp.feature.cosplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.retryWhenBackOnline
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.repository.CosplayRepository
import com.conght.facemakerapp.data.repository.SettingsRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Màn bắt đầu Cosplay: đề bài ngẫu nhiên từ dữ liệu API (Loading → shimmer). */
@HiltViewModel
class CosplayViewModel @Inject constructor(
    private val cosplayRepository: CosplayRepository,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val reloadKey = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val challenge: StateFlow<Resource<CosplayChallenge>> = reloadKey
        .flatMapLatest { cosplayRepository.randomChallenge() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Resource.Loading)

    init {
        cosplayRepository.prepare()
        retryWhenBackOnline(networkMonitor, isError = { challenge.value is Resource.Error }, retry = ::random)
    }

    /** Random: xin đề mới (UI quay về shimmer tới khi ghép xong ảnh tham chiếu). */
    fun random() {
        reloadKey.value++
    }

    fun challengeJson(challenge: CosplayChallenge): String = gson.toJson(challenge)

    /** Instruction tự hiện lần đầu vào Cosplay: trả true đúng một lần rồi lưu cờ. */
    suspend fun shouldShowInstructionOnce(): Boolean {
        if (settingsRepository.cosplayInstructionShown.first()) return false
        settingsRepository.setCosplayInstructionShown()
        return true
    }

    fun markInstructionSeen() {
        viewModelScope.launch { settingsRepository.setCosplayInstructionShown() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
