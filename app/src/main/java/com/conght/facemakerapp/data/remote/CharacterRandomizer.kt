package com.conght.facemakerapp.data.remote

import com.conght.facemakerapp.data.model.CharacterConfig
import com.conght.facemakerapp.data.model.PartCategory
import com.conght.facemakerapp.data.model.PartSelection
import com.conght.facemakerapp.data.model.TemplateDetail
import javax.inject.Inject
import kotlin.random.Random

/**
 * Random nhân vật từ dữ liệu template (API không có endpoint random): dùng cho xúc xắc ở
 * customize / cosplay, đề Cosplay và Lucky roll.
 */
class CharacterRandomizer(private val random: Random) {

    @Inject constructor() : this(Random.Default)

    /** Item + màu ngẫu nhiên của [category]; null nếu category không có item. */
    fun randomSelection(category: PartCategory): PartSelection? {
        val item = category.items.randomOrNull(random) ?: return null
        return PartSelection(item.id, item.colors.randomOrNull(random)?.id)
    }

    /** Mọi category đều có item + màu ngẫu nhiên (nhân vật đủ bộ phận); chỉ category không có item mới None. */
    fun randomConfig(detail: TemplateDetail): CharacterConfig = CharacterConfig(
        templateId = detail.templateId,
        selections = detail.categories.associate { category -> category.id to (randomSelection(category) ?: NONE) },
    )

    fun <T> pick(list: List<T>): T? = list.randomOrNull(random)

    private companion object {
        val NONE = PartSelection(itemId = null, colorId = null)
    }
}
