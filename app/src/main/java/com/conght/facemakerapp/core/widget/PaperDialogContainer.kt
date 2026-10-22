package com.conght.facemakerapp.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.conght.facemakerapp.R
import com.conght.facemakerapp.databinding.ViewPaperDialogBinding

/**
 * Khung dialog "giấy rách": tiêu đề + nội dung + vùng tuỳ biến + 2 WoodButton (trái / phải).
 * View con khai báo trong XML được đưa vào vùng tuỳ biến (giữa nội dung và hàng nút).
 * attr: `app:dialogTitle`, `app:dialogMessage`, `app:startText`, `app:endText`.
 */
class PaperDialogContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewPaperDialogBinding.inflate(LayoutInflater.from(context), this)

    /** Nút bên trái (Figma: Yes / Cancel). */
    val startButton: WoodButton get() = binding.btnStart

    /** Nút bên phải (Figma: No / Watch Ad / Done). */
    val endButton: WoodButton get() = binding.btnEnd

    init {
        setBackgroundResource(R.drawable.bg_dialog_paper)
        context.withStyledAttributes(attrs, R.styleable.PaperDialogContainer, defStyleAttr) {
            title = getString(R.styleable.PaperDialogContainer_dialogTitle)
            message = getString(R.styleable.PaperDialogContainer_dialogMessage)
            getString(R.styleable.PaperDialogContainer_startText)?.let { binding.btnStart.text = it }
            getString(R.styleable.PaperDialogContainer_endText)?.let { binding.btnEnd.text = it }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val extra = (0 until childCount).map(::getChildAt).filter { it !== binding.panel }
        extra.forEach {
            removeView(it)
            binding.content.addView(it)
        }
        binding.content.isVisible = extra.isNotEmpty()
    }

    var title: CharSequence?
        get() = binding.tvTitle.text
        set(value) {
            binding.tvTitle.text = value
            binding.tvTitle.isVisible = !value.isNullOrEmpty()
        }

    var message: CharSequence?
        get() = binding.tvMessage.text
        set(value) {
            binding.tvMessage.text = value
            binding.tvMessage.isVisible = !value.isNullOrEmpty()
        }

    fun setOnStartClickListener(listener: View.OnClickListener?) {
        binding.btnStart.setOnClickListener(listener)
    }

    fun setOnEndClickListener(listener: View.OnClickListener?) {
        binding.btnEnd.setOnClickListener(listener)
    }
}
