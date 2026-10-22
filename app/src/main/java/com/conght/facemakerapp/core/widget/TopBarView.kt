package com.conght.facemakerapp.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.content.withStyledAttributes
import androidx.core.view.children
import com.conght.facemakerapp.R

/**
 * Hàng icon trên cùng (y = 60dp): trái là nút back, phải là các action.
 * Các view con khai báo trong XML được xếp vào bên phải, cách nhau 16dp.
 */
class TopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    val backButton: ImageButton = ImageButton(context, null, 0, R.style.Widget_FaceMaker_ImageIconButton).apply {
        setImageResource(R.drawable.ic_back)
        contentDescription = context.getString(R.string.cd_back)
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        context.withStyledAttributes(attrs, R.styleable.TopBarView, defStyleAttr) {
            backButton.visibility = if (getBoolean(R.styleable.TopBarView_showBack, true)) VISIBLE else INVISIBLE
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val actions = children.toList()
        removeAllViews()
        val iconSize = resources.getDimensionPixelSize(R.dimen.icon_button_large)
        addView(backButton, LayoutParams(iconSize, iconSize))
        addView(Space(context), LayoutParams(0, 0, 1f))
        val gap = resources.getDimensionPixelSize(R.dimen.top_bar_action_gap)
        actions.forEachIndexed { index, view ->
            val params = (view.layoutParams as? LayoutParams) ?: LayoutParams(iconSize, iconSize)
            if (index > 0) params.marginStart = gap
            addView(view, params)
        }
    }

    fun setOnBackClickListener(listener: View.OnClickListener?) {
        backButton.setOnClickListener(listener)
    }
}
