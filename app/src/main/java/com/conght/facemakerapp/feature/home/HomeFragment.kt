package com.conght.facemakerapp.feature.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.FragmentHomeBinding
import com.conght.facemakerapp.feature.dialog.NoInternetDialogFragment
import com.conght.facemakerapp.feature.dialog.RateUsDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Home (Figma 3:438). Back ở Home: lần lẻ thoát app, lần chẵn hiện dialog Rate (đến khi đã rate).
 * Rate xong hoặc bấm Exit trong dialog thì thoát app.
 * Maker / Cosplay / Lucky roll cần mạng: mất mạng → dialog No Internet (Try again kiểm tra lại).
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.btnSettings.setOnSingleClickListener { navigateSafe(R.id.action_home_to_setting) }
        binding.cardMaker.setOnSingleClickListener { navigateOnline(R.id.action_home_to_templateSelect) }
        binding.cardCosplay.setOnSingleClickListener { navigateOnline(R.id.action_home_to_cosplayStart) }
        binding.cardLucky.setOnSingleClickListener { navigateOnline(R.id.action_home_to_luckyRoll) }
        binding.cardCreation.setOnSingleClickListener { navigateSafe(R.id.action_home_to_myCreation) }

        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, bundle ->
            navigateOnline(NoInternetDialogFragment.destination(bundle))
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })
        childFragmentManager.setFragmentResultListener(RateUsDialogFragment.KEY_RATED, viewLifecycleOwner) { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.markAppRated()
                // App đóng ngay nên Snackbar của showAppToast không kịp hiện: dùng Toast hệ thống.
                Toast.makeText(requireContext().applicationContext, R.string.rate_thank_you, Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
        childFragmentManager.setFragmentResultListener(RateUsDialogFragment.KEY_EXIT, viewLifecycleOwner) { _, _ ->
            requireActivity().finish()
        }
    }

    private fun onBack() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.shouldShowRateOnBack()) {
                RateUsDialogFragment.showOnce(childFragmentManager)
            } else {
                requireActivity().finish()
            }
        }
    }

    /** Có mạng → mở [action]; không → dialog No Internet (Try again gọi lại hàm này). */
    private fun navigateOnline(@IdRes action: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                navigateSafe(action)
            } else {
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET, action)
            }
        }
    }

    private companion object {
        const val KEY_NO_INTERNET = "home_no_internet"
    }
}
