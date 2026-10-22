package com.conght.facemakerapp.feature.cosplay

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResult
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.loadImage
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogCosplayScoreBinding

/**
 * Popup điểm Cosplay: ảnh nhân vật người chơi + "{percent}%". Chỉ đóng bằng nút X
 * → `setFragmentResult(REQUEST_KEY)` để màn chơi lưu kết quả và mở Successful.
 */
class CosplayScoreDialogFragment : BaseDialogFragment<DialogCosplayScoreBinding>(DialogCosplayScoreBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.ivResult.loadImage(args.getString(ARG_IMAGE_PATH), R.color.white)
        binding.tvPercent.text = getString(R.string.score_percent, args.getInt(ARG_PERCENT))
        binding.btnClose.setOnSingleClickListener {
            setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
            dismiss()
        }
    }

    companion object {
        const val TAG = "CosplayScoreDialog"
        const val REQUEST_KEY = "cosplay_score_closed"

        private const val ARG_PERCENT = "percent"
        private const val ARG_IMAGE_PATH = "imagePath"

        fun newInstance(percent: Int, imagePath: String) = CosplayScoreDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PERCENT, percent)
                putString(ARG_IMAGE_PATH, imagePath)
            }
        }
    }
}
