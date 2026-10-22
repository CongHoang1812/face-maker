package com.conght.facemakerapp.feature.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.RatePrompter
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.repository.CreationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SuccessEvent {
    data object Downloaded : SuccessEvent
    data object DownloadFailed : SuccessEvent

    /** Lần lưu thành công thứ 2 (chưa rate) → hiện Rate us. */
    data object ShowRateUs : SuccessEvent
}

@HiltViewModel
class SuccessViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    private val ratePrompter: RatePrompter,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val creationId: Long = savedStateHandle[SuccessFragment.ARG_CREATION_ID] ?: -1L

    val source: SuccessFragment.Source = savedStateHandle.get<String>(SuccessFragment.ARG_SOURCE)
        ?.let { runCatching { SuccessFragment.Source.valueOf(it) }.getOrNull() } ?: SuccessFragment.Source.MAKER

    val stars: Int = (savedStateHandle.get<Int>(SuccessFragment.ARG_STARS) ?: 0).coerceIn(0, MAX_STARS)

    private val _creation = MutableStateFlow<Creation?>(null)
    val creation: StateFlow<Creation?> = _creation.asStateFlow()

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    private val _events = Channel<SuccessEvent>(Channel.BUFFERED)
    val events: Flow<SuccessEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { _creation.value = creationRepository.get(creationId) }
        // Chỉ kiểm tra 1 lần cho mỗi màn Successful (không lặp lại khi xoay màn)
        if (savedStateHandle.get<Boolean>(KEY_RATE_CHECKED) != true) {
            savedStateHandle[KEY_RATE_CHECKED] = true
            viewModelScope.launch { if (ratePrompter.shouldPromptAfterSave()) _events.send(SuccessEvent.ShowRateUs) }
        }
    }

    /** Copy ảnh vào internal storage `filesDir/downloads/` (không ra Gallery). */
    fun download() {
        if (_downloading.value || creationId < 0) return
        _downloading.value = true
        viewModelScope.launch {
            val result = creationRepository.download(listOf(creationId))
            _downloading.value = false
            _events.send(if (result.isSuccess) SuccessEvent.Downloaded else SuccessEvent.DownloadFailed)
        }
    }

    private companion object {
        const val MAX_STARS = 3
        const val KEY_RATE_CHECKED = "rate_checked"
    }
}
