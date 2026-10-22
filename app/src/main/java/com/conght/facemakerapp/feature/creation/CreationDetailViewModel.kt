package com.conght.facemakerapp.feature.creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface CreationDetailEvent {
    data object Deleted : CreationDetailEvent
    data class Downloaded(val success: Boolean) : CreationDetailEvent
}

@HiltViewModel
class CreationDetailViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val creationId: Long = savedStateHandle[CreationDetailFragment.ARG_CREATION_ID] ?: -1L

    private val _creation = MutableStateFlow<Creation?>(null)
    val creation: StateFlow<Creation?> = _creation.asStateFlow()

    private val _events = Channel<CreationDetailEvent>(Channel.BUFFERED)
    val events: Flow<CreationDetailEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { _creation.value = creationRepository.get(creationId) }
    }

    /** Sau khi Delete dialog xác nhận: xoá bản ghi + file. */
    fun delete() {
        if (creationId < 0) return
        viewModelScope.launch {
            creationRepository.delete(listOf(creationId))
            _events.send(CreationDetailEvent.Deleted)
        }
    }

    /** Copy ảnh vào internal storage `filesDir/downloads/` (không ra Gallery). */
    fun download() {
        if (creationId < 0) return
        viewModelScope.launch {
            val result = creationRepository.download(listOf(creationId))
            _events.send(CreationDetailEvent.Downloaded(result.isSuccess))
        }
    }
}
