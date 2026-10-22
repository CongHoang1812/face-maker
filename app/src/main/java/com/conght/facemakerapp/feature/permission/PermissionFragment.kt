package com.conght.facemakerapp.feature.permission

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.AppPermissions
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.FragmentPermissionBinding
import com.conght.facemakerapp.databinding.ItemPermissionRowBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {

    private val viewModel: PermissionViewModel by viewModels()

    private val mediaLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onPermissionResult(AppPermissions.media.asIterable(), AppPermissions.isMediaGranted(requireContext()))
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPermissionResult(listOfNotNull(AppPermissions.notification), granted)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.rowMedia.tvName.setText(R.string.permission_media)
        binding.rowNotification.tvName.setText(R.string.permission_notification)

        binding.rowMedia.root.setOnSingleClickListener {
            if (!AppPermissions.isMediaGranted(requireContext())) mediaLauncher.launch(AppPermissions.media)
        }
        binding.rowNotification.root.setOnSingleClickListener {
            val permission = AppPermissions.notification
            if (permission != null && !AppPermissions.isNotificationGranted(requireContext())) {
                notificationLauncher.launch(permission)
            }
        }
        binding.tvContinue.setOnSingleClickListener { viewModel.completeOnboarding() }

        collectWhenStarted(viewModel.onboardingCompleted) {
            findNavController().navigate(R.id.action_permission_to_home)
        }
    }

    override fun onResume() {
        super.onResume()
        // Người dùng có thể vừa cấp quyền trong Settings
        renderStates()
    }

    private fun renderStates() {
        val context = requireContext()
        binding.rowMedia.setGranted(AppPermissions.isMediaGranted(context))
        binding.rowNotification.setGranted(AppPermissions.isNotificationGranted(context))
    }

    private fun ItemPermissionRowBinding.setGranted(granted: Boolean) {
        tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, if (granted) R.drawable.ic_radio_on else R.drawable.ic_radio_off, 0,
        )
    }

    /** Bị từ chối vĩnh viễn (không còn hiện hộp thoại hệ thống) -> mở trang cài đặt của app. */
    private fun onPermissionResult(permissions: Iterable<String>, granted: Boolean) {
        renderStates()
        val canAskAgain = permissions.any { shouldShowRequestPermissionRationale(it) }
        if (!granted && !canAskAgain) openAppSettings()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri())
        startActivity(intent)
    }
}
