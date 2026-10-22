package com.conght.facemakerapp.feature.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.setFragmentResult
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogConfirmBinding

/**
 * Dialog xác nhận dùng chung (Exit / Reset / Delete / Random) – tạo qua [ConfirmDialogs].
 * Kết quả: `setFragmentResult(requestKey, { KEY_CONFIRMED: Boolean })` trên parentFragmentManager;
 * bấm ngoài / back = `false`.
 *
 * Chuỗi truyền bằng resource id nên dialog hiển thị đúng ngôn ngữ cả khi recreate.
 */
class ConfirmDialogFragment : BaseDialogFragment<DialogConfirmBinding>(DialogConfirmBinding::inflate) {

    private val args get() = requireArguments()
    private var resultSent = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = binding.container
        container.title = getString(args.getInt(ARG_TITLE))
        container.message = getString(args.getInt(ARG_MESSAGE))
        val confirmOnStart = args.getBoolean(ARG_CONFIRM_ON_START)
        val confirmText = getString(args.getInt(ARG_CONFIRM_TEXT))
        val dismissText = getString(args.getInt(ARG_DISMISS_TEXT))
        container.startButton.text = if (confirmOnStart) confirmText else dismissText
        container.endButton.text = if (confirmOnStart) dismissText else confirmText
        container.startButton.setOnSingleClickListener { finish(confirmed = confirmOnStart) }
        container.endButton.setOnSingleClickListener { finish(confirmed = !confirmOnStart) }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        sendResult(false)
    }

    private fun finish(confirmed: Boolean) {
        // TODO(ADS): khi confirmed && args.getBoolean(ARG_WATCH_AD) -> show rewarded ad trước khi trả kết quả
        sendResult(confirmed)
        dismiss()
    }

    private fun sendResult(confirmed: Boolean) {
        if (resultSent) return
        resultSent = true
        setFragmentResult(args.getString(ARG_REQUEST_KEY).orEmpty(), Bundle().apply { putBoolean(KEY_CONFIRMED, confirmed) })
    }

    companion object {
        const val TAG = "ConfirmDialog"
        const val KEY_CONFIRMED = "confirmed"

        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CONFIRM_TEXT = "confirmText"
        private const val ARG_DISMISS_TEXT = "dismissText"
        private const val ARG_CONFIRM_ON_START = "confirmOnStart"
        private const val ARG_WATCH_AD = "watchAd"

        /** Đọc kết quả trong FragmentResultListener. */
        fun isConfirmed(result: Bundle): Boolean = result.getBoolean(KEY_CONFIRMED)

        /**
         * @param confirmOnStart nút xác nhận nằm bên trái (Yes | No) hay bên phải (Cancel | Watch Ad).
         */
        fun newInstance(
            requestKey: String,
            @StringRes title: Int,
            @StringRes message: Int,
            @StringRes confirmText: Int,
            @StringRes dismissText: Int,
            confirmOnStart: Boolean = true,
            watchAd: Boolean = false,
        ) = ConfirmDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_REQUEST_KEY, requestKey)
                putInt(ARG_TITLE, title)
                putInt(ARG_MESSAGE, message)
                putInt(ARG_CONFIRM_TEXT, confirmText)
                putInt(ARG_DISMISS_TEXT, dismissText)
                putBoolean(ARG_CONFIRM_ON_START, confirmOnStart)
                putBoolean(ARG_WATCH_AD, watchAd)
            }
        }
    }
}
