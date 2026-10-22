package com.conght.facemakerapp.feature.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.fragment.app.setFragmentResult
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogColorPickerBinding

/**
 * Chọn màu tuỳ ý (HSV). Input [ARG_INITIAL_COLOR]; bấm Done trả
 * `setFragmentResult(requestKey, { KEY_COLOR: Int })`. Cancel / bấm ngoài: không trả kết quả.
 */
class ColorPickerDialogFragment : BaseDialogFragment<DialogColorPickerBinding>(DialogColorPickerBinding::inflate) {

    /** h (0..360), s (0..1), v (0..1) – lưu lại khi recreate. */
    private val hsv = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = savedInstanceState?.getFloatArray(STATE_HSV)
        if (saved != null) {
            saved.copyInto(hsv)
        } else {
            Color.colorToHSV(requireArguments().getInt(ARG_INITIAL_COLOR, Color.WHITE), hsv)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hueBar.hue = hsv[0]
        binding.svPicker.hue = hsv[0]
        binding.svPicker.setSaturationValue(hsv[1], hsv[2])

        binding.hueBar.onChange = { hue ->
            hsv[0] = hue
            binding.svPicker.hue = hue
        }
        binding.svPicker.onChange = { s, v ->
            hsv[1] = s
            hsv[2] = v
        }
        binding.container.startButton.setOnSingleClickListener { dismiss() }
        binding.container.endButton.setOnSingleClickListener {
            val key = requireArguments().getString(ARG_REQUEST_KEY).orEmpty()
            setFragmentResult(key, Bundle().apply { putInt(KEY_COLOR, Color.HSVToColor(hsv)) })
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloatArray(STATE_HSV, hsv)
    }

    companion object {
        const val TAG = "ColorPickerDialog"
        const val KEY_COLOR = "color"

        private const val ARG_REQUEST_KEY = "requestKey"
        private const val ARG_INITIAL_COLOR = "initialColor"
        private const val STATE_HSV = "hsv"

        @ColorInt
        fun color(result: Bundle): Int = result.getInt(KEY_COLOR)

        fun newInstance(requestKey: String, @ColorInt initialColor: Int) = ColorPickerDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_REQUEST_KEY, requestKey)
                putInt(ARG_INITIAL_COLOR, initialColor)
            }
        }
    }
}
