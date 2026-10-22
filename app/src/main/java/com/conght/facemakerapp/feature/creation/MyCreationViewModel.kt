package com.conght.facemakerapp.feature.creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.repository.CreationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyCreationUiState(
    val tab: CreationType = CreationType.DESIGN,
    /** null = đang tải lần đầu. */
    val items: List<Creation>? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
) {
    val selectedItems: List<Creation> get() = items.orEmpty().filter { it.id in selectedIds }
}

sealed interface MyCreationEvent {
    data class Downloaded(val success: Boolean) : MyCreationEvent
}

/**
 * My creation: đọc dữ liệu thật từ Room ([CreationRepository.observe]). Tab, chế độ chọn và
 * các id đang chọn lưu trong [SavedStateHandle] để không mất khi xoay / recreate.
 */
@HiltViewModel
class MyCreationViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tab = savedStateHandle.getStateFlow(
        MyCreationFragment.ARG_TAB,
        CreationType.DESIGN.name,
    )
    private val selectionMode = savedStateHandle.getStateFlow(KEY_SELECTION_MODE, false)
    private val selectedIds = savedStateHandle.getStateFlow(KEY_SELECTED, LongArray(0))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val items: Flow<List<Creation>?> = tab.flatMapLatest { name ->
        creationRepository.observe(parseType(name)).map<List<Creation>, List<Creation>?> { it }.onStart { emit(null) }
    }

    val state: StateFlow<MyCreationUiState> = combine(tab, items, selectionMode, selectedIds) { tab, items, mode, ids ->
        val existing = items?.map { it.id }?.toSet()
        MyCreationUiState(
            tab = parseType(tab),
            items = items,
            selectionMode = mode,
            // bỏ id đã bị xoá khỏi DB
            selectedIds = ids.filter { existing == null || it in existing }.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MyCreationUiState())

    private val _events = Channel<MyCreationEvent>(Channel.BUFFERED)
    val events: Flow<MyCreationEvent> = _events.receiveAsFlow()

    fun selectTab(type: CreationType) {
        if (state.value.tab == type) return
        exitSelection()
        savedStateHandle[MyCreationFragment.ARG_TAB] = type.name
    }

    /** Long-press: vào chế độ chọn và chọn luôn item đó. */
    fun startSelection(id: Long) {
        savedStateHandle[KEY_SELECTION_MODE] = true
        setSelected(setOf(id))
    }

    fun toggle(id: Long) {
        val ids = state.value.selectedIds
        setSelected(if (id in ids) ids - id else ids + id)
    }

    fun exitSelection() {
        savedStateHandle[KEY_SELECTION_MODE] = false
        setSelected(emptySet())
    }

    /** Ghi nhớ id cần xoá trong lúc Delete dialog đang mở (sống sót recreate). */
    fun requestDelete(ids: Collection<Long>) {
        savedStateHandle[KEY_PENDING_DELETE] = ids.toLongArray()
    }

    /** Delete dialog xác nhận: xoá bản ghi + file trong internal storage. Huỷ: bỏ yêu cầu. */
    fun onDeleteConfirmed(confirmed: Boolean) {
        val ids = savedStateHandle.remove<LongArray>(KEY_PENDING_DELETE)?.toSet().orEmpty()
        if (!confirmed || ids.isEmpty()) return
        viewModelScope.launch {
            creationRepository.delete(ids.toList())
            setSelected(state.value.selectedIds - ids)
        }
    }

    /** Copy ảnh vào internal storage `filesDir/downloads/` (không ra Gallery). */
    fun download(ids: Collection<Long>) {
        if (ids.isEmpty() || _busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = creationRepository.download(ids.toList())
                _events.send(MyCreationEvent.Downloaded(result.isSuccess))
            } finally {
                _busy.value = false
            }
        }
    }

    private val _busy = MutableStateFlow(false)

    /** Đang export nhiều ảnh → hiện LoadingDialog. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private fun setSelected(ids: Set<Long>) {
        savedStateHandle[KEY_SELECTED] = ids.toLongArray()
    }

    private fun parseType(name: String) =
        runCatching { CreationType.valueOf(name) }.getOrDefault(CreationType.DESIGN)

    private companion object {
        const val KEY_SELECTION_MODE = "selection_mode"
        const val KEY_SELECTED = "selected_ids"
        const val KEY_PENDING_DELETE = "pending_delete"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
