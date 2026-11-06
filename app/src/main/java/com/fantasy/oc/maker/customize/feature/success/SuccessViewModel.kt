package com.fantasy.oc.maker.customize.feature.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasy.oc.maker.customize.data.model.Creation
import com.fantasy.oc.maker.customize.data.repository.CreationRepository
import com.fantasy.oc.maker.customize.feature.maker.customize.CustomizeFragment
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
}

@HiltViewModel
class SuccessViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val creationId: Long = savedStateHandle[SuccessFragment.ARG_CREATION_ID] ?: -1L

    val source: SuccessFragment.Source = savedStateHandle.get<String>(SuccessFragment.ARG_SOURCE)
        ?.let { runCatching { SuccessFragment.Source.valueOf(it) }.getOrNull() } ?: SuccessFragment.Source.MAKER

    /** COSPLAY: đề bài vừa chơi, để Re-edit mở lại màn chơi. */
    val cosplayTemplateId: String? = savedStateHandle[CustomizeFragment.ARG_TEMPLATE_ID]
    val cosplayChallengeJson: String? = savedStateHandle[CustomizeFragment.ARG_CHALLENGE_JSON]

    val stars: Int = (savedStateHandle.get<Int>(SuccessFragment.ARG_STARS) ?: 0).coerceIn(0, MAX_STARS)

    private val _creation = MutableStateFlow<Creation?>(null)
    val creation: StateFlow<Creation?> = _creation.asStateFlow()

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    private val _events = Channel<SuccessEvent>(Channel.BUFFERED)
    val events: Flow<SuccessEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { _creation.value = creationRepository.get(creationId) }
    }

    /** Lưu ảnh ra thư viện máy (Gallery). */
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
    }
}
