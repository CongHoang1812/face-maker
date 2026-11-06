package com.conght.facemakerapp.feature.dialog

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogPackNameBinding

/**
 * Create Package Name: đặt tên gói sticker trước khi thêm vào WhatsApp.
 * Yes (chỉ bấm được khi đã nhập tên) → `setFragmentResult(requestKey, { KEY_PACK_NAME: String })`
 * trên parentFragmentManager. No / bấm ngoài / back: không trả kết quả.
 */
class PackNameDialogFragment : BaseDialogFragment<DialogPackNameBinding>(DialogPackNameBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val input = binding.etPackName
        input.doAfterTextChanged { updateYesEnabled() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            actionId == EditorInfo.IME_ACTION_DONE
        }
        binding.btnYes.setOnSingleClickListener { submit() }
        binding.btnNo.setOnSingleClickListener { dismiss() }
        updateYesEnabled()
        input.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        // Mở dialog là hiện bàn phím luôn
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun currentName() = binding.etPackName.text?.toString()?.trim().orEmpty()

    private fun updateYesEnabled() {
        val enabled = currentName().isNotEmpty()
        binding.btnYes.isEnabled = enabled
        binding.btnYes.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private fun submit() {
        val name = currentName()
        if (name.isEmpty()) return
        setFragmentResult(requireArguments().getString(ARG_REQUEST_KEY).orEmpty(), bundleOf(KEY_PACK_NAME to name))
        dismiss()
    }

    companion object {
        const val TAG = "PackNameDialog"
        private const val ARG_REQUEST_KEY = "requestKey"
        private const val KEY_PACK_NAME = "packName"
        private const val DISABLED_ALPHA = 0.5f

        fun packName(result: Bundle): String = result.getString(KEY_PACK_NAME).orEmpty()

        fun newInstance(requestKey: String) = PackNameDialogFragment().apply {
            arguments = bundleOf(ARG_REQUEST_KEY to requestKey)
        }
    }
}
