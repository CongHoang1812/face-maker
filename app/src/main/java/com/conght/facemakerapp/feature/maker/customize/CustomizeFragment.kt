package com.conght.facemakerapp.feature.maker.customize

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
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
import com.conght.facemakerapp.core.ui.loadImage
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.core.ui.skeleton.NoneAdapter
import com.conght.facemakerapp.core.ui.skeleton.SkeletonAdapter
import com.conght.facemakerapp.core.ui.skeleton.noneFirst
import com.conght.facemakerapp.databinding.FragmentCustomizeBinding
import com.conght.facemakerapp.feature.decor.editor.DecorEditorFragment
import com.conght.facemakerapp.feature.cosplay.CosplayScoreDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogs
import com.conght.facemakerapp.feature.dialog.setLoadingVisible
import com.conght.facemakerapp.feature.maker.move.MovePanelView
import com.conght.facemakerapp.feature.success.SuccessFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Customize nhân vật (Figma 3:335) + chế độ Move (8:24).
 * Args: [ARG_TEMPLATE_ID] hoặc [ARG_CONFIG_JSON] (Edit), [ARG_MODE] (MAKER | COSPLAY).
 * Chưa có API → luôn Loading: canvas / thanh màu / lưới / tab là skeleton, riêng ô None & Xúc xắc là ô thật.
 */
@AndroidEntryPoint
class CustomizeFragment : BaseFragment<FragmentCustomizeBinding>(FragmentCustomizeBinding::inflate) {

    private val viewModel: CustomizeViewModel by viewModels()

    private val isCosplay: Boolean
        get() = arguments?.getString(ARG_MODE) == CustomizeMode.COSPLAY.name

    /** Ảnh tham chiếu đã load (tránh load lại mỗi giây khi đồng hồ chạy). */
    private var referenceUrl: String? = null

    private lateinit var noneAdapter: NoneAdapter
    private lateinit var partAdapter: PartItemAdapter
    private lateinit var colorAdapter: ColorDotAdapter
    private lateinit var tabAdapter: CategoryTabAdapter
    private lateinit var loadingParts: ConcatAdapter
    private lateinit var readyParts: ConcatAdapter
    private lateinit var loadingColors: SkeletonAdapter
    private lateinit var loadingTabs: SkeletonAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        referenceUrl = null
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        setupLists()
        setupActions()
        setupDialogResults()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        collectWhenStarted(viewModel.state, ::render)
        collectWhenStarted(viewModel.saving) { setLoadingVisible(it) }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is CustomizeEvent.OpenEditor -> navigateSafe(
                    R.id.action_customize_to_decorEditor,
                    Bundle().apply {
                        putString(DecorEditorFragment.ARG_CHARACTER_PATH, event.characterPath)
                        putString(DecorEditorFragment.ARG_CONFIG_JSON, event.configJson)
                    },
                )

                is CustomizeEvent.OpenCosplaySuccess -> navigateSafe(
                    R.id.action_cosplayPlay_to_success,
                    Bundle().apply {
                        putLong(SuccessFragment.ARG_CREATION_ID, event.creationId)
                        putString(SuccessFragment.ARG_SOURCE, SuccessFragment.Source.COSPLAY.name)
                        putInt(SuccessFragment.ARG_STARS, event.stars)
                    },
                )

