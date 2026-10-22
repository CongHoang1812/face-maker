package com.conght.facemakerapp.feature.decor.editor

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.loadImage
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.core.widget.WoodButton
import com.conght.facemakerapp.databinding.ItemDecorCellBinding

private const val CLICK_DEBOUNCE_MS = 150L

/** Ô trong lưới editor (sau ô None do [com.conght.facemakerapp.core.ui.skeleton.NoneAdapter] lo). */
sealed interface DecorCell {
    val key: String
    val selected: Boolean

    data object ColorWheel : DecorCell {
        override val key = "wheel"
        override val selected = false
    }

    data object AddImage : DecorCell {
        override val key = "add"
        override val selected = false
    }

    data class Color(@param:ColorInt val color: Int, override val selected: Boolean) : DecorCell {
        override val key = "color_$color"
    }

    /** Ảnh (đường dẫn file gần đây hoặc URL item pack). */
    data class Image(val id: String, val url: String, override val selected: Boolean) : DecorCell {
        override val key = "image_$id"
    }
}

class DecorCellAdapter(
    private val onClick: (DecorCell) -> Unit,
) : ListAdapter<DecorCell, DecorCellAdapter.ViewHolder>(object : DiffUtil.ItemCallback<DecorCell>() {
    override fun areItemsTheSame(old: DecorCell, new: DecorCell) = old.key == new.key
    override fun areContentsTheSame(old: DecorCell, new: DecorCell) = old == new
}) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDecorCellBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemDecorCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(cell: DecorCell) {
            val image = binding.ivImage
            val res = image.resources
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.layoutParams = image.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            binding.root.contentDescription = null
            if (cell !is DecorCell.Image) Glide.with(image).clear(image)
            when (cell) {
                DecorCell.ColorWheel -> {
                    image.layoutParams = image.layoutParams.apply {
                        width = res.getDimensionPixelSize(R.dimen.editor_icon_color_wheel)
                        height = width
                    }
                    image.setImageResource(R.drawable.img_color_wheel)
                    binding.root.contentDescription = res.getString(R.string.cd_color_wheel)
                }

                DecorCell.AddImage -> {
                    image.layoutParams = image.layoutParams.apply {
                        width = res.getDimensionPixelSize(R.dimen.editor_icon_add_image)
                        height = width
                    }
                    image.scaleType = ImageView.ScaleType.FIT_CENTER
                    image.setImageResource(R.drawable.img_add_image)
                    binding.root.contentDescription = res.getString(R.string.cd_add_image)
                }

                is DecorCell.Color -> image.setImageDrawable(cell.color.toDrawable())
                is DecorCell.Image -> image.loadImage(cell.url, R.color.white)
            }
            binding.root.isSelected = cell.selected
            binding.root.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(cell) }
        }
    }
}

/** Tab con (Color / Image / tên pack): WoodButton cam khi chọn, xám khi không. */
class SubTabAdapter(
    private val onClick: (Int) -> Unit,
) : ListAdapter<SubTabAdapter.Tab, SubTabAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Tab>() {
    override fun areItemsTheSame(old: Tab, new: Tab) = old.index == new.index
    override fun areContentsTheSame(old: Tab, new: Tab) = old == new
}) {

    data class Tab(val index: Int, val title: String, val selected: Boolean)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sub_tab, parent, false) as WoodButton)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tab = getItem(position)
        holder.button.text = tab.title
        holder.button.variant = if (tab.selected) WoodButton.Variant.ORANGE else WoodButton.Variant.GRAY
        holder.button.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(tab.index) }
    }

    class ViewHolder(val button: WoodButton) : RecyclerView.ViewHolder(button)
}

/** Chip font "Aa" (ô đầu "T" = font mặc định). */
class FontChipAdapter(
    private val onClick: (DecorFonts) -> Unit,
) : RecyclerView.Adapter<FontChipAdapter.ViewHolder>() {

    private val fonts = DecorFonts.entries
    var selectedId: String? = null
        set(value) {
            if (field == value) return
            val old = fonts.indexOfFirst { it.id == field }
            field = value
            if (old >= 0) notifyItemChanged(old)
            fonts.indexOfFirst { it.id == value }.takeIf { it >= 0 }?.let(::notifyItemChanged)
        }

    override fun getItemCount() = fonts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_font_chip, parent, false) as TextView)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val font = fonts[position]
        val view = holder.view
        view.text = view.context.getString(if (font == DecorFonts.DEFAULT) R.string.font_default_sample else R.string.font_sample)
        view.typeface = ResourcesCompat.getFont(view.context, font.fontRes)
        view.isSelected = font.id == selectedId
        view.contentDescription = view.context.getString(R.string.cd_font) + " " + (position + 1)
        view.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(font) }
    }

    class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)
}

/** Chip màu chữ; [colors] phần tử null = bánh xe màu (mở ColorPicker). */
class ColorChipAdapter(
    private val colors: List<Int?>,
    private val onClick: (Int?) -> Unit,
) : RecyclerView.Adapter<ColorChipAdapter.ViewHolder>() {

    var selectedColor: Int? = null
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun getItemCount() = colors.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_color_chip, parent, false) as ImageView)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val color = colors[position]
        val view = holder.view
        if (color == null) {
            view.backgroundTintList = null
            view.setImageResource(R.drawable.img_color_wheel)
            view.contentDescription = view.context.getString(R.string.cd_color_wheel)
        } else {
            view.setImageDrawable(null)
            view.backgroundTintList = ColorStateList.valueOf(color)
            view.contentDescription = String.format("#%06X", color and 0xFFFFFF)
        }
        view.isSelected = color != null && color == selectedColor
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        view.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(color) }
    }

    class ViewHolder(val view: ImageView) : RecyclerView.ViewHolder(view)
}
