package com.conght.facemakerapp.data.model

data class CharacterTemplate(val id: String, val name: String, val thumbnailUrl: String)

data class TemplateDetail(
    val templateId: String,
    val categories: List<PartCategory>,
    val defaultConfig: CharacterConfig,
)

data class PartCategory(
    val id: String,
    val name: String,
    val iconUrl: String,
    val zIndex: Int,              // thứ tự vẽ layer
    val isRequired: Boolean,      // vẫn cho phép "None" nếu false; nếu true None = về default
    val items: List<PartItem>,
)

data class PartItem(
    val id: String,
    val thumbnailUrl: String,
    val colors: List<PartColor>,  // rỗng = không đổi màu
    val layerUrl: String? = null, // layer khi item không có màu (có màu thì dùng PartColor.layerUrl)
)

data class PartColor(val id: String, val hex: String, val layerUrl: String)

/** Ảnh layer cần vẽ cho [selection]; null = None / item không còn trong category. */
fun PartCategory.layerUrlFor(selection: PartSelection?): String? {
    val item = items.firstOrNull { it.id == selection?.itemId } ?: return null
    val color = item.colors.firstOrNull { it.id == selection?.colorId } ?: item.colors.firstOrNull()
    return color?.layerUrl ?: item.layerUrl ?: item.thumbnailUrl
}

data class PartSelection(val itemId: String?, val colorId: String?)   // itemId null = None

data class CharacterTransform(
    val offsetX: Float = 0f, val offsetY: Float = 0f,
    val scale: Float = 1f, val rotation: Float = 0f, val flipped: Boolean = false,
)

data class CharacterConfig(
    val templateId: String,
    val selections: Map<String, PartSelection>, // key = categoryId
    val transform: CharacterTransform = CharacterTransform(),
)
