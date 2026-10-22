package com.conght.facemakerapp.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.conght.facemakerapp.R

/** DialogFragment nền trong suốt (Theme.FaceMaker.Dialog), nội dung tự vẽ bằng ViewBinding. */
abstract class BaseDialogFragment<VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB,
) : DialogFragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = checkNotNull(_binding) { "Binding chỉ dùng giữa onCreateView và onDestroyView" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_FaceMaker_Dialog)
    }

    /**
     * Bọc layout trong FrameLayout wrap_content: nếu inflate với parent null, layout_width/height
     * của thẻ gốc (vd. @dimen/dialog_width) bị bỏ qua và dialog co theo nội dung.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val host = FrameLayout(inflater.context)
        _binding = inflate(inflater, host, false)
        host.addView(binding.root)
        return host
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
