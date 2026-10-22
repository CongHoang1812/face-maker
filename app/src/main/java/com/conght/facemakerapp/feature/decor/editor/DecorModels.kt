package com.conght.facemakerapp.feature.decor.editor

import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import com.conght.facemakerapp.R
import com.conght.facemakerapp.data.model.DecorType

/** Tab chính của editor (Figma 3:773 / 3:913 / 3:993 / 3:1155). */
enum class EditorTab(val packType: DecorType?) {
    BACKGROUND(DecorType.BACKGROUND),
    STICKER(DecorType.STICKER),
    SPEECH(DecorType.SPEECH),
    TEXT(null),
}

enum class ElementType { STICKER, SPEECH, TEXT }

/**
 * Một phần tử trên canvas. Toạ độ tâm [x], [y] là tỉ lệ 0..1 theo kích thước canvas
 * (độc lập mật độ màn hình); [scale] / [rotation] (độ) / [flipped] áp quanh tâm.
 */
data class DecorElement(
    val id: String,
    val type: ElementType,
    val imageUrl: String? = null,
    val text: String? = null,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipped: Boolean = false,
    val fontId: String? = null,
    @param:ColorInt val color: Int? = null,
)

/** Nền canvas: null hết = trắng (None). */
data class DecorBackground(
    @param:ColorInt val color: Int? = null,
    /** Ảnh người dùng chọn (đã copy vào cacheDir) hoặc ảnh pack (URL). */
    val imagePath: String? = null,
) {
    val isNone: Boolean get() = color == null && imagePath == null
}

/** Toàn bộ dữ liệu editor – lưu JSON trong SavedStateHandle. */
data class EditorData(
    val background: DecorBackground = DecorBackground(),
    val elements: List<DecorElement> = emptyList(),
    val selectedId: String? = null,
    val recentImages: List<String> = emptyList(),
    val mainTab: EditorTab = EditorTab.BACKGROUND,
    /** Tab con theo từng tab chính (index trong hàng tab con). */
    val subTabs: Map<EditorTab, Int> = emptyMap(),
    val fontId: String = DecorFonts.DEFAULT.id,
    @param:ColorInt val textColor: Int = DEFAULT_TEXT_COLOR,
    val isDirty: Boolean = false,
) {
    val selected: DecorElement? get() = elements.firstOrNull { it.id == selectedId }
    val subTab: Int get() = subTabs[mainTab] ?: 0

    companion object {
        const val DEFAULT_TEXT_COLOR = -0x1 // trắng (Figma: chữ "Hello" trắng)
    }
}

/** Font local cho Speech / Text (Google Fonts, SIL OFL – xem docs/licenses). */
enum class DecorFonts(val id: String, @param:FontRes val fontRes: Int) {
    DEFAULT("londrina", R.font.londrina_solid_family),
    CHELA_ONE("chela_one", R.font.chela_one),
    CHERRY_BOMB("cherry_bomb_one", R.font.cherry_bomb_one),
    BANGERS("bangers", R.font.bangers),
    PACIFICO("pacifico", R.font.pacifico),
    LOBSTER("lobster", R.font.lobster),
    CREEPSTER("creepster", R.font.creepster),
    BUNGEE("bungee", R.font.bungee);

    companion object {
        fun of(id: String?): DecorFonts = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
