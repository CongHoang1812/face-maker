package com.conght.facemakerapp.core.ui.skeleton

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * Lưới phụ kiện luôn có ô "None" ở vị trí 0 (quy ước 00_README mục 5), phần còn lại là [rest]
 * (skeleton khi chờ API hoặc item thật). Mọi lưới phụ kiện tạo adapter qua hàm này.
 */
fun noneFirst(none: NoneAdapter, vararg rest: RecyclerView.Adapter<out RecyclerView.ViewHolder>): ConcatAdapter =
    ConcatAdapter(noneFirstAdapters(none, *rest))

/** Thứ tự adapter của [noneFirst] (tách riêng để unit test). */
internal fun noneFirstAdapters(
    none: NoneAdapter,
    vararg rest: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
): List<RecyclerView.Adapter<out RecyclerView.ViewHolder>> = listOf(none) + rest
