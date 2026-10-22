package com.conght.facemakerapp.feature.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.conght.facemakerapp.R
import com.conght.facemakerapp.databinding.ItemIntroPageBinding

class IntroPageAdapter : RecyclerView.Adapter<IntroPageAdapter.ViewHolder>() {

    data class Page(@param:DrawableRes val image: Int, @param:StringRes val text: Int)

    private val pages = listOf(
        Page(R.drawable.img_intro_1, R.string.intro_1),
        Page(R.drawable.img_intro_2, R.string.intro_2),
        Page(R.drawable.img_intro_3, R.string.intro_3),
    )

    override fun getItemCount(): Int = pages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemIntroPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]
        holder.binding.ivImage.setImageResource(page.image)
        holder.binding.tvText.setText(page.text)
    }

    class ViewHolder(val binding: ItemIntroPageBinding) : RecyclerView.ViewHolder(binding.root)
}
