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
import com.conght.facemakerapp.feature.maker.move.MoveLimits
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
import kotlinx.coroutines.flow.collectLatest
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

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 2f
private const val MAX_OFFSET_DP = 200f

enum class CustomizeMode { MAKER, COSPLAY }

enum class PanelMode { EDIT, MOVE }

data class CustomizeUiState(
    val detail: Resource<TemplateDetail> = Resource.Loading,
    /** null khi chưa có cả config ban đầu lẫn detail (đang chờ API). */
    val config: CharacterConfig? = null,
    val selectedCategoryId: String? = null,
    val panel: PanelMode = PanelMode.EDIT,
    val isDirty: Boolean = false,
    /** Chỉ có ở mode COSPLAY. */
    val cosplay: CosplayState? = null,
) {
    val templateDetail: TemplateDetail? get() = (detail as? Resource.Success)?.data
    val selectedCategory: PartCategory? get() = templateDetail?.categories?.firstOrNull { it.id == selectedCategoryId }
    val selection: PartSelection? get() = selectedCategoryId?.let { config?.selections?.get(it) }
    val selectedItem: PartItem? get() = selectedCategory?.items?.firstOrNull { it.id == selection?.itemId }
    val isReady: Boolean get() = templateDetail != null && config != null

    /** Đang focus ô None thì không có bộ phận nào để di chuyển. */
    val canMovePart: Boolean get() = selection?.itemId != null

    /** Trạng thái các nút ở panel Move theo bộ phận đang chọn. */
    val moveLimits: MoveLimits
        get() {
            val categoryId = selectedCategoryId ?: return MoveLimits()
            val current = config ?: return MoveLimits()
            val part = current.partTransform(categoryId)
            // Cả nhân vật đang lật thì đi sang trái trên màn hình là tăng offsetX
            val canIncreaseX = part.offsetX < MAX_OFFSET_DP
            val canDecreaseX = part.offsetX > -MAX_OFFSET_DP
            val flipped = current.transform.flipped
            return MoveLimits(
                canMoveLeft = if (flipped) canIncreaseX else canDecreaseX,
                canMoveRight = if (flipped) canDecreaseX else canIncreaseX,
                canMoveUp = part.offsetY > -MAX_OFFSET_DP,
                canMoveDown = part.offsetY < MAX_OFFSET_DP,
                canZoomIn = part.scale < MAX_SCALE,
                canZoomOut = part.scale > MIN_SCALE,
            )
        }
}