                CustomizeEvent.SaveFailed ->
                    showAppToast(R.string.save_failed)
            }
        }
    }

    private fun setupLists() {
        val gap = resources.getDimensionPixelSize(R.dimen.grid_gap)
        noneAdapter = NoneAdapter(R.layout.item_none_part) { viewModel.selectItem(null) }
        val diceAdapter = NoneAdapter(R.layout.item_dice_part) { viewModel.randomCategory() }
        partAdapter = PartItemAdapter { viewModel.selectItem(it.id) }
        loadingParts = noneFirst(noneAdapter, diceAdapter, SkeletonAdapter(R.layout.item_skeleton_part, SKELETON_PARTS))
        readyParts = noneFirst(noneAdapter, diceAdapter, partAdapter)
        binding.rvParts.layoutManager = GridLayoutManager(requireContext(), GRID_COLUMNS)
        binding.rvParts.addItemDecoration(GridSpacingItemDecoration(GRID_COLUMNS, gap, gap))

        colorAdapter = ColorDotAdapter { viewModel.selectColor(it.id) }
        loadingColors = SkeletonAdapter(R.layout.item_skeleton_color_dot, SKELETON_COLORS)
        binding.rvColors.addItemDecoration(
            LinearSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.color_dot_gap)),
        )

        tabAdapter = CategoryTabAdapter { viewModel.selectCategory(it.id) }
        loadingTabs = SkeletonAdapter(R.layout.item_skeleton_category_tab, SKELETON_TABS)
        binding.rvTabs.addItemDecoration(
            LinearSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.category_tab_gap)),
        )
    }

    private fun setupActions() {
        binding.btnBack.setOnSingleClickListener { onBack() }
        // COSPLAY: nút Next đổi thành "Done" (nộp bài) – màn thiếu trong Figma
        if (isCosplay) binding.btnNext.setText(R.string.done)
        binding.btnNext.setOnSingleClickListener {
            if (!viewModel.state.value.isReady) return@setOnSingleClickListener
            if (isCosplay) viewModel.finishCosplay() else viewModel.saveAndContinue(binding.canvas.exportBitmap())
        }
        binding.btnReset.setOnSingleClickListener {
            ConfirmDialogs.reset(KEY_RESET).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
        binding.btnFlip.setOnSingleClickListener { viewModel.flip() }
        binding.btnMove.setOnSingleClickListener { viewModel.setPanel(PanelMode.MOVE) }
        binding.btnRandomAll.setOnSingleClickListener {
            ConfirmDialogs.random(KEY_RANDOM).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
        binding.errorState.setOnRetryClickListener { viewModel.retry() }

        binding.movePanel.listener = object : MovePanelView.Listener {
            override fun onExit() = viewModel.setPanel(PanelMode.EDIT)
            override fun onMove(dxDp: Float, dyDp: Float) = viewModel.move(dxDp, dyDp)
            override fun onRotate(degrees: Float) = viewModel.rotate(degrees)
            override fun onZoom(delta: Float) = viewModel.zoom(delta)
        }
        binding.canvas.onGesture = { dx, dy, scale ->
            if (dx != 0f || dy != 0f) viewModel.move(dx, dy)
            if (scale != 1f) viewModel.zoomBy(scale)
        }
    }

    private fun setupDialogResults() {
        childFragmentManager.setFragmentResultListener(KEY_EXIT, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) findNavController().popBackStack()
        }
        childFragmentManager.setFragmentResultListener(KEY_RESET, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) viewModel.reset()
        }
        childFragmentManager.setFragmentResultListener(KEY_RANDOM, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) viewModel.randomAll()
        }
        childFragmentManager.setFragmentResultListener(CosplayScoreDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            viewModel.saveCosplayResult()
        }
    }

    private fun onBack() {
        val state = viewModel.state.value
        when {
            state.panel == PanelMode.MOVE -> viewModel.setPanel(PanelMode.EDIT)
            state.cosplay?.isFinished == true -> Unit // đang chấm điểm / popup điểm
            state.isDirty || isCosplay -> ConfirmDialogs.exit(KEY_EXIT).show(childFragmentManager, ConfirmDialogFragment.TAG)
            else -> findNavController().popBackStack()
        }
    }

    private fun render(state: CustomizeUiState) {
        val detail = state.templateDetail
        val config = state.config
        val ready = detail != null && config != null

        // Loading / Error / Success
        if (state.detail is Resource.Loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
        binding.errorState.isVisible = state.detail is Resource.Error
        (state.detail as? Resource.Error)?.let { binding.errorState.setError(it.throwable) }
        binding.canvasSkeleton.root.isInvisible = ready || state.detail is Resource.Error
        binding.canvas.isInvisible = !ready
        binding.btnNext.isEnabled = ready
        binding.btnNext.alpha = if (ready) 1f else DISABLED_ALPHA
        binding.rvParts.swapAdapter(if (ready) readyParts else loadingParts)
        binding.rvColors.swapAdapter(if (ready) colorAdapter else loadingColors)
        binding.rvTabs.swapAdapter(if (ready) tabAdapter else loadingTabs)

        // Panel Edit / Move
        val moving = state.panel == PanelMode.MOVE
        binding.editPanel.isVisible = !moving
        binding.movePanel.isVisible = moving
        binding.canvas.gesturesEnabled = moving
        binding.btnBack.isInvisible = moving
        binding.btnNext.isInvisible = moving
        // COSPLAY: bỏ cột tool trái + random-all (ô xúc xắc trong lưới vẫn giữ)
        listOf(binding.btnReset, binding.btnFlip, binding.btnMove, binding.btnRandomAll).forEach {
            it.isVisible = !moving && !isCosplay
        }
        renderCosplay(state.cosplay)

        if (!ready) {
            noneAdapter.isSelected = false
            binding.colorBar.isInvisible = false
            return
        }
        binding.canvas.render(detail, config)

        val selection = state.selection
        noneAdapter.isSelected = selection?.itemId == null
        tabAdapter.submitList(detail.categories.map { Selectable(it, it.id == state.selectedCategoryId) })
        partAdapter.submitList(state.selectedCategory?.items.orEmpty().map { Selectable(it, it.id == selection?.itemId) })

        val colors = state.selectedItem?.colors.orEmpty()
        binding.colorBar.isInvisible = colors.isEmpty()
        colorAdapter.submitList(colors.map { Selectable(it, it.id == selection?.colorId) })

        state.cosplay?.let(::handleCosplayResult)
    }

    /** Thanh thời gian + thumbnail ảnh tham chiếu (chỉ COSPLAY). */
    private fun renderCosplay(cosplay: CosplayState?) {
        binding.timerBar.isVisible = cosplay != null
        binding.ivReference.isVisible = cosplay != null
        cosplay ?: return
        val left = cosplay.remainingSeconds
        binding.tvTimer.text = getString(R.string.timer_format, left / SECONDS_PER_MINUTE, left % SECONDS_PER_MINUTE)
        binding.progressTimer.max = cosplay.totalSeconds
        binding.progressTimer.progress = left
        if (referenceUrl != cosplay.referenceImageUrl) {
            referenceUrl = cosplay.referenceImageUrl
            binding.ivReference.loadImage(cosplay.referenceImageUrl, R.color.white)
        }
    }

    /** Đã nộp bài: chụp nhân vật cho popup → hiện popup điểm (một lần, sống qua xoay màn). */
    private fun handleCosplayResult(cosplay: CosplayState) {
        val percent = cosplay.scorePercent ?: return
        val path = cosplay.resultImagePath
        if (path == null) {
            viewModel.setCosplayResultImage(binding.canvas.exportBitmap())
        } else if (childFragmentManager.findFragmentByTag(CosplayScoreDialogFragment.TAG) == null) {
            CosplayScoreDialogFragment.newInstance(percent, path).show(childFragmentManager, CosplayScoreDialogFragment.TAG)
        }
    }

    private fun RecyclerView.swapAdapter(target: RecyclerView.Adapter<*>) {
        if (adapter !== target) adapter = target
    }

    companion object {
        /** Id template nhân vật gốc (từ màn cata). */
        const val ARG_TEMPLATE_ID = "templateId"

        /** JSON [com.conght.facemakerapp.data.model.CharacterConfig] để Edit (Lucky roll / My creation). */
        const val ARG_CONFIG_JSON = "configJson"

        /** [CustomizeMode] name, mặc định MAKER. */
        const val ARG_MODE = "mode"

        /** JSON [com.conght.facemakerapp.data.model.CosplayChallenge] (mode COSPLAY). */
        const val ARG_CHALLENGE_JSON = "challengeJson"

        private const val KEY_EXIT = "customize_exit"
        private const val KEY_RESET = "customize_reset"
        private const val KEY_RANDOM = "customize_random"
        private const val GRID_COLUMNS = 5
        private const val SKELETON_PARTS = 18
        private const val SKELETON_COLORS = 8
        private const val SKELETON_TABS = 6
        private const val DISABLED_ALPHA = 0.5f
        private const val SECONDS_PER_MINUTE = 60
    }
}
