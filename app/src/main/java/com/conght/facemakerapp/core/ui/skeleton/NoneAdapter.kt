package com.conght.facemakerapp.core.ui.skeleton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.core.ui.setOnSingleClickListener

/**
 * Ô "bỏ chọn" (None) luôn đứng đầu mọi lưới phụ kiện, do app tự chèn ở UI layer
 * và vẫn hoạt động kể cả khi phần còn lại đang shimmer.
 * Dùng: `noneFirst(NoneAdapter(...), SkeletonAdapter(...) | ItemAdapter)`.
 */
class NoneAdapter(
    @param:LayoutRes private val layoutRes: Int,
    private val onClick: () -> Unit,
) : RecyclerView.Adapter<NoneAdapter.ViewHolder>() {

    var isSelected: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemChanged(0)
        }

    override fun getItemCount(): Int = 1

    override fun getItemViewType(position: Int): Int = layoutRes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        view.setOnSingleClickListener(haptic = true) { onClick() }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.isSelected = isSelected
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
