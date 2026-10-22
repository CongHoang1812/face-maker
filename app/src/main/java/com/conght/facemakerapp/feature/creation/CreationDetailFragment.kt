package com.conght.facemakerapp.feature.creation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.loadWithShimmer
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.core.ui.shareImages
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.databinding.FragmentCreationDetailBinding
import com.conght.facemakerapp.feature.dialog.ConfirmDialogFragment
import com.conght.facemakerapp.feature.dialog.ConfirmDialogs
import com.conght.facemakerapp.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint

/** Chi tiết creation (Figma 3:1583 / 3:1608). Character có nút Edit → Customize. */
@AndroidEntryPoint
class CreationDetailFragment : BaseFragment<FragmentCreationDetailBinding>(FragmentCreationDetailBinding::inflate) {

    private val viewModel: CreationDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnDelete.setOnSingleClickListener {
            ConfirmDialogs.delete(KEY_DELETE).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
        binding.btnShare.setOnSingleClickListener {
            val path = viewModel.creation.value?.imagePath ?: return@setOnSingleClickListener
            if (!requireContext().shareImages(listOf(path), getString(R.string.cd_share))) {
                showAppToast(R.string.error_generic)
            }
        }
        binding.btnDownload.setOnSingleClickListener { viewModel.download() }
        binding.btnEdit.setOnSingleClickListener {
            val creation = viewModel.creation.value ?: return@setOnSingleClickListener
            navigateSafe(
                R.id.action_creationDetail_to_customize,
                Bundle().apply { putString(CustomizeFragment.ARG_CONFIG_JSON, creation.configJson) },
            )
        }
        childFragmentManager.setFragmentResultListener(KEY_DELETE, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) viewModel.delete()
        }

        collectWhenStarted(viewModel.creation) { creation ->
            binding.ivImage.loadWithShimmer(creation?.imagePath, binding.shimmer)
            binding.btnEdit.isVisible = creation?.type == CreationType.CHARACTER
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                CreationDetailEvent.Deleted -> findNavController().popBackStack()
                is CreationDetailEvent.Downloaded -> showAppToast(if (event.success) R.string.saved else R.string.save_failed)
            }
        }
    }

    companion object {
        /** Id creation (Long). */
        const val ARG_CREATION_ID = "creationId"

        private const val KEY_DELETE = "detail_delete"
    }
}
