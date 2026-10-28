package com.conght.facemakerapp.feature.maker.customize

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.facemakerapp.core.common.CosplayScorer
import com.conght.facemakerapp.core.common.NetworkMonitor
import com.conght.facemakerapp.core.common.retryWhenBackOnline
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.common.StarRating
import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.CharacterTransform
import com.conght.facemakerapp.data.model.CosplayChallenge
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.data.model.PartCategory
import com.conght.facemakerapp.data.model.PartItem
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import com.conght.facemakerapp.data.model.layerUrlFor
import com.conght.facemakerapp.data.repository.CreationRepository
import com.conght.facemakerapp.data.repository.MakerRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class CustomizeMode { MAKER, COSPLAY }

enum class PanelMode { EDIT, MOVE }

data class CustomizeUiState(
    val detail: Resource<TemplateDetail> = Resource.Loading,
    /** null khi chưa có cả config ban đầu lẫn detail (đang chờ API). */
    val config: CharacterConfig? = null,
    val selectedCategoryId: String? = null,
    val panel: PanelMode = PanelMode.EDIT,
    val isDirty: Boolean = false,
    val mode: CustomizeMode = CustomizeMode.MAKER,
    /** Chỉ có ở mode COSPLAY. */
    val cosplay: CosplayState? = null,
) {
    val templateDetail: TemplateDetail? get() = (detail as? Resource.Success)?.data
    val selectedCategory: PartCategory? get() = templateDetail?.categories?.firstOrNull { it.id == selectedCategoryId }
    val selection: PartSelection? get() = selectedCategoryId?.let { config?.selections?.get(it) }
    val selectedItem: PartItem? get() = selectedCategory?.items?.firstOrNull { it.id == selection?.itemId }
    val isReady: Boolean get() = templateDetail != null && config != null
}

/** Trạng thái màn chơi Cosplay (đồng hồ 10 phút, điểm khi nộp bài). */
data class CosplayState(
    val referenceImageUrl: String,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    /** % giống đáp án, có sau khi Done / hết giờ. */
    val scorePercent: Int? = null,
    /** Ảnh nhân vật người chơi (cacheDir) để hiện ở popup điểm. */
    val resultImagePath: String? = null,
) {
    val isFinished: Boolean get() = scorePercent != null
}

sealed interface CustomizeEvent {
    data class OpenEditor(val characterPath: String, val configJson: String) : CustomizeEvent
    data class OpenCosplaySuccess(val creationId: Long, val stars: Int) : CustomizeEvent
    data object SaveFailed : CustomizeEvent
}

/**
 * Customize nhân vật (Maker / Cosplay / Edit). Mọi lựa chọn lưu trong [SavedStateHandle]
 * (config dạng JSON) để không mất khi xoay / recreate / process death.
 */
