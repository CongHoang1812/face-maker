package com.conght.facemakerapp.feature.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.loadWithShimmer
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType
import com.conght.facemakerapp.databinding.ItemCreationBinding

/** Một thẻ trong lưới My creation. */
data class CreationItem(
    val creation: Creation,
    val selectionMode: Boolean,
    val selected: Boolean,
)

class CreationAdapter(
    private val listener: Listener,
) : ListAdapter<CreationItem, CreationAdapter.ViewHolder>(DIFF) {

    interface Listener {
        fun onOpen(creation: Creation)
        fun onLongPress(creation: Creation)
        fun onToggle(creation: Creation)
        fun onDelete(creation: Creation)
        fun onEdit(creation: Creation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCreationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemCreationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CreationItem) {
            val creation = item.creation
            binding.ivImage.loadWithShimmer(creation.imagePath, binding.shimmer)
            binding.root.contentDescription = binding.root.resources.getString(R.string.cd_result_image)

            // Bình thường: xoá (+ sửa với Character). Chọn nhiều: checkbox.
            binding.btnDelete.isVisible = !item.selectionMode
            binding.btnEdit.isVisible = !item.selectionMode && creation.type == CreationType.CHARACTER
            binding.ivCheckbox.isVisible = item.selectionMode
            binding.ivCheckbox.isSelected = item.selected
            binding.root.isSelected = item.selected

            binding.root.setOnSingleClickListener {
                if (item.selectionMode) listener.onToggle(creation) else listener.onOpen(creation)
            }
            binding.root.setOnLongClickListener {
                if (!item.selectionMode) listener.onLongPress(creation)
                true
            }
            binding.btnDelete.setOnSingleClickListener { listener.onDelete(creation) }
            binding.btnEdit.setOnSingleClickListener { listener.onEdit(creation) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CreationItem>() {
            override fun areItemsTheSame(old: CreationItem, new: CreationItem) = old.creation.id == new.creation.id
            override fun areContentsTheSame(old: CreationItem, new: CreationItem) = old == new
        }
    }
}
