package com.conght.facemakerapp.feature.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.core.common.AppLanguage
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val onClick: (AppLanguage) -> Unit,
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    private val items = AppLanguage.entries

    var selected: AppLanguage? = null
        set(value) {
            val old = field
            field = value
            old?.let { notifyItemChanged(items.indexOf(it)) }
            value?.let { notifyItemChanged(items.indexOf(it)) }
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    inner class ViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(language: AppLanguage) {
            binding.ivFlag.setImageResource(language.flagRes)
            binding.tvName.setText(language.nameRes)
            binding.root.isSelected = language == selected
            binding.root.setOnSingleClickListener { onClick(language) }
        }
    }
}
