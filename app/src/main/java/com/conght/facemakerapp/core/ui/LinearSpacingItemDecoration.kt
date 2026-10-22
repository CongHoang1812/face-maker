package com.conght.facemakerapp.core.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Khoảng cách [spacing] giữa các item của list ngang (không chừa ở 2 đầu). */
class LinearSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position > 0) outRect.left = spacing
    }
}
