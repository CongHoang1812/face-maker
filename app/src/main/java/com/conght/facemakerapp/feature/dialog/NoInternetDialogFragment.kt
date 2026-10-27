package com.conght.facemakerapp.feature.dialog

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogConfirmBinding

/**
 * "No Internet Connection" (Figma 172:18): khung giấy như [ConfirmDialogFragment], 1 nút Try again.
 * Try again → đóng dialog và `setFragmentResult(requestKey, { KEY_DESTINATION })` để màn gọi kiểm tra
 * mạng lại rồi mở [destination] (hoặc hiện lại dialog nếu vẫn mất mạng). Bấm ngoài / back = chỉ đóng.
 */
class NoInternetDialogFragment : BaseDialogFragment<DialogConfirmBinding>(DialogConfirmBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = binding.container
        container.title = getString(R.string.dialog_no_internet_title)
        container.message = getString(R.string.dialog_no_internet_message)
        container.startButton.text = getString(R.string.try_again)
        container.endButton.isVisible = false
        container.startButton.setOnSingleClickListener {
            val args = requireArguments()
            val fm = parentFragmentManager
            // Đóng hẳn trước khi trả kết quả để màn gọi hiện lại được dialog nếu vẫn mất mạng
            dismissNow()
            fm.setFragmentResult(
                args.getString(ARG_REQUEST_KEY).orEmpty(),
                Bundle().apply { putInt(KEY_DESTINATION, args.getInt(ARG_DESTINATION)) },
            )
        }
    }

    companion object {
        const val TAG = "NoInternetDialog"

        private const val KEY_DESTINATION = "destination"
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_DESTINATION = "destination"

        /** Đọc action/destination cần mở lại trong FragmentResultListener. */
        @IdRes
        fun destination(result: Bundle): Int = result.getInt(KEY_DESTINATION)

        /** @param destination action / destination id sẽ mở khi có mạng trở lại. */
        fun newInstance(requestKey: String, @IdRes destination: Int) = NoInternetDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_REQUEST_KEY, requestKey)
                putInt(ARG_DESTINATION, destination)
            }
        }
    }
}
