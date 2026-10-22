package com.conght.facemakerapp.feature.decor.editor

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.ColorInt
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.DecorRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface DecorEditorEvent {
    data class Saved(val creationId: Long) : DecorEditorEvent
    data object SaveFailed : DecorEditorEvent
    data object ImportFailed : DecorEditorEvent
}

/**
 * Editor trang trí (Background / Sticker / Speech / Text). Toàn bộ [EditorData] lưu JSON trong
 * [SavedStateHandle] để không mất khi xoay / recreate / process death.
 * Pack background / sticker / speech lấy từ [DecorRepository] (chưa có API → luôn Loading).
 */
@HiltViewModel
class DecorEditorViewModel @Inject constructor(
    decorRepository: DecorRepository,
    private val creationRepository: CreationRepository,
    private val gson: Gson,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val characterPath: String? = savedStateHandle[DecorEditorFragment.ARG_CHARACTER_PATH]

    private val _data = MutableStateFlow(
        savedStateHandle.get<String>(KEY_DATA)?.let { runCatching { gson.fromJson(it, EditorData::class.java) }.getOrNull() }
            ?: EditorData(),
    )
    val data: StateFlow<EditorData> = _data.asStateFlow()

    /** Pack theo loại (BACKGROUND / STICKER / SPEECH). */
    val packs: Map<DecorType, StateFlow<Resource<List<DecorPack>>>> = DecorType.entries.associateWith { type ->
        decorRepository.packs(type).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), Resource.Loading)
    }

    private val _events = Channel<DecorEditorEvent>(Channel.BUFFERED)
    val events: Flow<DecorEditorEvent> = _events.receiveAsFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val current get() = _data.value

    // ---------- Tab ----------

    fun selectMainTab(tab: EditorTab) = update(dirty = false) { it.copy(mainTab = tab) }

    fun selectSubTab(index: Int) = update(dirty = false) { it.copy(subTabs = it.subTabs + (it.mainTab to index)) }

    // ---------- Background ----------

    fun setBackgroundNone() = update { it.copy(background = DecorBackground()) }

    fun setBackgroundColor(@ColorInt color: Int) = update { it.copy(background = DecorBackground(color = color)) }

    fun setBackgroundImage(path: String) = update { it.copy(background = DecorBackground(imagePath = path)) }

    /** Ảnh từ Photo Picker: copy vào internal storage, thêm vào "gần đây" và đặt làm nền. */
    fun importBackground(uri: Uri) {
        viewModelScope.launch {
            try {
                val path = creationRepository.importImage(uri)
                update {
                    it.copy(
                        background = DecorBackground(imagePath = path),
                        recentImages = (listOf(path) + it.recentImages).distinct().take(MAX_RECENTS),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Import ảnh lỗi", e)
                _events.send(DecorEditorEvent.ImportFailed)
            }
        }
    }

    // ---------- Phần tử ----------

    fun addSticker(url: String) = addElement(DecorElement(newId(), ElementType.STICKER, imageUrl = url))

    fun addSpeech(url: String, defaultText: String) = addElement(
        DecorElement(newId(), ElementType.SPEECH, imageUrl = url, text = defaultText, fontId = current.fontId, color = current.textColor),
    )

    /** ✓ ở tab Text: cập nhật text của phần tử chữ đang chọn, hoặc thêm text mới. */
    fun submitText(text: String) {
        val value = text.trim()
        if (value.isEmpty()) return
        val selected = current.selected
        if (selected != null && selected.type != ElementType.STICKER) {
            updateElement(selected.id) { it.copy(text = value) }
        } else {
            addElement(DecorElement(newId(), ElementType.TEXT, text = value, fontId = current.fontId, color = current.textColor))
        }
    }

    private fun addElement(element: DecorElement) = update {
        it.copy(elements = it.elements + element, selectedId = element.id)
    }

    fun select(id: String?) = update(dirty = false) { it.copy(selectedId = id) }

    /**
     * Ô None ở lưới Sticker / Speech: xoá phần tử cùng loại đang chọn (nếu có), ngược lại bỏ chọn.
     */
    fun noneFor(type: ElementType) {
        val selected = current.selected
        if (selected != null && selected.type == type) delete(selected.id) else select(null)
    }

    fun transform(id: String, x: Float, y: Float, scale: Float, rotation: Float) = updateElement(id) {
        it.copy(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            scale = scale.coerceIn(MIN_SCALE, MAX_SCALE),
            rotation = rotation % FULL_TURN,
        )
    }

    fun flip(id: String) = updateElement(id) { it.copy(flipped = !it.flipped) }

    fun delete(id: String) = update {
        it.copy(elements = it.elements.filterNot { e -> e.id == id }, selectedId = it.selectedId.takeIf { s -> s != id })
    }

    /** Đổi font: áp cho phần tử chữ đang chọn và làm mặc định cho chữ mới. */
    fun setFont(fontId: String) {
        update { it.copy(fontId = fontId) }
        current.selected?.takeIf { it.type != ElementType.STICKER }?.let { updateElement(it.id) { e -> e.copy(fontId = fontId) } }
    }

    fun setTextColor(@ColorInt color: Int) {
        update { it.copy(textColor = color) }
        current.selected?.takeIf { it.type != ElementType.STICKER }?.let { updateElement(it.id) { e -> e.copy(color = color) } }
    }

    // ---------- Lưu ----------

    /** Download: [bitmap] đã vẽ từ canvas (không viền chọn) → My creation (DESIGN, internal storage). */
    fun save(bitmap: Bitmap) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val id = creationRepository.save(CreationType.DESIGN, bitmap, null)
                update(dirty = false) { it.copy(isDirty = false) }
                _events.send(DecorEditorEvent.Saved(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lưu design lỗi", e)
                _events.send(DecorEditorEvent.SaveFailed)
            } finally {
                _saving.value = false
            }
        }
    }

    private fun updateElement(id: String, block: (DecorElement) -> DecorElement) = update {
        it.copy(elements = it.elements.map { e -> if (e.id == id) block(e) else e })
    }

    /** [dirty] = thay đổi nội dung thiết kế (tab / chọn phần tử không tính). */
    private fun update(dirty: Boolean = true, block: (EditorData) -> EditorData) {
        val old = current
        var new = block(old)
        if (new == old) return
        if (dirty) new = new.copy(isDirty = true)
        _data.value = new
        savedStateHandle[KEY_DATA] = gson.toJson(new)
    }

    private fun newId() = UUID.randomUUID().toString()

    private companion object {
        const val TAG = "DecorEditorViewModel"
        const val KEY_DATA = "editor_data"
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_RECENTS = 18
        const val MIN_SCALE = 0.3f
        const val MAX_SCALE = 4f
        const val FULL_TURN = 360f
    }
}