@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val makerRepository: MakerRepository,
    private val creationRepository: CreationRepository,
    private val gson: Gson,
    private val savedStateHandle: SavedStateHandle,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val mode: CustomizeMode =
        savedStateHandle.get<String>(CustomizeFragment.ARG_MODE)?.let(CustomizeMode::valueOf) ?: CustomizeMode.MAKER

    private val initialConfig: CharacterConfig? =
        savedStateHandle.get<String>(CustomizeFragment.ARG_CONFIG_JSON)?.let(::parseConfig)

    /** Đề bài Cosplay (ảnh tham chiếu + đáp án), chỉ có ở mode COSPLAY. */
    private val challenge: CosplayChallenge? =
        savedStateHandle.get<String>(CustomizeFragment.ARG_CHALLENGE_JSON)
            ?.let { runCatching { gson.fromJson(it, CosplayChallenge::class.java) }.getOrNull() }

    private val templateId: String? = savedStateHandle.get<String>(CustomizeFragment.ARG_TEMPLATE_ID)
        ?: initialConfig?.templateId
        ?: challenge?.templateId

    private val reloadKey = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val detail: Flow<Resource<TemplateDetail>> = reloadKey.flatMapLatest {
        val id = templateId
        // Không có template (args thiếu) → giữ Loading
        if (id == null) flow { emit(Resource.Loading); awaitCancellation() }
        else makerRepository.templateDetail(id)
    }.onEach { resource ->
        if (resource is Resource.Success) onDetailLoaded(resource.data)
    }

    private val configJson = savedStateHandle.getStateFlow(KEY_CONFIG, initialConfig?.let(gson::toJson))
    private val selectedCategory = savedStateHandle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val panel = savedStateHandle.getStateFlow(KEY_PANEL, PanelMode.EDIT.name)
    private val dirty = savedStateHandle.getStateFlow(KEY_DIRTY, false)

    private val remaining = savedStateHandle.getStateFlow(KEY_REMAINING, COSPLAY_SECONDS)
    private val score = savedStateHandle.getStateFlow<Int?>(KEY_SCORE, null)
    private val resultPath = savedStateHandle.getStateFlow<String?>(KEY_RESULT_PATH, null)

    private val cosplayState: Flow<CosplayState?> = combine(remaining, score, resultPath) { left, score, path ->
        challenge?.let { CosplayState(it.referenceImageUrl, left, COSPLAY_SECONDS, score, path) }
    }

    val state: StateFlow<CustomizeUiState> = combine(
        combine(detail, configJson, selectedCategory, panel, dirty) { detail, json, category, panel, dirty ->
            CustomizeUiState(
                detail = detail,
                config = json?.let(::parseConfig),
                selectedCategoryId = category,
                panel = PanelMode.valueOf(panel),
                isDirty = dirty,
                mode = mode,
            )
        },
        cosplayState,
    ) { ui, cosplay -> ui.copy(cosplay = cosplay) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CustomizeUiState(mode = mode))

    private val _events = Channel<CustomizeEvent>(Channel.BUFFERED)
    val events: Flow<CustomizeEvent> = _events.receiveAsFlow()

    /**
     * Kết quả random đã tính sẵn (random cả nhân vật / xúc xắc của 1 category): ảnh layer của nó được
     * tải trước qua [prefetchUrls] → bấm random thì layer lấy từ disk cache, nhân vật hiện ngay.
     */
    private var nextRandomAll: CharacterConfig? = null
    private var nextRandomCategory: Pair<String, PartSelection>? = null

    private val _prefetchUrls = Channel<List<String>>(Channel.BUFFERED)

    /** Link ảnh layer cần tải trước vào disk cache (Fragment gọi ImagePrefetcher). */
    val prefetchUrls: Flow<List<String>> = _prefetchUrls.receiveAsFlow()

    private val current get() = state.value

    init {
        if (mode == CustomizeMode.COSPLAY && challenge != null) startCountdown()
        retryWhenBackOnline(networkMonitor, isError = { current.detail is Resource.Error }, retry = ::retry)
    }

    /** Đếm ngược trong ViewModel (sống qua xoay màn); bắt đầu khi nhân vật đã sẵn sàng. */
    private fun startCountdown() {
        viewModelScope.launch {
            state.first { it.isReady }
            while (remaining.value > 0 && score.value == null) {
                delay(TICK_MS)
                if (score.value != null) break
                savedStateHandle[KEY_REMAINING] = (remaining.value - 1).coerceAtLeast(0)
            }
            if (score.value == null) finishCosplay()
        }
    }

    /** Done hoặc hết giờ: chấm điểm bằng [CosplayScorer] (một lần). */
    fun finishCosplay() {
        val answer = challenge?.answer ?: return
        val config = current.config ?: return
        if (score.value != null) return
        savedStateHandle[KEY_SCORE] = CosplayScorer.score(answer, config)
    }

    /** Lưu ảnh nhân vật người chơi (cacheDir) để hiện ở popup điểm. */
    fun setCosplayResultImage(bitmap: Bitmap) {
        if (resultPath.value != null || savingResultImage) return
        savingResultImage = true
        viewModelScope.launch {
            try {
                savedStateHandle[KEY_RESULT_PATH] = creationRepository.saveTemp(bitmap, TEMP_COSPLAY_FILE)
            } finally {
                savingResultImage = false
            }
        }
    }

    private var savingResultImage = false

    private val _saving = MutableStateFlow(false)

    /** Đang lưu (Next / kết quả cosplay) → hiện LoadingDialog. */
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Đóng popup điểm: lưu vào My creation (CHARACTER + config) rồi mở Successful. */
    fun saveCosplayResult() {
        val percent = score.value ?: return
        val path = resultPath.value ?: return
        val config = current.config ?: return
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path) }
                    ?: error("Không đọc được ảnh kết quả")
                val id = creationRepository.save(CreationType.CHARACTER, bitmap, config)
                savedStateHandle[KEY_DIRTY] = false
                _events.send(CustomizeEvent.OpenCosplaySuccess(id, StarRating.fromPercent(percent)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lưu kết quả cosplay lỗi", e)
                _events.send(CustomizeEvent.SaveFailed)
            } finally {
                _saving.value = false
            }
        }
    }

    private fun onDetailLoaded(detail: TemplateDetail) {
        if (configJson.value == null) savedStateHandle[KEY_CONFIG] = gson.toJson(detail.defaultConfig)
        val category = selectedCategory.value
        if (category == null || detail.categories.none { it.id == category }) {
            savedStateHandle[KEY_CATEGORY] = detail.categories.firstOrNull()?.id
        }
        // Nút random cả nhân vật chỉ có ở MAKER
        if (mode == CustomizeMode.MAKER && nextRandomAll == null) prepareRandomAll(detail)
        selectedCategory.value?.let { prepareRandomCategory(it, detail) }
    }

    /** Tính trước lần random cả nhân vật kế tiếp + tải trước mọi layer của nó. */
    private fun prepareRandomAll(detail: TemplateDetail) {
        val id = detail.templateId
        viewModelScope.launch {
            val random = randomOrNull { makerRepository.randomizeCharacter(id) } ?: return@launch
            nextRandomAll = random
            _prefetchUrls.send(detail.categories.mapNotNull { it.layerUrlFor(random.selections[it.id]) })
        }
    }

    /** Tính trước lần bấm xúc xắc kế tiếp của [categoryId] + tải trước layer của nó. */
    private fun prepareRandomCategory(categoryId: String, detail: TemplateDetail? = current.templateDetail) {
        val id = templateId ?: return
        val category = detail?.categories?.firstOrNull { it.id == categoryId } ?: return
        if (nextRandomCategory?.first == categoryId) return
        viewModelScope.launch {
            val selection = randomOrNull { makerRepository.randomizeCategory(id, categoryId) } ?: return@launch
            nextRandomCategory = categoryId to selection
            category.layerUrlFor(selection)?.let { _prefetchUrls.send(listOf(it)) }
        }
    }

    fun retry() {
        reloadKey.value++
    }

    fun selectCategory(id: String) {
        savedStateHandle[KEY_CATEGORY] = id
        if (current.isReady) prepareRandomCategory(id)
    }

    /** [itemId] null = None. Category bắt buộc chọn: None = trả về lựa chọn mặc định. */
    fun selectItem(itemId: String?) {
        val category = current.selectedCategory ?: return
        val detail = current.templateDetail ?: return
        val newSelection = if (itemId == null) {
            if (category.isRequired) {
                detail.defaultConfig.selections[category.id] ?: PartSelection(null, null)
            } else {
                PartSelection(null, null)
            }
        } else {
            val item = category.items.firstOrNull { it.id == itemId } ?: return
            val keepColor = current.selection?.colorId?.takeIf { c -> item.colors.any { it.id == c } }
            PartSelection(itemId, keepColor ?: item.colors.firstOrNull()?.id)
        }
        updateConfig { it.copy(selections = it.selections + (category.id to newSelection)) }
    }

    fun selectColor(colorId: String) {
        val categoryId = current.selectedCategoryId ?: return
        val selection = current.selection ?: return
        if (selection.itemId == null) return
        updateConfig { it.copy(selections = it.selections + (categoryId to selection.copy(colorId = colorId))) }
    }

    /** Sau khi Reset dialog xác nhận. */
    fun reset() {
        val default = current.templateDetail?.defaultConfig ?: return
        savedStateHandle[KEY_CONFIG] = gson.toJson(default)
        savedStateHandle[KEY_DIRTY] = false
    }

    fun flip() = updateTransform { it.copy(flipped = !it.flipped) }

    /** Dịch theo dp. */
    fun move(dxDp: Float, dyDp: Float) = updateTransform {
        it.copy(
            offsetX = (it.offsetX + dxDp).coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
            offsetY = (it.offsetY + dyDp).coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        )
    }

    fun rotate(degrees: Float) = updateTransform { it.copy(rotation = (it.rotation + degrees) % FULL_TURN) }

    /** Cộng [delta] vào scale (nút +/−), giới hạn [MIN_SCALE]..[MAX_SCALE]. */
    fun zoom(delta: Float) = updateTransform { it.copy(scale = (it.scale + delta).coerceIn(MIN_SCALE, MAX_SCALE)) }

    /** Nhân scale với [factor] (pinch). */
    fun zoomBy(factor: Float) = updateTransform { it.copy(scale = (it.scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)) }

    fun setPanel(mode: PanelMode) {
        savedStateHandle[KEY_PANEL] = mode.name
    }

    /** Xúc xắc trong lưới: random item + màu của category đang chọn. */
    fun randomCategory() {
        val id = templateId ?: return
        val categoryId = current.selectedCategoryId ?: return
        if (!current.isReady) return
        val prepared = nextRandomCategory?.takeIf { it.first == categoryId }?.second
        nextRandomCategory = null
        viewModelScope.launch {
            val selection = prepared ?: randomOrNull { makerRepository.randomizeCategory(id, categoryId) } ?: return@launch
            updateConfig { it.copy(selections = it.selections + (categoryId to selection)) }
            prepareRandomCategory(categoryId)
        }
    }

    /** Gọi sau khi Random dialog (Watch Ad) xác nhận: random cả nhân vật, giữ transform. */
    fun randomAll() {
        val id = templateId ?: return
        if (!current.isReady) return
        val prepared = nextRandomAll?.takeIf { it.templateId == id }
        nextRandomAll = null
        viewModelScope.launch {
            val random = prepared ?: randomOrNull { makerRepository.randomizeCharacter(id) } ?: return@launch
            updateConfig { it.copy(selections = random.selections) }
            current.templateDetail?.let(::prepareRandomAll)
        }
    }

    private suspend fun <T> randomOrNull(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Random lỗi", e)
        null
    }

    /**
     * Next: lưu bản Character vào My creation (internal storage) + ảnh tạm cho editor,
     * rồi phát [CustomizeEvent.OpenEditor].
     */
    fun saveAndContinue(bitmap: Bitmap) {
        val config = current.config ?: return
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                creationRepository.save(CreationType.CHARACTER, bitmap, config)
                val path = creationRepository.saveTemp(bitmap, TEMP_CHARACTER_FILE)
                savedStateHandle[KEY_DIRTY] = false
                _events.send(CustomizeEvent.OpenEditor(path, gson.toJson(config)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Lưu nhân vật lỗi", e)
                _events.send(CustomizeEvent.SaveFailed)
            } finally {
                _saving.value = false
            }
        }
    }

    private fun updateTransform(block: (CharacterTransform) -> CharacterTransform) =
        updateConfig { it.copy(transform = block(it.transform)) }

    private fun updateConfig(block: (CharacterConfig) -> CharacterConfig) {
        val config = current.config ?: return
        val updated = block(config)
        if (updated == config) return
        savedStateHandle[KEY_CONFIG] = gson.toJson(updated)
        savedStateHandle[KEY_DIRTY] = true
    }

    private fun parseConfig(json: String): CharacterConfig? =
        runCatching { gson.fromJson(json, CharacterConfig::class.java) }.getOrNull()

    private companion object {
        const val TAG = "CustomizeViewModel"
        const val KEY_CONFIG = "config_json"
        const val KEY_CATEGORY = "selected_category"
        const val KEY_PANEL = "panel"
        const val KEY_DIRTY = "dirty"
        const val TEMP_CHARACTER_FILE = "character_tmp.png"
        const val TEMP_COSPLAY_FILE = "cosplay_result.png"
        const val KEY_REMAINING = "cosplay_remaining"
        const val KEY_SCORE = "cosplay_score"
        const val KEY_RESULT_PATH = "cosplay_result_path"
        const val COSPLAY_SECONDS = 10 * 60
        const val TICK_MS = 1_000L
        const val STOP_TIMEOUT_MS = 5_000L
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 2f
        const val MAX_OFFSET_DP = 200f
        const val FULL_TURN = 360f
    }
}
