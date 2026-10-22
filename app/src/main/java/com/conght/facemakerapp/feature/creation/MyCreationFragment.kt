package com.conght.facemakerapp.feature.creation

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.GridSpacingItemDecoration
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.core.ui.shareImages
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.databinding.FragmentMyCreationBinding
import com.conght.facemakerapp.feature.dialog.ConfirmDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogs
import com.conght.facemakerapp.feature.dialog.setLoadingVisible
import com.conght.facemakerapp.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * My creation (Figma 3:1228): 2 tab Character / Design, dữ liệu thật từ Room.
 * Long-press → chế độ chọn nhiều (xoá / share / download; tab Character thêm xuất sticker).
 */
@AndroidEntryPoint
class MyCreationFragment : BaseFragment<FragmentMyCreationBinding>(FragmentMyCreationBinding::inflate) {

    private val viewModel: MyCreationViewModel by viewModels()

    private val adapter = CreationAdapter(object : CreationAdapter.Listener {
        override fun onOpen(creation: Creation) = navigateSafe(
            R.id.action_myCreation_to_creationDetail,
            Bundle().apply { putLong(CreationDetailFragment.ARG_CREATION_ID, creation.id) },
        )

        override fun onLongPress(creation: Creation) = viewModel.startSelection(creation.id)
        override fun onToggle(creation: Creation) = viewModel.toggle(creation.id)
        override fun onDelete(creation: Creation) = confirmDelete(listOf(creation.id))
        override fun onEdit(creation: Creation) = openEdit(creation)
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.rvCreations.layoutManager = GridLayoutManager(requireContext(), COLUMNS)
        binding.rvCreations.addItemDecoration(
            GridSpacingItemDecoration(
                COLUMNS,
                resources.getDimensionPixelSize(R.dimen.creation_grid_gap_h),
                resources.getDimensionPixelSize(R.dimen.creation_grid_gap_v),
            ),
        )
        binding.rvCreations.adapter = adapter

        binding.btnBack.setOnSingleClickListener { onBack() }
        binding.tabCharacter.setOnSingleClickListener { viewModel.selectTab(CreationType.CHARACTER) }
        binding.tabDesign.setOnSingleClickListener { viewModel.selectTab(CreationType.DESIGN) }
        binding.btnDelete.setOnSingleClickListener { confirmDelete(viewModel.state.value.selectedIds) }
        binding.btnShare.setOnSingleClickListener { share(viewModel.state.value.selectedItems) }
        binding.btnDownload.setOnSingleClickListener { viewModel.download(viewModel.state.value.selectedIds) }
        listOf(binding.btnTelegram, binding.btnWhatsapp).forEach {
            it.setOnSingleClickListener {
                // TODO(STICKER_EXPORT): tích hợp WhatsApp sticker / Telegram
                showAppToast(R.string.coming_soon)
            }
        }

        childFragmentManager.setFragmentResultListener(KEY_DELETE, viewLifecycleOwner) { _, result ->
            viewModel.onDeleteConfirmed(ConfirmDialogFragment.isConfirmed(result))
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        collectWhenStarted(viewModel.state, ::render)
        collectWhenStarted(viewModel.busy) { setLoadingVisible(it) }
        collectWhenStarted(viewModel.events) { event ->
            if (event is MyCreationEvent.Downloaded) {
                val message = if (event.success) R.string.saved else R.string.save_failed
                showAppToast(message)
            }
        }
    }

    override fun onDestroyView() {
        // Adapter sống theo Fragment -> gỡ khỏi RecyclerView để không giữ view cũ
        binding.rvCreations.adapter = null
        super.onDestroyView()
    }

    private fun render(state: MyCreationUiState) {
        renderTabs(state.tab)
        val items = state.items
        val empty = items != null && items.isEmpty()
        binding.emptyState.isVisible = empty
        binding.tvEmpty.setText(
            if (state.tab == CreationType.CHARACTER) R.string.no_character_here else R.string.no_design_here,
        )
        adapter.submitList(items.orEmpty().map { CreationItem(it, state.selectionMode, it.id in state.selectedIds) })

        val hasSelection = state.selectionMode && state.selectedIds.isNotEmpty()
        listOf(binding.btnDelete, binding.btnShare, binding.btnDownload).forEach {
            it.isVisible = state.selectionMode
            it.isEnabled = hasSelection
            it.alpha = if (hasSelection) 1f else DISABLED_ALPHA
        }
        binding.stickerBar.isVisible = state.selectionMode && state.tab == CreationType.CHARACTER
    }

    /** Tab đang chọn: nền cream, nằm cao hơn 8dp và đè lên tab kia. */
    private fun renderTabs(tab: CreationType) {
        val top = resources.getDimensionPixelSize(R.dimen.creation_tabs_top)
        val offset = resources.getDimensionPixelSize(R.dimen.creation_tab_offset)
        val z = resources.getDimension(R.dimen.creation_tab_z)
        listOf(binding.tabCharacter to CreationType.CHARACTER, binding.tabDesign to CreationType.DESIGN).forEach { (view, type) ->
            val selected = type == tab
            view.isSelected = selected
            view.translationZ = if (selected) z else 0f
            view.updateLayoutParams<ConstraintLayout.LayoutParams> { topMargin = if (selected) top else top + offset }
        }
    }

    private fun confirmDelete(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModel.requestDelete(ids)
        ConfirmDialogs.delete(KEY_DELETE).show(childFragmentManager, ConfirmDialogFragment.TAG)
    }

    private fun share(items: List<Creation>) {
        if (items.isEmpty()) return
        if (!requireContext().shareImages(items.map { it.imagePath }, getString(R.string.cd_share))) {
            showAppToast(R.string.error_generic)
        }
    }

    private fun openEdit(creation: Creation) {
        navigateSafe(
            R.id.action_myCreation_to_customize,
            Bundle().apply { putString(CustomizeFragment.ARG_CONFIG_JSON, creation.configJson) },
        )
    }

    private fun onBack() {
        if (viewModel.state.value.selectionMode) viewModel.exitSelection() else findNavController().popBackStack()
    }

    companion object {
        /** Tab mở đầu: [CreationType] name (mặc định DESIGN). */
        const val ARG_TAB = "tab"

        private const val KEY_DELETE = "creation_delete"
        private const val COLUMNS = 2
        private const val DISABLED_ALPHA = 0.5f
    }
}
