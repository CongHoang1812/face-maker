package com.conght.facemakerapp.core.ui.skeleton

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

/** Adapter chỉ inflate [count] skeleton từ [layoutRes] (đặt RecyclerView trong ShimmerFrameLayout). */
class SkeletonAdapter(
    @param:LayoutRes private val layoutRes: Int,
    count: Int,
) : RecyclerView.Adapter<SkeletonAdapter.ViewHolder>() {

    var count: Int = count
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = count

    override fun getItemViewType(position: Int): Int = layoutRes

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(layoutRes, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
