package com.conght.facemakerapp.feature.home

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.FragmentHomeBinding
import com.conght.facemakerapp.feature.dialog.RateUsDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Home (Figma 3:438). Back ở Home = thoát app; lần đầu (chưa rate) hiện Rate us trước. */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.btnSettings.setOnSingleClickListener { navigateSafe(R.id.action_home_to_setting) }
        binding.cardMaker.setOnSingleClickListener { navigateSafe(R.id.action_home_to_templateSelect) }
        binding.cardCosplay.setOnSingleClickListener { navigateSafe(R.id.action_home_to_cosplayStart) }
        binding.cardLucky.setOnSingleClickListener { navigateSafe(R.id.action_home_to_luckyRoll) }
        binding.cardCreation.setOnSingleClickListener { navigateSafe(R.id.action_home_to_myCreation) }

        // Rate us đóng (đã rate hay chưa) → thoát app
        childFragmentManager.setFragmentResultListener(RateUsDialogFragment.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
            requireActivity().finish()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })
    }

    private fun onBack() {
        if (childFragmentManager.findFragmentByTag(RateUsDialogFragment.TAG) != null) return
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.shouldPromptRateOnExit()) {
                RateUsDialogFragment.newInstance().show(childFragmentManager, RateUsDialogFragment.TAG)
            } else {
                requireActivity().finish()
            }
        }
    }
}
