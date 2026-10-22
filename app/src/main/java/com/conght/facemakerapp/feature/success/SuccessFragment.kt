package com.conght.facemakerapp.feature.success

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.loadImage
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.core.ui.shareImages
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.databinding.FragmentSuccessBinding
import com.conght.facemakerapp.feature.creation.MyCreationFragment
import com.conght.facemakerapp.feature.dialog.RateUsDialogFragment
import com.conght.facemakerapp.feature.dialog.setLoadingVisible
import dagger.hilt.android.AndroidEntryPoint

/**
 * Successful (Figma 3:1109 maker / 3:223 cosplay).
 * Download → copy vào internal storage (`filesDir/downloads/`), không ra Gallery.
 * Share → FileProvider. Re-edit → quay lại editor. Back = Home.
 */
@AndroidEntryPoint
class SuccessFragment : BaseFragment<FragmentSuccessBinding>(FragmentSuccessBinding::inflate) {

    enum class Source { MAKER, COSPLAY }

    private val viewModel: SuccessViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)
        setupSource()

        binding.btnReEdit.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnShare.setOnSingleClickListener { share() }
        binding.btnMyCreation.setOnSingleClickListener {
            val tab = if (viewModel.source == Source.COSPLAY) CreationType.CHARACTER else CreationType.DESIGN
            navigateSafe(R.id.action_success_to_myCreation, Bundle().apply { putString(MyCreationFragment.ARG_TAB, tab.name) })
        }
        binding.btnHome.setOnSingleClickListener { goHome() }
        binding.btnDownload.setOnSingleClickListener { viewModel.download() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        collectWhenStarted(viewModel.creation) { creation ->
            binding.ivResult.loadImage(creation?.imagePath, R.color.white)
            binding.btnShare.isEnabled = creation != null
            binding.btnDownload.isEnabled = creation != null
        }
        collectWhenStarted(viewModel.downloading) {
            binding.btnDownload.alpha = if (it) DISABLED_ALPHA else 1f
            setLoadingVisible(it)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                SuccessEvent.Downloaded -> showAppToast(R.string.saved)
                SuccessEvent.DownloadFailed -> showAppToast(R.string.save_failed)
                SuccessEvent.ShowRateUs -> if (childFragmentManager.findFragmentByTag(RateUsDialogFragment.TAG) == null) {
                    RateUsDialogFragment.newInstance().show(childFragmentManager, RateUsDialogFragment.TAG)
                }
            }
        }
    }

    /** COSPLAY: huy hiệu sao + viền ảnh nâu 4dp, ảnh dịch xuống dưới huy hiệu. */
    private fun setupSource() {
        val cosplay = viewModel.source == Source.COSPLAY
        binding.starBadge.isVisible = cosplay
        if (!cosplay) return
        val border = resources.getDimension(R.dimen.success_image_border)
        binding.ivResult.strokeWidth = border
        val inset = (border / 2).toInt()
        binding.ivResult.setPadding(inset, inset, inset, inset)
        binding.ivResult.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = resources.getDimensionPixelSize(R.dimen.success_cosplay_image_top)
        }
        listOf(binding.ivStar1, binding.ivStar2, binding.ivStar3).forEachIndexed { index, star ->
            star.setImageResource(if (index < viewModel.stars) R.drawable.img_star_on else R.drawable.img_star_off)
        }
        binding.starBadge.contentDescription =
            resources.getQuantityString(R.plurals.cd_rate_stars, viewModel.stars, viewModel.stars)
    }

    private fun share() {
        val path = viewModel.creation.value?.imagePath ?: return
        if (!requireContext().shareImages(listOf(path), getString(R.string.cd_share))) {
            showAppToast(R.string.error_generic)
        }
    }

    private fun goHome() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    companion object {
        /** Id creation vừa lưu (Long). */
        const val ARG_CREATION_ID = "creationId"

        /** [Source] name. */
        const val ARG_SOURCE = "source"

        /** Số sao 0..3 (chỉ COSPLAY). */
        const val ARG_STARS = "stars"

        private const val DISABLED_ALPHA = 0.5f
    }
}
