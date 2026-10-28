package com.conght.facemakerapp.feature.maker.template

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.loadWithShimmer
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.data.model.CharacterTemplate
import com.conght.facemakerapp.databinding.ItemTemplateBinding

/** Một thẻ trong lưới; [template] null = thẻ chờ API (ô ảnh shimmer), chưa bấm được. */
data class TemplateCell(val position: Int, val template: CharacterTemplate?)

class TemplateAdapter(
    private val onClick: (CharacterTemplate) -> Unit,
) : ListAdapter<TemplateCell, TemplateAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTemplateBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemTemplateBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cell: TemplateCell) {
            val template = cell.template
            // Chờ API (template null) hoặc đang tải ảnh: ô ảnh shimmer
            binding.ivThumbnail.loadWithShimmer(template?.thumbnailUrl, binding.shimmer)
            binding.root.contentDescription = template?.name ?: binding.root.resources.getString(R.string.loading)
            // Chỉ bấm được khi đã có dữ liệu template (tránh mở Customize không có category)
            if (template != null) {
                binding.root.setOnSingleClickListener { onClick(template) }
            } else {
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TemplateCell>() {
            override fun areItemsTheSame(old: TemplateCell, new: TemplateCell) =
                old.template?.id == new.template?.id && old.position == new.position

            override fun areContentsTheSame(old: TemplateCell, new: TemplateCell) = old == new
        }
    }
}
