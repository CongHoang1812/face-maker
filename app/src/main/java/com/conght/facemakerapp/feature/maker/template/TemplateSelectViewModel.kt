package com.conght.facemakerapp.feature.maker.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.retryWhenBackOnline
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.data.repository.MakerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TemplateSelectViewModel @Inject constructor(
    private val makerRepository: MakerRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    /** Template vừa bấm lúc mất mạng: Try again có mạng thì mở tiếp (sống qua xoay màn). */
    var pendingTemplate: CharacterTemplate? = null

    /** Mỗi lần tăng = tải lại danh sách (Retry). */
    private val reloadKey = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<Resource<List<CharacterTemplate>>> = reloadKey
        .flatMapLatest { makerRepository.templates() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Resource.Loading)

    init {
        retryWhenBackOnline(networkMonitor, isError = { state.value is Resource.Error }, retry = ::retry)
    }

    fun retry() {
        reloadKey.value++
    }

    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