/** Trạng thái màn chơi Cosplay (đồng hồ 10 phút, điểm khi nộp bài). */
data class CosplayState(
    val referenceImageUrl: String,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    /** % giống đáp án, có sau khi đạt 100% / hết giờ. */
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
    data object NoInternet : CustomizeEvent

    /** Có mạng: hiện dialog xác nhận Random cả nhân vật. */
    data object ConfirmRandomAll : CustomizeEvent

    /** Vừa random: cuộn lưới item và thanh màu tới lựa chọn mới của category đang mở. */
    data class ScrollToSelection(val selection: PartSelection) : CustomizeEvent
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
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val mode: CustomizeMode =
        savedStateHandle.get<String>(CustomizeFragment.ARG_MODE)?.let(CustomizeMode::valueOf) ?: CustomizeMode.MAKER

    /** JSON → config gần nhất: mỗi lần kéo / zoom state phát lại, khỏi parse lại chuỗi vừa serialize. */
    private var parsedJson: String? = null
    private var parsedConfig: CharacterConfig? = null

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
    private val countdownPaused = MutableStateFlow(false)
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
            )
        },
        cosplayState,
    ) { ui, cosplay -> ui.copy(cosplay = cosplay) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CustomizeUiState())

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
        if (mode == CustomizeMode.COSPLAY && challenge != null) {
            startCountdown()
            finishWhenComplete()
        }
        retryWhenBackOnline(networkMonitor, isError = { current.detail is Resource.Error }, retry = ::retry)
    }

    /**
     * Đếm ngược trong ViewModel (sống qua xoay màn); bắt đầu khi nhân vật đã sẵn sàng.
     * Dừng khi [setCountdownPaused] true, chạy tiếp từ số giây còn lại khi false.
     */
    private fun startCountdown() {
        viewModelScope.launch {
            state.first { it.isReady }
            countdownPaused.collectLatest { paused ->
                if (paused) return@collectLatest
                while (remaining.value > 0 && score.value == null) {
                    delay(TICK_MS)
                    if (score.value != null) break
                    savedStateHandle[KEY_REMAINING] = (remaining.value - 1).coerceAtLeast(0)
                }
                if (score.value == null) finishCosplay()
            }
        }
    }

    /** App vào nền (Home, tắt màn hình...) thì dừng đồng hồ Cosplay, quay lại thì chạy tiếp. */
    fun setCountdownPaused(paused: Boolean) {
        countdownPaused.value = paused
    }

    /**
     * Người chơi đổi đồ khớp 100% đáp án → nộp bài ngay, không chờ hết giờ. Theo dõi qua [state] để khi
     * điểm được chốt thì state đã mang config mới (Fragment không chụp nhầm nhân vật cũ).
     */
    private fun finishWhenComplete() {
        val answer = challenge?.answer ?: return
        viewModelScope.launch {
            state.first { ui ->
                ui.cosplay?.isFinished == true ||
                    (ui.isDirty && ui.config?.let { CosplayScorer.score(answer, it) } == COMPLETE_PERCENT)
            }
            finishCosplay()
        }
    }

    /** % hoàn thành hiện tại so với đáp án (popup ảnh mẫu); null khi chưa có đề bài / nhân vật. */
    fun cosplayProgressPercent(): Int? {
        val answer = challenge?.answer ?: return null
        val config = current.config ?: return null
        return CosplayScorer.score(answer, config)
    }

    /** Đạt 100% hoặc hết giờ: chốt điểm bằng [CosplayScorer] (một lần). */
    fun finishCosplay() {
        if (score.value != null) return
        savedStateHandle[KEY_SCORE] = cosplayProgressPercent() ?: return
    }

    /** false khi đã có / đang lưu ảnh kết quả → Fragment khỏi chụp thêm bitmap. */
    val needsCosplayResultImage: Boolean get() = resultPath.value == null && !savingResultImage

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

    /** Thao tác vừa bấm lúc mất mạng: Try again có mạng thì làm tiếp (sống qua xoay màn). */
    private var pendingOnlineAction: (CustomizeViewModel.() -> Unit)? = null

    /** Có mạng → chạy [action]; không → giữ lại [action] và phát [CustomizeEvent.NoInternet]. */
    fun runWhenOnline(action: CustomizeViewModel.() -> Unit) {
        viewModelScope.launch {
            if (networkMonitor.isOnline.first()) {
                action()
            } else {
                pendingOnlineAction = action
                _events.send(CustomizeEvent.NoInternet)
            }
        }
    }

    /** Try again trên dialog No Internet. */
    fun retryPendingAction() {
        val action = pendingOnlineAction ?: return
        pendingOnlineAction = null
        runWhenOnline(action)
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
        val selection = current.selection
        if (selection?.itemId == null) {
            // Đang None: chọn màu = chọn item đầu tiên có màu đó
            val item = current.selectedCategory?.items?.firstOrNull { i -> i.colors.any { it.id == colorId } } ?: return
            updateConfig { it.copy(selections = it.selections + (categoryId to PartSelection(item.id, colorId))) }
            return
        }
        updateConfig { it.copy(selections = it.selections + (categoryId to selection.copy(colorId = colorId))) }
    }

    /** Sau khi Reset dialog xác nhận. */
    fun reset() {
        val default = current.templateDetail?.defaultConfig ?: return
        savedStateHandle[KEY_CONFIG] = gson.toJson(default)
        savedStateHandle[KEY_DIRTY] = false
    }

    /** Lật cả nhân vật. */
    fun flip() = updateConfig { it.copy(transform = it.transform.copy(flipped = !it.transform.flipped)) }

    /** Dịch bộ phận đang chọn theo dp. */
    fun move(dxDp: Float, dyDp: Float) = updatePartTransform {
        it.copy(
            offsetX = (it.offsetX + dxDp * flipSign).coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
            offsetY = (it.offsetY + dyDp).coerceIn(-MAX_OFFSET_DP, MAX_OFFSET_DP),
        )
    }

    /** Xoay bộ phận đang chọn. */
    fun rotate(degrees: Float) = updatePartTransform { it.copy(rotation = (it.rotation + degrees * flipSign) % FULL_TURN) }

    /** Cộng [delta] vào scale bộ phận đang chọn (nút +/−), giới hạn [MIN_SCALE]..[MAX_SCALE]. */
    fun zoom(delta: Float) = updatePartTransform { it.copy(scale = (it.scale + delta).coerceIn(MIN_SCALE, MAX_SCALE)) }

    /** Nhân scale bộ phận đang chọn với [factor] (pinch). */
    fun zoomBy(factor: Float) = updatePartTransform { it.copy(scale = (it.scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)) }

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
            _events.send(CustomizeEvent.ScrollToSelection(selection))
            prepareRandomCategory(categoryId)
        }
    }

    /** Bấm nút Random cả nhân vật (đã qua [runWhenOnline]) → Fragment hiện dialog xác nhận. */
    fun requestRandomAll() {
        viewModelScope.launch { _events.send(CustomizeEvent.ConfirmRandomAll) }
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
            random.selections[current.selectedCategoryId]?.let { _events.send(CustomizeEvent.ScrollToSelection(it)) }
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

    /** Cả nhân vật đang lật thì layer cũng bị lật: đảo chiều ngang để bộ phận đi đúng hướng tay kéo. */
    private val flipSign: Float get() = if (current.config?.transform?.flipped == true) -1f else 1f

    /** Đổi transform của bộ phận đang chọn; đang là None (không có gì để di chuyển) thì bỏ qua. */
    private fun updatePartTransform(block: (CharacterTransform) -> CharacterTransform) {
        val categoryId = current.selectedCategoryId ?: return
        if (current.selection?.itemId == null) return
        updateConfig { config ->
            val updated = block(config.partTransform(categoryId))
            config.copy(partTransforms = config.partTransforms.orEmpty() + (categoryId to updated))
        }
    }

    private fun updateConfig(block: (CharacterConfig) -> CharacterConfig) {
        if (score.value != null) return // đã nộp bài: không đổi được nữa
        val config = current.config ?: return
        val updated = block(config)
        if (updated == config) return
        val json = gson.toJson(updated)
        parsedJson = json
        parsedConfig = updated
        savedStateHandle[KEY_CONFIG] = json
        savedStateHandle[KEY_DIRTY] = true
    }

    private fun parseConfig(json: String): CharacterConfig? {
        if (json == parsedJson) return parsedConfig
        return runCatching { gson.fromJson(json, CharacterConfig::class.java) }.getOrNull().also {
            parsedJson = json
            parsedConfig = it
        }
    }

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
        const val COMPLETE_PERCENT = 100
        const val TICK_MS = 1_000L
        const val STOP_TIMEOUT_MS = 5_000L
        const val FULL_TURN = 360f
    }
}
