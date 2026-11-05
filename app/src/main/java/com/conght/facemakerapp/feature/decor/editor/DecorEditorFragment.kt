package com.conght.facemakerapp.feature.decor.editor

import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.ui.GridSpacingItemDecoration
import com.conght.facemakerapp.core.ui.LinearSpacingItemDecoration
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.core.ui.skeleton.NoneAdapter
import com.conght.facemakerapp.core.ui.skeleton.SkeletonAdapter
import com.conght.facemakerapp.core.ui.skeleton.noneFirst
import com.conght.facemakerapp.data.model.DecorItem
import com.conght.facemakerapp.data.model.DecorPack
import com.conght.facemakerapp.data.model.DecorType
import com.conght.facemakerapp.databinding.FragmentDecorEditorBinding
import com.conght.facemakerapp.feature.dialog.ColorPickerDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogs
import com.conght.facemakerapp.feature.dialog.setLoadingVisible
import com.conght.facemakerapp.feature.success.SuccessFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine

/**
 * Decor editor (Figma 3:773 / 3:843 / 3:913 / 3:993 / 3:1155).
 * Local làm đầy đủ: Color, Image (Photo Picker), Text, font, màu chữ. Pack (API) → skeleton.
 * Ô None chỉ có ở lưới Background; Sticker / Speech không có, xoá phần tử bằng handle trên canvas.
 */
@AndroidEntryPoint
class DecorEditorFragment : BaseFragment<FragmentDecorEditorBinding>(FragmentDecorEditorBinding::inflate) {

