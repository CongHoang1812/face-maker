package com.conght.facemakerapp.feature.cosplay

import android.os.Bundle
import android.view.View
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogInstructionBinding

/** Luật chơi Cosplay (Figma 3:77): thẻ giấy rách + nút X bên dưới, nền dim. */
class InstructionDialogFragment : BaseDialogFragment<DialogInstructionBinding>(DialogInstructionBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnSingleClickListener { dismiss() }
    }

    companion object {
        const val TAG = "InstructionDialog"
    }
}
