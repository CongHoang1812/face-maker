package com.conght.facemakerapp.feature.dialog

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.databinding.DialogLoadingBinding

/**
 * Dialog chờ (màn thiếu trong Figma): thẻ giấy 200×140 + vòng xoay `brown_primary` + "Saving…".
 * Không huỷ được bằng chạm ngoài / back. Điều khiển qua [Fragment.setLoadingVisible].
 */
class LoadingDialogFragment : BaseDialogFragment<DialogLoadingBinding>(DialogLoadingBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvMessage.setText(requireArguments().getInt(ARG_MESSAGE, R.string.saving))
    }

    companion object {
        const val TAG = "LoadingDialog"
        private const val ARG_MESSAGE = "message"

        fun newInstance(@StringRes message: Int) = LoadingDialogFragment().apply {
            arguments = Bundle().apply { putInt(ARG_MESSAGE, message) }
        }
    }
}

/** Hiện / ẩn [LoadingDialogFragment] (idempotent, an toàn khi recreate). */
fun Fragment.setLoadingVisible(visible: Boolean, @StringRes message: Int = R.string.saving) {
    val fm = childFragmentManager
    val current = fm.findFragmentByTag(LoadingDialogFragment.TAG) as? LoadingDialogFragment
    if (visible && current == null) {
        LoadingDialogFragment.newInstance(message).show(fm, LoadingDialogFragment.TAG)
    } else if (!visible && current != null) {
        current.dismissAllowingStateLoss()
    }
}
