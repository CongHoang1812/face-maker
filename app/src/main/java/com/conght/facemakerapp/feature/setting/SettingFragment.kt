package com.conght.facemakerapp.feature.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.navigateSafe
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.ui.showAppToast
import com.conght.facemakerapp.databinding.FragmentSettingBinding
import com.conght.facemakerapp.feature.dialog.RateUsDialogFragment
import com.conght.facemakerapp.feature.language.LanguageFragment

/** Setting (Figma 3:468): Language / Rate us / Share app / Privacy & Policy. */
class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.rowLanguage.setOnSingleClickListener {
            navigateSafe(
                R.id.action_setting_to_language,
                Bundle().apply { putBoolean(LanguageFragment.ARG_FROM_SETTING, true) },
            )
        }
        binding.rowRate.setOnSingleClickListener {
            if (childFragmentManager.findFragmentByTag(RateUsDialogFragment.TAG) == null) {
                RateUsDialogFragment.newInstance().show(childFragmentManager, RateUsDialogFragment.TAG)
            }
        }
        binding.rowShare.setOnSingleClickListener { shareApp() }
        binding.rowPrivacy.setOnSingleClickListener { openPrivacyPolicy() }
    }

    private fun shareApp() {
        val context = requireContext()
        val message = getString(
            R.string.share_app_message,
            getString(R.string.app_name),
            AppConfig.playStoreUrl(context.packageName),
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_app)))
    }

    /** Mở Custom Tab; máy không có trình duyệt hỗ trợ thì thử ACTION_VIEW. */
    private fun openPrivacyPolicy() {
        val uri = AppConfig.PRIVACY_URL.toUri()
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(requireContext(), uri)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                showAppToast(R.string.error_generic)
            }
        }
    }
}
