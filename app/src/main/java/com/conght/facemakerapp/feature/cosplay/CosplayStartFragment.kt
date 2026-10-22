package com.conght.facemakerapp.feature.cosplay

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.Resource
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.loadImage
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.FragmentCosplayStartBinding
import com.conght.facemakerapp.feature.maker.customize.CustomizeFragment
import com.conght.facemakerapp.feature.maker.customize.CustomizeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Cosplay start (Figma 3:41). Chưa có API: ảnh tham chiếu là skeleton shimmer, nút Cosplay disable.
 * Random gọi lại đề; (i) mở luật chơi (tự hiện lần đầu).
 */
@AndroidEntryPoint
class CosplayStartFragment : BaseFragment<FragmentCosplayStartBinding>(FragmentCosplayStartBinding::inflate) {

    private val viewModel: CosplayViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnInfo.setOnSingleClickListener {
            viewModel.markInstructionSeen()
            showInstruction()
        }
        binding.btnRandom.setOnSingleClickListener { viewModel.random() }
        binding.errorState.setOnRetryClickListener { viewModel.random() }
        binding.btnCosplay.setOnSingleClickListener {
            val challenge = (viewModel.challenge.value as? Resource.Success)?.data ?: return@setOnSingleClickListener
            navigateSafe(
                R.id.action_cosplayStart_to_cosplayPlay,
                Bundle().apply {
                    putString(CustomizeFragment.ARG_MODE, CustomizeMode.COSPLAY.name)
                    putString(CustomizeFragment.ARG_TEMPLATE_ID, challenge.templateId)
                    putString(CustomizeFragment.ARG_CHALLENGE_JSON, viewModel.challengeJson(challenge))
                },
            )
        }

        if (savedInstanceState == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                if (viewModel.shouldShowInstructionOnce()) showInstruction()
            }
        }

        collectWhenStarted(viewModel.challenge) { resource ->
            val ready = resource is Resource.Success
            if (resource is Resource.Loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
            binding.referenceSkeleton.root.isInvisible = resource !is Resource.Loading
            binding.errorState.isVisible = resource is Resource.Error
            if (resource is Resource.Error) binding.errorState.setError(resource.throwable)
            binding.ivReference.isInvisible = !ready
            binding.btnCosplay.isEnabled = ready
            binding.btnCosplay.alpha = if (ready) 1f else DISABLED_ALPHA
            if (resource is Resource.Success) binding.ivReference.loadImage(resource.data.referenceImageUrl, R.color.white)
        }
    }

    private fun showInstruction() {
        if (childFragmentManager.findFragmentByTag(InstructionDialogFragment.TAG) == null) {
            InstructionDialogFragment().show(childFragmentManager, InstructionDialogFragment.TAG)
        }
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
    }
}