    private val viewModel: DecorEditorViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importBackground)
    }

    private lateinit var noneAdapter: NoneAdapter
    private lateinit var cellAdapter: DecorCellAdapter
    private lateinit var gridLoading: ConcatAdapter
    private lateinit var gridReady: ConcatAdapter
    private lateinit var gridSkeleton: SkeletonAdapter
    private lateinit var subTabAdapter: SubTabAdapter
    private lateinit var subTabsWithLoading: ConcatAdapter
    private lateinit var subTabSkeleton: SkeletonAdapter
    private lateinit var fontAdapter: FontChipAdapter
    private lateinit var colorChipAdapter: ColorChipAdapter
    private lateinit var backgroundColors: List<Int>

    /** Pack hiện tại theo loại, cập nhật từ ViewModel. */
    private var packs: Map<DecorType, Resource<List<DecorPack>>> = emptyMap()

    /** Đầu vào của hàng tab con + lưới ở lần render trước: kéo / xoay phần tử không đổi gì thì khỏi dựng lại. */
    private var renderedGridKey: GridKey? = null
    private var renderedPacks: Map<DecorType, Resource<List<DecorPack>>>? = null

    /** Bong bóng mà ô nhập ẩn et_speech đang sửa. */
    private var editingSpeechId: String? = null
    private var renderedInputFontId: String? = null

    private data class GridKey(
        val tab: EditorTab,
        val subTab: Int,
        val background: DecorBackground,
        val recentImages: List<String>,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderedGridKey = null
        renderedPacks = null
        editingSpeechId = null
        renderedInputFontId = null
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        backgroundColors = resources.obtainTypedArray(R.array.decor_background_colors).use { it.colors() }
        setupCanvas()
        setupTabs()
        setupLists()
        setupTextInput()
        setupDialogResults()

        binding.btnBack.setOnSingleClickListener { onBack() }
        binding.btnDownload.setOnSingleClickListener {
            // exportBitmap() không vẽ viền chọn / handle nên không cần bỏ chọn trước
            viewModel.save(binding.canvas.exportBitmap())
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        collectWhenStarted(viewModel.data) { render() }
        val flows = viewModel.packs
        val allPacks = combine(
            flows.getValue(DecorType.BACKGROUND),
            flows.getValue(DecorType.STICKER),
            flows.getValue(DecorType.SPEECH),
        ) { bg, sticker, speech ->
            mapOf(DecorType.BACKGROUND to bg, DecorType.STICKER to sticker, DecorType.SPEECH to speech)
        }
        collectWhenStarted(allPacks) {
            packs = it
            render()
        }
        collectWhenStarted(viewModel.saving) {
            binding.btnDownload.isEnabled = !it
            setLoadingVisible(it)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is DecorEditorEvent.Saved -> navigateSafe(
                    R.id.action_decorEditor_to_success,
                    Bundle().apply {
                        putLong(SuccessFragment.ARG_CREATION_ID, event.creationId)
                        putString(SuccessFragment.ARG_SOURCE, SuccessFragment.Source.MAKER.name)
                    },
                )

                DecorEditorEvent.SaveFailed -> showAppToast(R.string.save_failed)
                DecorEditorEvent.ImportFailed -> showAppToast(R.string.error_generic)
            }
        }
    }

    // ---------- Setup ----------

    private fun setupCanvas() {
        binding.canvas.setCharacter(viewModel.characterPath)
        binding.canvas.listener = object : DecorCanvasView.Listener {
            override fun onSelect(id: String?) = viewModel.select(id)
            override fun onTransform(id: String, x: Float, y: Float, scale: Float, rotation: Float) =
                viewModel.transform(id, x, y, scale, rotation)

            override fun onFlip(id: String) = viewModel.flip(id)
            override fun onDelete(id: String) = viewModel.delete(id)
            override fun onEditText(id: String) {
                val element = viewModel.data.value.elements.firstOrNull { it.id == id } ?: return
                viewModel.select(id)
                if (element.type == ElementType.SPEECH) {
                    viewModel.selectMainTab(EditorTab.SPEECH)
                    showKeyboard(binding.etSpeech)
                } else {
                    viewModel.selectMainTab(EditorTab.TEXT)
                    binding.inputRow.isVisible = true
                    binding.etText.setTextAndMoveCursor(element.text.orEmpty())
                    showKeyboard(binding.etText)
                }
            }
        }
    }

    private fun setupTabs() {
        mapOf(
            binding.tabBackground to EditorTab.BACKGROUND,
            binding.tabSticker to EditorTab.STICKER,
            binding.tabSpeech to EditorTab.SPEECH,
            binding.tabText to EditorTab.TEXT,
        ).forEach { (view, tab) -> view.setOnSingleClickListener(intervalMs = TAB_DEBOUNCE_MS) { viewModel.selectMainTab(tab) } }
    }

    private fun setupLists() {
        val gap = resources.getDimensionPixelSize(R.dimen.grid_gap)
        val columns = resources.getInteger(R.integer.part_grid_columns)

        noneAdapter = NoneAdapter(R.layout.item_none_decor) { viewModel.setBackgroundNone() }
        cellAdapter = DecorCellAdapter(::onCellClick)
        gridSkeleton = SkeletonAdapter(R.layout.item_skeleton_decor, SKELETON_CELLS)
        gridLoading = noneFirst(noneAdapter, gridSkeleton)
        gridReady = noneFirst(noneAdapter, cellAdapter)
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.rvGrid.addItemDecoration(GridSpacingItemDecoration(columns, gap, gap))

        subTabAdapter = SubTabAdapter(viewModel::selectSubTab)
        subTabSkeleton = SkeletonAdapter(R.layout.item_skeleton_sub_tab, SKELETON_PACKS)
        subTabsWithLoading = ConcatAdapter(subTabAdapter, subTabSkeleton)
        binding.rvSubTabs.addItemDecoration(LinearSpacingItemDecoration(gap))

        fontAdapter = FontChipAdapter { viewModel.setFont(it.id) }
        binding.rvFonts.adapter = fontAdapter
        binding.rvFonts.addItemDecoration(LinearSpacingItemDecoration(gap))

        val textColors = resources.obtainTypedArray(R.array.decor_text_colors).use { it.colors() }
        colorChipAdapter = ColorChipAdapter(listOf<Int?>(null) + textColors) { color ->
            if (color == null) {
                showColorPicker(KEY_TEXT_COLOR)
            } else {
                viewModel.setTextColor(color)
            }
        }
        binding.rvTextColors.adapter = colorChipAdapter
        binding.rvTextColors.addItemDecoration(LinearSpacingItemDecoration(gap))
    }

    /**
     * Edge-to-edge nên adjustResize không đẩy layout lên: tự dịch panel vừa đủ để hàng nhập chữ
     * nằm trên bàn phím. Canvas giữ nguyên kích thước.
     */
    private fun liftPanelAboveKeyboard() {
        val panel = binding.panel
        val row = binding.inputRow
        val insets = ViewCompat.getRootWindowInsets(panel)
        val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val navBar = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val overlap = ime - navBar - (panel.height - row.bottom) + resources.getDimensionPixelSize(R.dimen.grid_gap)
        panel.translationY = if (ime > 0 && row.isVisible) -overlap.coerceAtLeast(0).toFloat() else 0f
    }

    private fun setupTextInput() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.panel) { _, insets ->
            liftPanelAboveKeyboard()
            insets
        }
        binding.panel.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> liftPanelAboveKeyboard() }
        binding.btnTextDone.setOnSingleClickListener { submitText() }
        binding.etSpeech.doAfterTextChanged { text ->
            editingSpeechId?.let { viewModel.setSpeechText(it, text?.toString().orEmpty()) }
        }
        binding.etSpeech.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        binding.etText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitText()
                true
            } else {
                false
            }
        }
    }

    private fun setupDialogResults() {
        childFragmentManager.setFragmentResultListener(KEY_EXIT, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) findNavController().popBackStack()
        }
        childFragmentManager.setFragmentResultListener(KEY_BG_COLOR, viewLifecycleOwner) { _, result ->
            viewModel.setBackgroundColor(ColorPickerDialogFragment.color(result))
        }
        childFragmentManager.setFragmentResultListener(KEY_TEXT_COLOR, viewLifecycleOwner) { _, result ->
            viewModel.setTextColor(ColorPickerDialogFragment.color(result))
        }
    }

    // ---------- Render ----------

    private fun render() {
        val data = viewModel.data.value
        binding.canvas.render(data)

        val tab = data.mainTab
        binding.tabBackground.isSelected = tab == EditorTab.BACKGROUND
        binding.tabSticker.isSelected = tab == EditorTab.STICKER
        binding.tabSpeech.isSelected = tab == EditorTab.SPEECH
        binding.tabText.isSelected = tab == EditorTab.TEXT

        val showText = tab == EditorTab.SPEECH || tab == EditorTab.TEXT
        val target = data.textTarget
        val speech = target?.takeIf { it.type == ElementType.SPEECH }
        val leftTextInput = tab != EditorTab.TEXT && binding.etText.hasFocus()
        val leftSpeechInput = speech == null && binding.etSpeech.hasFocus()
        if (leftTextInput || leftSpeechInput) hideKeyboard()
        binding.inputRow.isVisible = tab == EditorTab.TEXT
        if (speech?.id != editingSpeechId) {
            editingSpeechId = speech?.id
            binding.etSpeech.setTextAndMoveCursor(speech?.text.orEmpty())
        }
        binding.rvFonts.isVisible = showText
        binding.rvTextColors.isVisible = showText
        binding.rvGrid.isVisible = tab != EditorTab.TEXT
        binding.rvSubTabs.isVisible = tab != EditorTab.TEXT
        val fontId = target?.fontId ?: data.fontId
        fontAdapter.selectedId = fontId
        colorChipAdapter.selectedColor = target?.color ?: data.textColor
        if (fontId != renderedInputFontId) {
            renderedInputFontId = fontId
            binding.etText.typeface = ResourcesCompat.getFont(requireContext(), DecorFonts.of(fontId).fontRes)
        }

        val gridKey = GridKey(tab, data.subTab, data.background, data.recentImages)
        if (gridKey != renderedGridKey || packs !== renderedPacks) {
            renderedGridKey = gridKey
            renderedPacks = packs
            renderGrid(data, renderSubTabs(data))
        }

        val loading = packs.values.any { it is Resource.Loading }
        if (loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
    }

    /** Vẽ hàng tab con, trả về Resource của pack đang chọn (null nếu tab Color / Image / Text). */
    private fun renderSubTabs(data: EditorData): Resource<DecorPack>? {
        val tab = data.mainTab
        val type = tab.packType ?: return null
        val resource = packs[type] ?: Resource.Loading
        val fixed = if (tab == EditorTab.BACKGROUND) {
            listOf(getString(R.string.tab_color), getString(R.string.tab_image))
        } else {
            emptyList()
        }
        val packList = (resource as? Resource.Success)?.data.orEmpty()
        val titles = fixed + packList.mapIndexed { i, p -> p.name.ifBlank { getString(R.string.tab_pack_number, i + 1) } }
        subTabAdapter.submitList(titles.mapIndexed { i, title -> SubTabAdapter.Tab(i, title, i == data.subTab) })
        binding.rvSubTabs.swapAdapter(if (resource is Resource.Success) subTabAdapter else subTabsWithLoading)

        val packIndex = data.subTab - fixed.size
        return when {
            packIndex < 0 -> null
            resource is Resource.Success -> resource.data.getOrNull(packIndex)?.let { Resource.Success(it) }
                ?: Resource.Success(DecorPack("", "", type, emptyList()))

            resource is Resource.Error -> resource
            else -> Resource.Loading
        }
    }

    private fun renderGrid(data: EditorData, pack: Resource<DecorPack>?) {
        val bg = data.background
        when (data.mainTab) {
            EditorTab.BACKGROUND -> when (data.subTab) {
                SUB_COLOR -> {
                    noneAdapter.isSelected = bg.isNone
                    val customColor = bg.color != null && bg.color !in backgroundColors
                    showCells(listOf(DecorCell.ColorWheel(customColor)) + backgroundColors.map { DecorCell.Color(it, it == bg.color) })
                }

                SUB_IMAGE -> {
                    noneAdapter.isSelected = bg.isNone
                    showCells(listOf(DecorCell.AddImage) + data.recentImages.map { DecorCell.Image(it, it, it == bg.imagePath) })
                }

                else -> {
                    noneAdapter.isSelected = bg.isNone
                    showPack(pack) { it.imageUrl == bg.imagePath }
                }
            }

            EditorTab.STICKER, EditorTab.SPEECH -> showPack(pack, withNone = false) { false }

            EditorTab.TEXT -> Unit
        }
    }

    private fun showPack(pack: Resource<DecorPack>?, withNone: Boolean = true, selected: (DecorItem) -> Boolean) {
        if (pack is Resource.Success) {
            showCells(pack.data.items.map { DecorCell.Image(it.id, it.thumbnailUrl, selected(it)) }, withNone)
        } else {
            binding.rvGrid.swapAdapter(if (withNone) gridLoading else gridSkeleton)
        }
    }

    private fun showCells(cells: List<DecorCell>, withNone: Boolean = true) {
        cellAdapter.submitList(cells)
        binding.rvGrid.swapAdapter(if (withNone) gridReady else cellAdapter)
    }

    // ---------- Hành động ----------

    private fun onCellClick(cell: DecorCell) {
        val data = viewModel.data.value
        when (cell) {
            is DecorCell.ColorWheel -> showColorPicker(KEY_BG_COLOR)
            DecorCell.AddImage -> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            is DecorCell.Color -> viewModel.setBackgroundColor(cell.color)
            is DecorCell.Image -> {
                val packItem = currentPackItems().firstOrNull { it.id == cell.id }
                when (data.mainTab) {
                    EditorTab.BACKGROUND -> viewModel.setBackgroundImage(packItem?.imageUrl ?: cell.url)
                    EditorTab.STICKER -> packItem?.let { viewModel.addSticker(it.imageUrl) }
                    EditorTab.SPEECH -> packItem?.let {
                        viewModel.addSpeech(it.imageUrl)
                        showKeyboard(binding.etSpeech)
                    }
                    EditorTab.TEXT -> Unit
                }
            }
        }
    }

    private fun currentPackItems(): List<DecorItem> {
        val data = viewModel.data.value
        val type = data.mainTab.packType ?: return emptyList()
        val fixed = if (data.mainTab == EditorTab.BACKGROUND) 2 else 0
        val list = (packs[type] as? Resource.Success)?.data ?: return emptyList()
        return list.getOrNull(data.subTab - fixed)?.items.orEmpty()
    }

    private fun EditText.setTextAndMoveCursor(value: String) {
        setText(value)
        setSelection(value.length)
    }

    private fun submitText() {
        viewModel.submitText(binding.etText.text?.toString().orEmpty())
        binding.etText.text?.clear()
        hideKeyboard()
    }

    /** Mỗi lần mở bảng màu đều bắt đầu từ màu mặc định, không giữ màu đã chọn lần trước. */
    private fun showColorPicker(requestKey: String) {
        if (childFragmentManager.findFragmentByTag(ColorPickerDialogFragment.TAG) == null) {
            ColorPickerDialogFragment.newInstance(requestKey, Color.RED).show(childFragmentManager, ColorPickerDialogFragment.TAG)
        }
    }

    private fun onBack() {
        if (viewModel.data.value.isDirty) {
            ConfirmDialogs.exit(KEY_EXIT).show(childFragmentManager, ConfirmDialogFragment.TAG)
        } else {
            findNavController().popBackStack()
        }
    }

    private fun showKeyboard(input: EditText) {
        input.post {
            input.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, input).show(WindowInsetsCompat.Type.ime())
        }
    }

    private fun hideKeyboard() {
        binding.etText.clearFocus()
        binding.etSpeech.clearFocus()
        WindowCompat.getInsetsController(requireActivity().window, binding.etText).hide(WindowInsetsCompat.Type.ime())
    }


    private fun RecyclerView.swapAdapter(target: RecyclerView.Adapter<*>) {
        if (adapter !== target) adapter = target
    }

    private fun TypedArray.colors(): List<Int> = (0 until length()).map { getColor(it, Color.WHITE) }

    companion object {
        /** Đường dẫn ảnh nhân vật tạm (cacheDir, internal storage). */
        const val ARG_CHARACTER_PATH = "characterPath"

        /** JSON CharacterConfig của nhân vật. */
        const val ARG_CONFIG_JSON = "configJson"

        private const val KEY_EXIT = "editor_exit"
        private const val KEY_BG_COLOR = "editor_bg_color"
        private const val KEY_TEXT_COLOR = "editor_text_color"
        private const val SUB_COLOR = 0
        private const val SUB_IMAGE = 1
        private const val SKELETON_CELLS = 14
        private const val SKELETON_PACKS = 3
        private const val TAB_DEBOUNCE_MS = 150L
    }
}
