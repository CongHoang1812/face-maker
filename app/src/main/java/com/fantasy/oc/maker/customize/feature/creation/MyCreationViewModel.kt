package com.fantasy.oc.maker.customize.feature.creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fantasy.oc.maker.customize.data.model.Creation
import com.fantasy.oc.maker.customize.data.model.CreationType
import com.fantasy.oc.maker.customize.data.model.PACK_NAME_MAX_LENGTH
import com.fantasy.oc.maker.customize.data.model.StickerExport
import com.fantasy.oc.maker.customize.data.model.StickerTarget
import com.fantasy.oc.maker.customize.data.model.WHATSAPP_PACK_MAX_STICKERS
import com.fantasy.oc.maker.customize.data.model.WHATSAPP_PACK_MIN_STICKERS
import com.fantasy.oc.maker.customize.data.repository.CreationRepository
import com.fantasy.oc.maker.customize.data.repository.StickerRepository
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
    val tab: CreationType = CreationType.CHARACTER,
    /** null = đang tải lần đầu. */
    val items: List<Creation>? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
) {
    val selectedItems: List<Creation> get() = items.orEmpty().filter { it.id in selectedIds }
}

sealed interface MyCreationEvent {
    data class Downloaded(val success: Boolean) : MyCreationEvent
    data object InvalidWhatsAppStickerCount : MyCreationEvent
    data class StickersReady(val export: StickerExport) : MyCreationEvent

    /** Hỏi tên gói sticker trước khi thêm vào WhatsApp. */
    data object RequestPackName : MyCreationEvent
    data object StickerExportFailed : MyCreationEvent
}

/**
 * My creation: đọc dữ liệu thật từ Room ([CreationRepository.observe]). Tab, chế độ chọn và
 * các id đang chọn lưu trong [SavedStateHandle] để không mất khi xoay / recreate.
 */
@HiltViewModel
class MyCreationViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    private val stickerRepository: StickerRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tab = savedStateHandle.getStateFlow(KEY_TAB, CreationType.CHARACTER.name)
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
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        // Giá trị đầu đã đúng tab được truyền vào, tránh khung hình đầu hiện tab Character rồi mới nhảy
        MyCreationUiState(tab = parseType(tab.value)),
    )

    private val _events = Channel<MyCreationEvent>(Channel.BUFFERED)
    val events: Flow<MyCreationEvent> = _events.receiveAsFlow()

    fun selectTab(type: CreationType) {
        exitSelection()
        if (state.value.tab == type) return
        savedStateHandle[KEY_TAB] = type.name
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

    /** Lưu ảnh ra thư viện máy (Gallery). */
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

    /**
     * Tạo sticker từ các nhân vật đang chọn; WhatsApp giới hạn số sticker trong một gói.
     * WhatsApp cần tên gói: đủ điều kiện thì hỏi tên trước ([MyCreationEvent.RequestPackName]),
     * người dùng nhập xong gọi [exportToWhatsApp].
     */
    fun exportStickers(target: StickerTarget) {
        val paths = selectedPaths(target) ?: return
        if (target == StickerTarget.WHATSAPP) {
            _events.trySend(MyCreationEvent.RequestPackName)
            return
        }
        export(target, paths, packName = null)
    }

    /** Sau dialog Create Package Name. Tên rỗng thì bỏ qua (dialog không cho bấm Yes). */
    fun exportToWhatsApp(packName: String) {
        val name = packName.trim().take(PACK_NAME_MAX_LENGTH)
        if (name.isEmpty()) return
        val paths = selectedPaths(StickerTarget.WHATSAPP) ?: return
        export(StickerTarget.WHATSAPP, paths, name)
    }

    /** `null` = không có gì để xuất hoặc sai số lượng (đã báo toast), không hiện dialog Saving. */
    private fun selectedPaths(target: StickerTarget): List<String>? {
        val paths = state.value.selectedItems.map { it.imagePath }
        if (paths.isEmpty() || _busy.value) return null
        val countValid = target != StickerTarget.WHATSAPP ||
            paths.size in WHATSAPP_PACK_MIN_STICKERS..WHATSAPP_PACK_MAX_STICKERS
        if (!countValid) {
            _events.trySend(MyCreationEvent.InvalidWhatsAppStickerCount)
            return null
        }
        return paths
    }

    private fun export(target: StickerTarget, paths: List<String>, packName: String?) {
        _busy.value = true
        viewModelScope.launch {
            try {
                val event = stickerRepository.export(target, paths, packName).fold(
                    onSuccess = { MyCreationEvent.StickersReady(it) },
                    onFailure = { MyCreationEvent.StickerExportFailed },
                )
                _events.send(event)
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
        runCatching { CreationType.valueOf(name) }.getOrDefault(CreationType.CHARACTER)

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_SELECTION_MODE = "selection_mode"
        const val KEY_SELECTED = "selected_ids"
        const val KEY_PENDING_DELETE = "pending_delete"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
